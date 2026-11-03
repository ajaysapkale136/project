import axios from 'axios';
import {
  ApiResponse,
  Project,
  IntelligenceOverview,
  ArchitectureGraph,
  ArchitectureDiff,
  ModuleBlueprint,
  BusinessRule,
  SourceEvidence,
  JobProgressEvent,
  ProviderHealth,
  MultiModelReviewResponse,
  AnatomyScene,
  ExportResponse,
  CodeProposal,
  CodeChange,
  GeneratedTest,
  ProposalDetailResponse,
  SecurityScan,
  VulnerabilityFinding,
  ContractVerification,
  ContractVerificationMatrix,
  SandboxRunResponse,
  ChangeSet,
  ChangeSetFile,
  ChangeSetResponse,
  TestExecution,
  TestExecutionReportResponse,
  SecurityAuditEvent,
  RollbackResponse,
  StudioContextResponse,
  StudioQueryRequest,
  StudioQueryResponse
} from '../types';

const apiBaseUrl = (import.meta as ImportMeta & { env?: Record<string, string> }).env?.VITE_API_BASE_URL || '/api/v1';
const resolvedApiUrl = new URL(apiBaseUrl, window.location.origin);
const apiOrigin = resolvedApiUrl.origin;
const apiBasePath = resolvedApiUrl.pathname.replace(/\/$/, '');

const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Production Observability: Propagate correlation ID across all API requests
api.interceptors.request.use((config) => {
  const correlationId = (window as any).__AGY_CORRELATION_ID__ || crypto.randomUUID();
  (window as any).__AGY_CORRELATION_ID__ = correlationId;
  config.headers['X-Correlation-ID'] = correlationId;
  return config;
});

