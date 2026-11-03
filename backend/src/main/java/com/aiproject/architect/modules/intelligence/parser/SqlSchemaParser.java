package com.aiproject.architect.modules.intelligence.parser;

import com.aiproject.architect.common.ConfidenceState;
import com.aiproject.architect.modules.intelligence.entity.AnalysisComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlSchemaParser {

    private static final Logger log = LoggerFactory.getLogger(SqlSchemaParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern CREATE_TABLE_PATTERN =
            Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\"`\\[]?)([a-zA-Z0-9_]+)\\1\\s*\\((.*?)\\);",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FOREIGN_KEY_PATTERN =
            Pattern.compile("FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s*REFERENCES\\s+([\"`\\[]?)([a-zA-Z0-9_]+)\\2",
                    Pattern.CASE_INSENSITIVE);

    public List<AnalysisComponent> parse(Path filePath, String relativePath, String moduleId) {
        List<AnalysisComponent> components = new ArrayList<>();
        try {
            String content = Files.readString(filePath);
            for (Statement statement : CCJSqlParserUtil.parseStatements(content).getStatements()) {
                if (!(statement instanceof CreateTable createTable)) continue;
                String tableName = createTable.getTable().getName();
                // JSqlParser establishes that this is a CREATE TABLE AST. The narrow
                // relationship extraction below preserves referenced table names across
                // dialect-specific FK syntaxes that the library normalizes differently.
                String body = createTable.toString();

                List<String> foreignKeys = new ArrayList<>();
                Matcher fkMatcher = FOREIGN_KEY_PATTERN.matcher(body);
                while (fkMatcher.find()) {
                    foreignKeys.add(fkMatcher.group(3));
                }

                Map<String, Object> details = new HashMap<>();
                details.put("tableName", tableName);
                details.put("foreignKeyReferences", foreignKeys);
                details.put("parser", "JSqlParser");
                details.put("columns", createTable.getColumnDefinitions().stream()
                        .map(column -> Map.of("name", column.getColumnName(), "type", column.getColDataType().toString()))
                        .toList());

                components.add(AnalysisComponent.builder()
                        .moduleId(moduleId)
                        .name(tableName)
                        .componentType("ENTITY")
                        .filePath(relativePath)
                        .endpointCount(0)
                        .confidenceState(ConfidenceState.VERIFIED)
                        .detailsJson(objectMapper.writeValueAsString(details))
                        .build());
            }

        } catch (Exception e) {
            log.warn("Could not parse SQL file {}: {}", relativePath, e.getMessage());
        }
        return components;
    }
}
