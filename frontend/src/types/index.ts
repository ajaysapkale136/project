export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  repositoryUrl?: string;
  defaultBranch?: string;
  status: 'CREATED' | 'INGESTED' | 'ANALYZED' | 'ERROR';
  createdAt: string;
  updatedAt: string;
  fileCount?: number;
  totalBytes?: number;
  totalEndpoints?: number;
  totalEntities?: number;
  primaryLanguage?: string;
  frameworkFamily?: string;
}

export interface DetectedTechnology {
  id: string;
  name: string;
  category: 'BACKEND' | 'FRONTEND' | 'DATABASE' | 'BUILD_TOOL' | 'DEPLOYMENT' | 'TESTING';
  version?: string;
  detectionSource?: string;
  confidenceState: 'VERIFIED' | 'INFERRED' | 'UNVERIFIED' | 'CONFLICTING';
}

export interface AnalysisComponent {
  id: string;
  moduleId: string;
  name: string;
  componentType: 'CONTROLLER' | 'SERVICE' | 'REPOSITORY' | 'ENTITY' | 'DTO' | 'UI_COMPONENT' | 'CONFIG' | 'DATA_VIEW';
  filePath: string;
  packageOrNamespace?: string;
  endpointCount: number;
  confidenceState: 'VERIFIED' | 'INFERRED' | 'UNVERIFIED' | 'CONFLICTING';
  detailsJson?: string;
}

export interface ModuleSummary {
  id: string;
  name: string;
  moduleType: string;
  basePath: string;
  componentCount: number;
  components: AnalysisComponent[];
}

export interface BusinessRule {
  id: string;
  analysisId: string;
  ruleCode: string;
  title: string;
  description: string;
  category: 'AUTH_SECURITY' | 'DATA_INTEGRITY' | 'TRANSACTIONAL' | 'API_CONTRACT' | 'WORKFLOW';
  confidenceState: 'VERIFIED' | 'INFERRED' | 'UNVERIFIED' | 'CONFLICTING';
}

export interface SourceEvidence {
  id: string;
  businessRuleId?: string;
  componentId?: string;
  filePath: string;
  startLine?: number;
  endLine?: number;
  codeSnippet: string;
  verificationState: string;
}

export interface IntelligenceOverview {
  projectId: string;
  snapshotId: string;
  analysisId: string;
  status: string;
  primaryLanguage: string;
  frameworkFamily: string;
  totalFiles: number;
  totalLinesOfCode: number;
  totalControllers: number;
  totalEndpoints: number;
  totalServices: number;
  totalRepositories: number;
  totalEntities: number;
  summary: string;
  completedAt: string;
  technologies: DetectedTechnology[];
  modules: ModuleSummary[];
  businessRules: BusinessRule[];
}

export interface ArchitectureNode {
  id: string;
  nodeKey: string;
  label: string;
  nodeType: string;
  category: string;
  posX: number;
  posY: number;
  metadataJson?: string;
}

export interface ArchitectureEdge {
  id: string;
  sourceNodeKey: string;
  targetNodeKey: string;
  label?: string;
  edgeType: string;
}

export interface ArchitectureGraph {
  versionId: string;
  analysisId: string;
  versionType: 'EXISTING' | 'PROPOSED';
  revisionNumber: number;
  summary: string;
  antiPatternsJson?: string;
  recommendationsJson?: string;
  nodes: ArchitectureNode[];
  edges: ArchitectureEdge[];
}

export interface ArchitectureDiff {
  projectId: string;
  existingVersionId: string;
  proposedVersionId: string;
  existingNodeCount: number;
  proposedNodeCount: number;
  addedCapabilities: string[];
  resolvedAntiPatterns: string[];
  riskLevel: string;
  rollbackStrategy: string;
}

export interface ModuleBlueprint {
  moduleName: string;
  purpose: string;
  businessRequirements: string[];
  functionalRequirements: string[];
  nonFunctionalRequirements: string[];
  folderStructure: string[];
  testCases: string[];
  acceptanceCriteria: string[];
  implementationSequence: string[];
  antigravityPrompt: string;
}

export interface JobProgressEvent {
  jobId: string;
  status: 'CONNECTED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progressPercent: number;
  currentStep: string;
  errorMessage?: string;
  timestamp: string;
}

export interface ProviderHealth {
  providerType: 'OPENAI' | 'GEMINI' | 'CLAUDE';
  defaultModel: string;
  configured: boolean;
  available: boolean;
  statusMessage: string;
  costPer1kInput: number;
  costPer1kOutput: number;
  latencyMs: number;
}

