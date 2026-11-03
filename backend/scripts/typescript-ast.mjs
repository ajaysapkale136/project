import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const file = process.argv[2];
if (!file || !fs.existsSync(file)) throw new Error('A readable source file is required');

const require = createRequire(import.meta.url);
const typescriptRoot = process.env.TYPESCRIPT_PARSER_PATH || path.resolve(process.cwd(), '../frontend/node_modules/typescript');
const ts = require(typescriptRoot);
const source = fs.readFileSync(file, 'utf8');
const sourceFile = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true);
const results = [];
const line = (node) => sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile)).line + 1;
const text = (node) => node.getText(sourceFile);
const add = (name, type, details = {}) => results.push({ name, componentType: type, line: details.line ?? 1, ...details });

for (const statement of sourceFile.statements) {
  if (ts.isFunctionDeclaration(statement) && statement.name && /^[A-Z]/.test(statement.name.text)) {
    add(statement.name.text, 'UI_COMPONENT', { line: line(statement), syntax: 'FunctionDeclaration' });
  }
  if (ts.isVariableStatement(statement)) {
    for (const declaration of statement.declarationList.declarations) {
      if (ts.isIdentifier(declaration.name) && /^[A-Z]/.test(declaration.name.text) && declaration.initializer &&
          (ts.isArrowFunction(declaration.initializer) || ts.isFunctionExpression(declaration.initializer))) {
        add(declaration.name.text, 'UI_COMPONENT', { line: line(declaration), syntax: ts.SyntaxKind[declaration.initializer.kind] });
      }
    }
  }
}

const visit = (node) => {
  if (ts.isCallExpression(node) && ts.isPropertyAccessExpression(node.expression)) {
    const target = text(node.expression.expression);
    const method = node.expression.name.text.toLowerCase();
    const first = node.arguments[0];
    if ((target === 'app' || target === 'router') && ['get', 'post', 'put', 'patch', 'delete', 'use'].includes(method) && first && ts.isStringLiteralLike(first)) {
      add(`${method.toUpperCase()} ${first.text}`, 'CONTROLLER', { line: line(node), endpoints: [{ method: method.toUpperCase(), path: first.text }], syntax: 'CallExpression' });
    }
  }
  ts.forEachChild(node, visit);
};
visit(sourceFile);
console.log(JSON.stringify({ parser: 'typescript-compiler-api', components: results }));