export const projectApi = {
  getProjects: async (): Promise<Project[]> => {
    const res = await api.get<ApiResponse<Project[]>>('/projects');
    return res.data.data;
  },

  createProject: async (payload: { name: string; description?: string; repositoryUrl?: string; defaultBranch?: string }): Promise<Project> => {
    const res = await api.post<ApiResponse<Project>>('/projects', payload);
    return res.data.data;
  },

  getProject: async (id: string): Promise<Project> => {
    const res = await api.get<ApiResponse<Project>>(`/projects/${id}`);
    return res.data.data;
  },

  uploadZip: async (projectId: string, file: File): Promise<{ jobId: string }> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post<ApiResponse<{ jobId: string }>>(`/projects/${projectId}/ingest/zip`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data.data;
  },

  importGitHub: async (projectId: string, payload: { repositoryUrl: string; branch?: string; accessToken?: string }): Promise<{ jobId: string }> => {
    const res = await api.post<ApiResponse<{ jobId: string }>>(`/projects/${projectId}/ingest/github`, payload);
    return res.data.data;
  },

  importWorkspace: async (projectId: string, path: string): Promise<{ jobId: string }> => {
    const res = await api.post<ApiResponse<{ jobId: string }>>(`/projects/${projectId}/ingest/workspace`, { path });
    return res.data.data;
  },

  triggerAnalysis: async (projectId: string): Promise<{ id: string }> => {
    const res = await api.post<ApiResponse<{ id: string }>>(`/projects/${projectId}/analyze`);
    return res.data.data;
  },

  getIntelligence: async (projectId: string): Promise<IntelligenceOverview> => {
    const res = await api.get<ApiResponse<IntelligenceOverview>>(`/projects/${projectId}/intelligence`);
    return res.data.data;
  },

  getRules: async (projectId: string): Promise<BusinessRule[]> => {
    const res = await api.get<ApiResponse<BusinessRule[]>>(`/projects/${projectId}/rules`);
    return res.data.data;
  },

  getRuleEvidence: async (projectId: string, ruleId: string): Promise<SourceEvidence[]> => {
    const res = await api.get<ApiResponse<SourceEvidence[]>>(`/projects/${projectId}/evidence/rule/${ruleId}`);
    return res.data.data;
  },

  getComponentEvidence: async (projectId: string, componentId: string): Promise<SourceEvidence[]> => {
    const res = await api.get<ApiResponse<SourceEvidence[]>>(`/projects/${projectId}/evidence/component/${componentId}`);
    return res.data.data;
  },

  getExistingArchitecture: async (projectId: string): Promise<ArchitectureGraph> => {
    const res = await api.get<ApiResponse<ArchitectureGraph>>(`/projects/${projectId}/architecture/existing`);
    return res.data.data;
  },

  getProposedArchitecture: async (projectId: string): Promise<ArchitectureGraph> => {
    const res = await api.get<ApiResponse<ArchitectureGraph>>(`/projects/${projectId}/architecture/proposed`);
    return res.data.data;
  },

  compareArchitecture: async (projectId: string): Promise<ArchitectureDiff> => {
    const res = await api.get<ApiResponse<ArchitectureDiff>>(`/projects/${projectId}/architecture/compare`);
    return res.data.data;
  },

  getModuleBlueprint: async (projectId: string, moduleName: string): Promise<ModuleBlueprint> => {
    const res = await api.get<ApiResponse<ModuleBlueprint>>(`/projects/${projectId}/architecture/blueprint?moduleName=${encodeURIComponent(moduleName)}`);
    return res.data.data;
  },

  // Multi-AI API
  getAiProviders: async (): Promise<ProviderHealth[]> => {
    const res = await api.get<ApiResponse<ProviderHealth[]>>('/ai/providers');
    return res.data.data;
  },

  triggerMultiModelReview: async (projectId: string, payload?: { primaryProvider?: string; reviewerProviders?: string[] }): Promise<MultiModelReviewResponse> => {
    const res = await api.post<ApiResponse<MultiModelReviewResponse>>(`/projects/${projectId}/ai/review`, payload || {});
    return res.data.data;
  },

  // Anatomy API
  getModuleAnatomyScene: async (projectId: string): Promise<AnatomyScene> => {
    const res = await api.get<ApiResponse<AnatomyScene>>(`/projects/${projectId}/anatomy/scenes/modules`);
    return res.data.data;
  },

  getExplodedAnatomyScene: async (projectId: string, moduleId?: string): Promise<AnatomyScene> => {
    const res = await api.get<ApiResponse<AnatomyScene>>(`/projects/${projectId}/anatomy/scenes/exploded${moduleId ? `?moduleId=${encodeURIComponent(moduleId)}` : ''}`);
    return res.data.data;
  },

  getExecutionAnatomyScene: async (projectId: string, operation?: string): Promise<AnatomyScene> => {
    const res = await api.get<ApiResponse<AnatomyScene>>(`/projects/${projectId}/anatomy/scenes/execution${operation ? `?operation=${encodeURIComponent(operation)}` : ''}`);
    return res.data.data;
  },
  getDatabaseAnatomyScene: async (projectId: string): Promise<AnatomyScene> => {
    const res = await api.get<ApiResponse<AnatomyScene>>(`/projects/${projectId}/anatomy/scenes/database`);
    return res.data.data;
  },

  exportAnatomyScene: async (projectId: string, payload: { exportFormat: string; animationFormat: string; targetIdentifier?: string }): Promise<ExportResponse> => {
    const res = await api.post<ApiResponse<ExportResponse>>(`/projects/${projectId}/anatomy/export`, payload);
    return res.data.data;
  },

  // AI Development Assistant API
  generateProposal: async (projectId: string, payload: { moduleName: string; title?: string; description?: string; targetLanguage?: string; providerType?: string }): Promise<ProposalDetailResponse> => {
    const res = await api.post<ApiResponse<ProposalDetailResponse>>(`/projects/${projectId}/assistant/proposals`, payload);
    return res.data.data;
  },

  refactorComponent: async (projectId: string, payload: { componentId: string; refactorGoal: string; providerType?: string }): Promise<ProposalDetailResponse> => {
    const res = await api.post<ApiResponse<ProposalDetailResponse>>(`/projects/${projectId}/assistant/refactor`, payload);
    return res.data.data;
  },

  getProposals: async (projectId: string): Promise<CodeProposal[]> => {
    const res = await api.get<ApiResponse<CodeProposal[]>>(`/projects/${projectId}/assistant/proposals`);
    return res.data.data;
  },

  getProposalDetails: async (projectId: string, proposalId: string): Promise<ProposalDetailResponse> => {
    const res = await api.get<ApiResponse<ProposalDetailResponse>>(`/projects/${projectId}/assistant/proposals/${proposalId}`);
    return res.data.data;
  },

  approveProposal: async (projectId: string, proposalId: string): Promise<CodeProposal> => {
    const res = await api.post<ApiResponse<CodeProposal>>(`/projects/${projectId}/assistant/proposals/${proposalId}/approve`);
    return res.data.data;
  },

  rejectProposal: async (projectId: string, proposalId: string, reason?: string): Promise<CodeProposal> => {
    const res = await api.post<ApiResponse<CodeProposal>>(`/projects/${projectId}/assistant/proposals/${proposalId}/reject`, { reason });
    return res.data.data;
  },

  applyProposal: async (projectId: string, proposalId: string, user?: string): Promise<CodeProposal> => {
    const res = await api.post<ApiResponse<CodeProposal>>(`/projects/${projectId}/assistant/proposals/${proposalId}/apply`, { user });
    return res.data.data;
  },

  generateTest: async (projectId: string, payload: { proposalId?: string; targetFilePath: string; framework?: string }): Promise<GeneratedTest> => {
    const res = await api.post<ApiResponse<GeneratedTest>>(`/projects/${projectId}/assistant/tests`, payload);
    return res.data.data;
  },

  // Security Validation Sandbox API
  runSecurityScan: async (projectId: string): Promise<{ scan: SecurityScan; findings: VulnerabilityFinding[] }> => {
    const res = await api.post<ApiResponse<{ scan: SecurityScan; findings: VulnerabilityFinding[] }>>(`/projects/${projectId}/security/scan`);
    return res.data.data;
  },

  getLatestScan: async (projectId: string): Promise<{ scan: SecurityScan | null; findings: VulnerabilityFinding[] }> => {
    const res = await api.get<ApiResponse<{ scan: SecurityScan | null; findings: VulnerabilityFinding[] }>>(`/projects/${projectId}/security/scans/latest`);
    return res.data.data;
  },

  verifyContracts: async (projectId: string): Promise<ContractVerificationMatrix> => {
    const res = await api.post<ApiResponse<ContractVerificationMatrix>>(`/projects/${projectId}/security/contracts/verify`);
    return res.data.data;
  },

  getContracts: async (projectId: string): Promise<ContractVerificationMatrix> => {
    const res = await api.get<ApiResponse<ContractVerificationMatrix>>(`/projects/${projectId}/security/contracts`);
    return res.data.data;
  },

  runSandbox: async (projectId: string, payload?: { testSuite?: string; timeoutSeconds?: number }): Promise<SandboxRunResponse> => {
    const res = await api.post<ApiResponse<SandboxRunResponse>>(`/projects/${projectId}/security/sandbox/run`, payload || {});
    return res.data.data;
  },

  // Patch Worktree & Safe Change Set API
  createChangeSet: async (projectId: string, payload: { targetType: string; targetIdentifier: string; title?: string; description?: string }): Promise<ChangeSetResponse> => {
    const res = await api.post<ApiResponse<ChangeSetResponse>>(`/projects/${projectId}/worktree/changesets`, payload);
    return res.data.data;
  },

  getChangeSets: async (projectId: string): Promise<ChangeSet[]> => {
    const res = await api.get<ApiResponse<ChangeSet[]>>(`/projects/${projectId}/worktree/changesets`);
    return res.data.data;
  },

  getChangeSetDetails: async (projectId: string, changeSetId: string): Promise<ChangeSetResponse> => {
    const res = await api.get<ApiResponse<ChangeSetResponse>>(`/projects/${projectId}/worktree/changesets/${changeSetId}`);
    return res.data.data;
  },

  approveChangeSet: async (projectId: string, changeSetId: string): Promise<ChangeSet> => {
    const res = await api.post<ApiResponse<ChangeSet>>(`/projects/${projectId}/worktree/changesets/${changeSetId}/approve`);
    return res.data.data;
  },

  rejectChangeSet: async (projectId: string, changeSetId: string): Promise<ChangeSet> => {
    const res = await api.post<ApiResponse<ChangeSet>>(`/projects/${projectId}/worktree/changesets/${changeSetId}/reject`);
    return res.data.data;
  },

  applyChangeSet: async (projectId: string, changeSetId: string): Promise<ChangeSet> => {
    const res = await api.post<ApiResponse<ChangeSet>>(`/projects/${projectId}/worktree/changesets/${changeSetId}/apply`);
    return res.data.data;
  },

  rollbackChangeSet: async (projectId: string, changeSetId: string): Promise<RollbackResponse> => {
    const res = await api.post<ApiResponse<RollbackResponse>>(`/projects/${projectId}/worktree/changesets/${changeSetId}/rollback`);
    return res.data.data;
  },

  updateFileDecision: async (projectId: string, fileId: string, decision: 'ACCEPTED' | 'REJECTED'): Promise<ChangeSetFile> => {
    const res = await api.post<ApiResponse<ChangeSetFile>>(`/projects/${projectId}/worktree/files/${fileId}/decision`, { decision });
    return res.data.data;
  },

  exportPatchFile: (projectId: string, changeSetId: string): string => {
    return `${apiBaseUrl}/projects/${projectId}/worktree/changesets/${changeSetId}/export/patch`;
  },

  exportWorkspaceZipUrl: (projectId: string): string => {
    return `${apiBaseUrl}/projects/${projectId}/worktree/export/zip`;
  },

  // Testing Engine API
  runSandboxedTests: async (projectId: string, payload?: { changeSetId?: string; testType?: string; timeoutSeconds?: number }): Promise<TestExecutionReportResponse> => {
    const res = await api.post<ApiResponse<TestExecutionReportResponse>>(`/projects/${projectId}/testing/run`, payload || {});
    return res.data.data;
  },

  getTestExecutions: async (projectId: string): Promise<TestExecution[]> => {
    const res = await api.get<ApiResponse<TestExecution[]>>(`/projects/${projectId}/testing/executions`);
    return res.data.data;
  },

  getAuditEvents: async (projectId: string): Promise<SecurityAuditEvent[]> => {
    const res = await api.get<ApiResponse<SecurityAuditEvent[]>>(`/projects/${projectId}/audit/events`);
    return res.data.data;
  },

  getStudioContext: async (projectId: string): Promise<StudioContextResponse> => {
    const res = await api.get<ApiResponse<StudioContextResponse>>(`/projects/${projectId}/studio/context`);
    return res.data.data;
  },

  queryStudio: async (projectId: string, req: StudioQueryRequest): Promise<StudioQueryResponse> => {
    const res = await api.post<ApiResponse<StudioQueryResponse>>(`/projects/${projectId}/studio/query`, req);
    return res.data.data;
  }
};

export const apiService = projectApi;

export function subscribeJobProgress(jobId: string, onEvent: (e: JobProgressEvent) => void, onError?: (err: any) => void) {
  const eventSource = new EventSource(`${apiOrigin}${apiBasePath}/jobs/${jobId}/events`);

  eventSource.addEventListener('CONNECTED', (e: MessageEvent) => {
    try {
      onEvent(JSON.parse(e.data));
    } catch (err) {}
  });

  eventSource.addEventListener('PROGRESS', (e: MessageEvent) => {
    try {
      const data: JobProgressEvent = JSON.parse(e.data);
      onEvent(data);
      if (data.status === 'COMPLETED' || data.status === 'FAILED') {
        eventSource.close();
      }
    } catch (err) {}
  });

  eventSource.onerror = (err) => {
    if (onError) onError(err);
    eventSource.close();
  };

  return () => {
    eventSource.close();
  };
}