export interface MultiModelReviewResponse {
  reviewId: string;
  primaryModel: string;
  reviewerModels: string[];
  agreements: string[];
  disagreements: string[];
  unsupportedClaims: string[];
  unresolvedRisks: string[];
  synthesisSummary: string;
  consensusStatus: 'CONSENSUS_REACHED' | 'DISPUTE_FLAGGED';
}

export interface IsometricAssembly {
  id: string;
  name: string;
  role: string;
  material: string;
  x: number;
  y: number;
  z: number;
  width: number;
  depth: number;
  height: number;
  explodedOffsetZ: number;
  componentId?: string;
  filePath?: string;
  codeSnippet?: string;
  verificationState: string;
  metadata?: Record<string, any>;
}

export interface VisualConnection {
  id: string;
  sourceAssemblyId: string;
  targetAssemblyId: string;
  label: string;
  connectionType: string;
  style: string;
  isAnimated: boolean;
}

export interface AnimationStep {
  stepIndex: number;
  stepCode: string;
  title: string;
  description: string;
  activeAssemblyId?: string;
  targetAssemblyId?: string;
  activeConnectionId?: string;
  dataPayload?: string;
  durationMs: number;
  phase: string;
  verificationState: string;
}

export interface AnatomyScene {
  sceneId: string;
  projectId: string;
  animationFormat: 'MODULE_TO_MODULE' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE' | 'DATABASE_INTELLIGENCE';
  targetIdentifier: string;
  title: string;
  subtitle: string;
  readout: string;
  hint: string;
  assemblies: IsometricAssembly[];
  connections: VisualConnection[];
  steps: AnimationStep[];
}

export interface ExportResponse {
  exportFormat: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  content: string;
}

export interface CodeProposal {
  id: string;
  projectId: string;
  analysisId?: string;
  moduleName: string;
  title: string;
  description?: string;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'APPLIED';
  targetLanguage: string;
  promptUsed?: string;
  appliedAt?: string;
  appliedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CodeChange {
  id: string;
  proposalId: string;
  filePath: string;
  changeType: 'CREATE' | 'MODIFY' | 'DELETE';
  originalContent?: string;
  proposedContent: string;
  diffUnified?: string;
  additionsCount: number;
  deletionsCount: number;
  businessRulesPreserved?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

export interface GeneratedTest {
  id: string;
  proposalId?: string;
  projectId: string;
  targetFilePath: string;
  testFilePath: string;
  testFramework: string;
  testCode: string;
  coverageTarget?: string;
  createdAt: string;
}

export interface ProposalDetailResponse {
  proposal: CodeProposal;
  changes: CodeChange[];
  tests: GeneratedTest[];
}

export interface SecurityScan {
  id: string;
  projectId: string;
  snapshotId?: string;
  scannedFilesCount: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  passed: boolean;
  scanSummary?: string;
  createdAt: string;
}

export interface VulnerabilityFinding {
  id: string;
  scanId: string;
  filePath: string;
  lineNumber: number;
  ruleId: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  title: string;
  description: string;
  codeSnippet?: string;
  remediationRecommendation: string;
  createdAt: string;
}

export interface ContractVerification {
  id: string;
  projectId: string;
  snapshotId?: string;
  endpointMethod: string;
  endpointPath: string;
  expectedContract?: string;
  actualContract?: string;
  complianceStatus: 'COMPLIANT' | 'BREAKING_CHANGE' | 'MISSING_FIELD' | 'TYPE_MISMATCH';
  notes?: string;
  createdAt: string;
}

export interface ContractVerificationMatrix {
  totalEndpoints: number;
  compliantCount: number;
  breakingChangesCount: number;
  verifications: ContractVerification[];
}

export interface SandboxRunResponse {
  executionId: string;
  status: 'PASSED' | 'FAILED' | 'TIMED_OUT';
  durationMs: number;
  testsRun: number;
  testsPassed: number;
  testsFailed: number;
  consoleOutput: string;
  sandboxIsolationPath: string;
}

export interface IsolatedWorkspace {
  id: string;
  projectId: string;
  snapshotId: string;
  workspacePath: string;
  status: 'ACTIVE' | 'COMMITTED' | 'ROLLED_BACK' | 'STALE';
  createdAt: string;
}

export interface ChangeSet {
  id: string;
  projectId: string;
  workspaceId: string;
  sourceSnapshotId: string;
  architectureVersionId?: string;
  targetType: 'MODULE' | 'COMPONENT' | 'BUG' | 'FEATURE' | 'REFACTOR' | 'API_OPERATION';
  targetIdentifier: string;
  title: string;
  description?: string;
  technicalPlanJson: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'APPLIED' | 'ROLLED_BACK' | 'STALE_CONFLICT';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  businessRuleImpactJson?: string;
  apiCompatibilityImpactJson?: string;
  migrationImpactJson?: string;
  rollbackInstructions?: string;
  appliedAt?: string;
  rolledBackAt?: string;
  createdAt: string;
}

export interface ChangeSetFile {
  id: string;
  changeSetId: string;
  filePath: string;
  changeType: 'CREATE' | 'UPDATE' | 'DELETE';
  originalContent?: string;
  proposedContent: string;
  diffUnified?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  explanation?: string;
  additionsCount: number;
  deletionsCount: number;
  createdAt: string;
}

export interface TechnicalPlanDto {
  targetType: string;
  targetIdentifier: string;
  steps: string[];
  detectedConventions: string[];
  affectedModules: string[];
  affectedEndpoints: string[];
  affectedDatabaseTables: string[];
  riskAssessment: string;
}

export interface BusinessRuleImpactDto {
  ruleCode: string;
  title: string;
  existingRule: string;
  proposedChange: string;
  sourceEvidence: string;
  affectedClass: string;
  affectedModules: string[];
  apiCompatibilityRisk: string;
  dataMigrationRisk: string;
  securityImpact: string;
  regressionTests: string;
  rollbackPlan: string;
}

export interface ApiCompatibilityImpactDto {
  breakingChanges: string[];
  compatibleAdditions: string[];
  deprecatedEndpoints: string[];
  isBackwardCompatible: boolean;
}

export interface ChangeSetResponse {
  changeSet: ChangeSet;
  files: ChangeSetFile[];
  technicalPlan: TechnicalPlanDto;
  businessRuleImpact: BusinessRuleImpactDto[];
  apiCompatibilityImpact: ApiCompatibilityImpactDto;
  migrationImpact: string;
  isStale: boolean;
}

export interface TestExecution {
  id: string;
  projectId: string;
  changeSetId?: string;
  workspaceId?: string;
  commandExecuted: string;
  exitCode: number;
  status: 'PASSED' | 'FAILED' | 'TIMEOUT' | 'ERROR' | 'UNAVAILABLE';
  passedCount: number;
  failedCount: number;
  skippedCount: number;
  durationMs: number;
  consoleOutput?: string;
  testType: string;
  createdAt: string;
}

export interface TestExecutionReportResponse {
  execution: TestExecution;
  environment: string;
  testSummary: string;
}

export interface SecurityAuditEvent {
  id: string;
  projectId: string;
  correlationId?: string;
  eventType: string;
  actor: string;
  detailsJson?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface RollbackResponse {
  changeSetId: string;
  status: string;
  revertedFiles: string[];
  message: string;
}



// ==============================================================================
// AI Engineering Studio Types
// ==============================================================================
export interface StudioModuleDto {
  id: string;
  name: string;
  moduleType: string;
  basePath: string;
  description: string;
  componentCount: number;
  components: AnalysisComponent[];
}

export interface StudioOperationDto {
  id: string;
  name: string;
  httpMethod: string;
  path: string;
  moduleName: string;
  controllerClass: string;
  description: string;
}

export interface StudioContextResponse {
  projectId: string;
  projectName: string;
  repositoryUrl?: string;
  branch?: string;
  commitHash?: string;
  primaryLanguage: string;
  frameworkFamily: string;
  technologies: DetectedTechnology[];
  modules: StudioModuleDto[];
  operations: StudioOperationDto[];
  defaultModuleId?: string;
  defaultOperation?: string;
  statistics: Record<string, any>;
}

export interface StudioActionCard {
  id: string;
  title: string;
  mode: 'MODULE_ASSEMBLY' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE' | 'CODE_EVIDENCE';
  targetId: string;
  description: string;
  icon?: string;
}

export interface StudioEvidenceDto {
  id: string;
  filePath: string;
  startLine: number;
  endLine: number;
  className: string;
  methodName: string;
  snippet: string;
  verificationState: string;
  explanation: string;
}

export interface StudioTraceDto {
  primaryProvider: string;
  model: string;
  durationMs: number;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
  taskDistribution: string;
}

export interface StudioQueryRequest {
  prompt: string;
  activeModuleId?: string;
  activeOperation?: string;
  preferredProvider?: 'AUTO' | 'OPENAI' | 'CLAUDE' | 'GEMINI';
}

export interface StudioQueryResponse {
  answer: string;
  recommendedMode: 'MODULE_ASSEMBLY' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE';
  targetModuleId?: string;
  targetModuleName?: string;
  targetOperation?: string;
  highlightAssemblyId?: string;
  stepIndex?: number;
  actionCards: StudioActionCard[];
  sourceEvidence: StudioEvidenceDto[];
  orchestrationTrace: StudioTraceDto;
}

export interface StudioChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  actionCards?: StudioActionCard[];
  sourceEvidence?: StudioEvidenceDto[];
  trace?: StudioTraceDto;
  recommendedMode?: 'MODULE_ASSEMBLY' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE';
}
