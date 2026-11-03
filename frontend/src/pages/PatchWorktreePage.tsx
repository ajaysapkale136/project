import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  GitPullRequest,
  GitBranch,
  ShieldAlert,
  ShieldCheck,
  CheckCircle,
  XCircle,
  RotateCcw,
  Play,
  RotateCw,
  Download,
  AlertTriangle,
  FileCode,
  FileText,
  Database,
  Terminal,
  Layers,
  ChevronDown,
  ChevronUp,
  Check,
  X,
  FileArchive
} from 'lucide-react';
import { projectApi } from '../api/client';
import {
  ChangeSet,
  ChangeSetFile,
  ChangeSetResponse,
  TestExecutionReportResponse
} from '../types';

export const PatchWorktreePage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  // State
  const [changeSets, setChangeSets] = useState<ChangeSet[]>([]);
  const [selectedChangeSet, setSelectedChangeSet] = useState<ChangeSet | null>(null);
  const [currentResponse, setCurrentResponse] = useState<ChangeSetResponse | null>(null);
  const [selectedFileIndex, setSelectedFileIndex] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [testModalOpen, setTestModalOpen] = useState<boolean>(false);
  const [testReport, setTestReport] = useState<TestExecutionReportResponse | null>(null);
  const [activeTab, setActiveTab] = useState<'PLAN' | 'DIFF' | 'RULES' | 'API_DB'>('PLAN');

  // Generator Form State
  const [targetType, setTargetType] = useState<string>('MODULE');
  const [targetId, setTargetId] = useState<string>('BillingEngine');
  const [title, setTitle] = useState<string>('Implement Automated Recurring Billing Engine');
  const [description, setDescription] = useState<string>(
    'Add subscription cycle billing with idempotency and state tax validation'
  );

  useEffect(() => {
    if (!projectId) return;
    loadChangeSets();
  }, [projectId]);

  const loadChangeSets = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const list = await projectApi.getChangeSets(projectId);
      setChangeSets(list);
      if (list.length > 0) {
        loadDetails(list[0].id);
      } else {
        setSelectedChangeSet(null);
        setCurrentResponse(null);
      }
    } catch (err) {
      console.error('Failed to load change sets', err);
    } finally {
      setLoading(false);
    }
  };

  const loadDetails = async (id: string) => {
    if (!projectId) return;
    try {
      const res = await projectApi.getChangeSetDetails(projectId, id);
      setSelectedChangeSet(res.changeSet);
      setCurrentResponse(res);
      setSelectedFileIndex(0);
    } catch (err) {
      console.error('Failed to load change set details', err);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !targetId.trim()) return;

    setLoading(true);
    try {
      const res = await projectApi.createChangeSet(projectId, {
        targetType,
        targetIdentifier: targetId,
        title,
        description
      });
      setChangeSets((prev) => [res.changeSet, ...prev]);
      setSelectedChangeSet(res.changeSet);
      setCurrentResponse(res);
      setSelectedFileIndex(0);
      setActiveTab('PLAN');
    } catch (err: any) {
      alert(`Generation failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!projectId || !selectedChangeSet) return;
    setActionLoading(true);
    try {
      const updated = await projectApi.approveChangeSet(projectId, selectedChangeSet.id);
      setSelectedChangeSet(updated);
      setChangeSets((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
    } catch (err: any) {
      alert(`Approval failed: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!projectId || !selectedChangeSet) return;
    setActionLoading(true);
    try {
      const updated = await projectApi.rejectChangeSet(projectId, selectedChangeSet.id);
      setSelectedChangeSet(updated);
      setChangeSets((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
    } catch (err: any) {
      alert(`Rejection failed: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleApply = async () => {
    if (!projectId || !selectedChangeSet) return;
    if (selectedChangeSet.status !== 'APPROVED') {
      alert('Security Guardrail: Change set must be explicitly APPROVED before application.');
      return;
    }

    setActionLoading(true);
    try {
      const updated = await projectApi.applyChangeSet(projectId, selectedChangeSet.id);
      setSelectedChangeSet(updated);
      setChangeSets((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      alert('Success! Change set applied to isolated worktree.');
    } catch (err: any) {
      alert(`Application failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRollback = async () => {
    if (!projectId || !selectedChangeSet) return;
    if (!confirm('Rollback this applied change set and revert all modified files in worktree?')) return;

    setActionLoading(true);
    try {
      const res = await projectApi.rollbackChangeSet(projectId, selectedChangeSet.id);
      setSelectedChangeSet((prev) => (prev ? { ...prev, status: 'ROLLED_BACK' } : null));
      setChangeSets((prev) =>
        prev.map((c) => (c.id === selectedChangeSet.id ? { ...c, status: 'ROLLED_BACK' } : c))
      );
      alert(`Rollback complete: ${res.message}`);
    } catch (err: any) {
      alert(`Rollback failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleFileDecision = async (fileId: string, decision: 'ACCEPTED' | 'REJECTED') => {
    if (!projectId) return;
    try {
      const updated = await projectApi.updateFileDecision(projectId, fileId, decision);
      if (currentResponse) {
        const newFiles = currentResponse.files.map((f) => (f.id === updated.id ? updated : f));
        setCurrentResponse({ ...currentResponse, files: newFiles });
      }
    } catch (err: any) {
      alert(`Failed to update file decision: ${err.message}`);
    }
  };

  const handleRunTests = async () => {
    if (!projectId) return;
    setActionLoading(true);
    try {
      const report = await projectApi.runSandboxedTests(projectId, {
        changeSetId: selectedChangeSet?.id,
        testType: 'ALL',
        timeoutSeconds: 45
      });
      setTestReport(report);
      setTestModalOpen(true);
    } catch (err: any) {
      alert(`Test execution failed: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const currentFile = currentResponse?.files[selectedFileIndex];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <GitBranch className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Worktree & Patch-Based Development
            </h1>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Isolated worktrees, character-accurate diffs, business rule preservation, staleness conflict rejection, and rollback history.
          </p>
        </div>

        {/* Global Export Buttons */}
        {projectId && (
          <div className="flex items-center gap-2">
            <a
              href={projectApi.exportWorkspaceZipUrl(projectId)}
              download={`workspace-${projectId}.zip`}
              className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold shadow-sm transition-all"
            >
              <FileArchive className="h-4 w-4 text-emerald-400" />
              Download Worktree ZIP
            </a>
          </div>
        )}
      </div>

      {/* Main 2-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Sidebar: New Change Set & History */}
        <div className="space-y-6">
          {/* New Change Set Form */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-4">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
              <GitPullRequest className="h-4 w-4 text-emerald-400" />
              New Change Set
            </h2>

            <form onSubmit={handleGenerate} className="space-y-3">
              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Target Type</label>
                <select
                  value={targetType}
                  onChange={(e) => setTargetType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-white"
                >
                  <option value="MODULE">Domain Module</option>
                  <option value="COMPONENT">Component</option>
                  <option value="BUG">Bug Fix</option>
                  <option value="FEATURE">Feature Request</option>
                  <option value="REFACTOR">Refactoring</option>
                  <option value="API_OPERATION">API Operation</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Target Identifier</label>
                <input
                  type="text"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  placeholder="e.g. BillingEngine, OrderValidation"
                  className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-white"
                  required
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Title</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-white"
                  required
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={2}
                  className="w-full bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-white"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-1.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold shadow-md transition-all disabled:opacity-50"
              >
                {loading ? <RotateCw className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5" />}
                Synthesize Patch Plan
              </button>
            </form>
          </div>

          {/* Change Sets History */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col max-h-[380px]">
            <div className="flex items-center justify-between mb-3 border-b border-slate-800 pb-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Change Sets ({changeSets.length})
              </span>
              <button onClick={loadChangeSets} title="Refresh" className="text-slate-400 hover:text-white">
                <RotateCw className="h-3.5 w-3.5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 pr-1">
              {changeSets.map((c) => {
                const isSelected = selectedChangeSet?.id === c.id;
                const statusColor =
                  c.status === 'APPLIED'
                    ? 'bg-purple-950 text-purple-400 border-purple-800'
                    : c.status === 'APPROVED'
                    ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                    : c.status === 'ROLLED_BACK'
                    ? 'bg-amber-950 text-amber-400 border-amber-800'
                    : c.status === 'STALE_CONFLICT'
                    ? 'bg-rose-950 text-rose-400 border-rose-800'
                    : 'bg-slate-800 text-slate-300 border-slate-700';

                return (
                  <div
                    key={c.id}
                    onClick={() => loadDetails(c.id)}
                    className={`p-3 rounded-lg border text-left cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-slate-800 border-emerald-500 shadow-sm'
                        : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-bold text-xs text-white truncate max-w-[140px]">
                        {c.targetIdentifier}
                      </span>
                      <span className={`px-1.5 py-0.5 text-[9px] font-mono rounded border ${statusColor}`}>
                        {c.status}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-400 line-clamp-1">{c.title}</p>
                    <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono mt-1">
                      <span>{c.targetType}</span>
                      <span>{new Date(c.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right 3 Columns: Active Change Set Inspector */}
        <div className="lg:col-span-3 bg-slate-900 border border-slate-800 rounded-xl flex flex-col h-[750px] overflow-hidden">
          {selectedChangeSet && currentResponse ? (
            <div className="flex flex-col h-full">
              {/* Staleness Conflict Alert Banner */}
              {currentResponse.isStale && (
                <div className="p-3 bg-rose-950/80 border-b border-rose-800 flex items-center justify-between gap-3 text-rose-200 text-xs">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="h-5 w-5 text-rose-400 shrink-0" />
                    <span>
                      <strong>STALE CONFLICT:</strong> The project source snapshot has advanced since this change set was synthesized.
                      Application is blocked to prevent overwriting newer changes.
                    </span>
                  </div>
                  <span className="px-2 py-0.5 bg-rose-900 rounded font-mono text-[10px] uppercase font-bold">
                    RE-SYNTHESIZE REQUIRED
                  </span>
                </div>
              )}

              {/* Change Set Header & Action Ribbon */}
              <div className="p-4 border-b border-slate-800 bg-slate-950/50 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2.5">
                    <h2 className="text-base font-bold text-white">{selectedChangeSet.title}</h2>
                    <span className="px-2 py-0.5 text-xs font-mono rounded bg-slate-800 text-slate-300 border border-slate-700">
                      {selectedChangeSet.targetType}
                    </span>
                    <span
                      className={`px-2 py-0.5 text-xs font-mono rounded border ${
                        selectedChangeSet.riskLevel === 'LOW'
                          ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                          : 'bg-amber-950 text-amber-400 border-amber-800'
                      }`}
                    >
                      {selectedChangeSet.riskLevel} RISK
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1 max-w-xl truncate">
                    {selectedChangeSet.description}
                  </p>
                </div>

                {/* Governance Guardrails: Approval, Apply, Rollback */}
                <div className="flex items-center gap-2 shrink-0">
                  {selectedChangeSet.status === 'PENDING_APPROVAL' && (
                    <>
                      <button
                        onClick={handleReject}
                        disabled={actionLoading}
                        className="flex items-center gap-1 px-3 py-1.5 bg-rose-950/60 hover:bg-rose-900 border border-rose-800 text-rose-300 rounded text-xs font-medium"
                      >
                        <XCircle className="h-3.5 w-3.5" />
                        Reject
                      </button>
                      <button
                        onClick={handleApprove}
                        disabled={actionLoading}
                        className="flex items-center gap-1 px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold shadow-sm"
                      >
                        <CheckCircle className="h-3.5 w-3.5" />
                        Approve Change Set
                      </button>
                    </>
                  )}

                  {selectedChangeSet.status === 'APPROVED' && (
                    <button
                      onClick={handleApply}
                      disabled={actionLoading || currentResponse.isStale}
                      className="flex items-center gap-1.5 px-4 py-1.5 bg-purple-600 hover:bg-purple-500 text-white rounded text-xs font-semibold shadow-md disabled:opacity-50"
                    >
                      <Play className="h-3.5 w-3.5" />
                      Apply to Isolated Worktree
                    </button>
                  )}

                  {selectedChangeSet.status === 'APPLIED' && (
                    <>
                      <button
                        onClick={handleRollback}
                        disabled={actionLoading}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-950 hover:bg-amber-900 border border-amber-800 text-amber-300 rounded text-xs font-medium"
                      >
                        <RotateCcw className="h-3.5 w-3.5" />
                        Rollback Change Set
                      </button>
                      <button
                        onClick={handleRunTests}
                        disabled={actionLoading}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-cyan-600 hover:bg-cyan-500 text-white rounded text-xs font-semibold shadow-sm"
                      >
                        <Terminal className="h-3.5 w-3.5" />
                        Run Sandboxed Tests
                      </button>
                    </>
                  )}

                  <a
                    href={projectApi.exportPatchFile(projectId!, selectedChangeSet.id)}
                    download={`changeset-${selectedChangeSet.id}.patch`}
                    className="flex items-center gap-1 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 rounded text-xs font-medium"
                  >
                    <Download className="h-3.5 w-3.5" />
                    .patch
                  </a>
                </div>
              </div>

              {/* Navigation Tabs */}
              <div className="flex border-b border-slate-800 bg-slate-900 px-4 pt-1">
                <button
                  onClick={() => setActiveTab('PLAN')}
                  className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                    activeTab === 'PLAN'
                      ? 'border-emerald-500 text-white'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Technical Plan
                </button>
                <button
                  onClick={() => setActiveTab('DIFF')}
                  className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                    activeTab === 'DIFF'
                      ? 'border-emerald-500 text-white'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  File-by-File Diffs ({currentResponse.files.length})
                </button>
                <button
                  onClick={() => setActiveTab('RULES')}
                  className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                    activeTab === 'RULES'
                      ? 'border-emerald-500 text-white'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Preserved Business Rules ({currentResponse.businessRuleImpact.length})
                </button>
                <button
                  onClick={() => setActiveTab('API_DB')}
                  className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                    activeTab === 'API_DB'
                      ? 'border-emerald-500 text-white'
                      : 'border-transparent text-slate-400 hover:text-slate-200'
                  }`}
                >
                  API & Database Impact
                </button>
              </div>

              {/* Tab Contents */}
              <div className="flex-1 overflow-y-auto">
                {activeTab === 'PLAN' && (
                  <div className="p-6 space-y-6">
                    {/* Implementation Steps */}
                    <div className="space-y-2">
                      <h3 className="text-xs uppercase font-bold text-slate-400 tracking-wider">
                        Step-by-Step Implementation Sequence
                      </h3>
                      <div className="space-y-2">
                        {currentResponse.technicalPlan.steps.map((st, i) => (
                          <div
                            key={i}
                            className="p-3 bg-slate-950 border border-slate-800 rounded-lg text-xs font-mono text-slate-200 flex items-center gap-3"
                          >
                            <span className="h-5 w-5 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-800 flex items-center justify-center font-bold text-[10px]">
                              {i + 1}
                            </span>
                            <span>{st}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Detected Conventions & Affected Components */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2">
                        <h4 className="text-xs uppercase font-bold text-slate-400">Detected Conventions</h4>
                        <ul className="space-y-1 text-xs font-mono text-slate-300">
                          {currentResponse.technicalPlan.detectedConventions.map((c, i) => (
                            <li key={i} className="flex items-center gap-2">
                              <span className="text-emerald-400">•</span> {c}
                            </li>
                          ))}
                        </ul>
                      </div>

                      <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2">
                        <h4 className="text-xs uppercase font-bold text-slate-400">Affected Scope</h4>
                        <div className="text-xs font-mono text-slate-300 space-y-1">
                          <div>
                            <strong>Modules:</strong> {currentResponse.technicalPlan.affectedModules.join(', ')}
                          </div>
                          <div>
                            <strong>Endpoints:</strong> {currentResponse.technicalPlan.affectedEndpoints.join(', ')}
                          </div>
                          <div>
                            <strong>Database Tables:</strong> {currentResponse.technicalPlan.affectedDatabaseTables.join(', ')}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Risk Assessment */}
                    <div className="p-3 bg-emerald-950/20 border border-emerald-800/40 rounded-lg flex items-start gap-2.5">
                      <ShieldCheck className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                      <div className="text-xs text-emerald-300/90 leading-relaxed">
                        <strong>Risk Assessment:</strong> {currentResponse.technicalPlan.riskAssessment}
                      </div>
                    </div>
                  </div>
                )}

                {activeTab === 'DIFF' && (
                  <div className="flex h-full flex-col md:flex-row">
                    {/* File Selector & Accept/Reject Toggles */}
                    <div className="w-full md:w-72 border-r border-slate-800 bg-slate-950/50 p-2 overflow-y-auto space-y-1">
                      <div className="text-[10px] uppercase font-bold text-slate-500 px-2 py-1">
                        Files in Patch ({currentResponse.files.length})
                      </div>
                      {currentResponse.files.map((f, idx) => (
                        <div
                          key={f.id}
                          className={`p-2 rounded text-xs font-mono flex items-center justify-between cursor-pointer ${
                            selectedFileIndex === idx
                              ? 'bg-slate-800 border border-slate-700 text-white'
                              : 'text-slate-400 hover:bg-slate-900'
                          }`}
                          onClick={() => setSelectedFileIndex(idx)}
                        >
                          <span className="truncate max-w-[140px]">{f.filePath.split('/').pop()}</span>
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`text-[9px] px-1 rounded ${
                                f.status === 'ACCEPTED'
                                  ? 'bg-emerald-950 text-emerald-400'
                                  : f.status === 'REJECTED'
                                  ? 'bg-rose-950 text-rose-400'
                                  : 'bg-slate-800 text-slate-400'
                              }`}
                            >
                              {f.status}
                            </span>
                            {/* Individual Accept/Reject Toggles */}
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleFileDecision(f.id, 'ACCEPTED');
                              }}
                              title="Accept this file"
                              className="text-slate-500 hover:text-emerald-400"
                            >
                              <Check className="h-3 w-3" />
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleFileDecision(f.id, 'REJECTED');
                              }}
                              title="Reject this file"
                              className="text-slate-500 hover:text-rose-400"
                            >
                              <X className="h-3 w-3" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Diff Viewer Pane */}
                    <div className="flex-1 flex flex-col overflow-hidden bg-slate-950">
                      {currentFile && (
                        <>
                          <div className="px-4 py-2 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between text-xs">
                            <span className="font-mono text-slate-300">{currentFile.filePath}</span>
                            <div className="flex items-center gap-3 text-xs font-mono">
                              <span className="text-emerald-400">+{currentFile.additionsCount}</span>
                              <span className="text-rose-400">-{currentFile.deletionsCount}</span>
                              <span className="text-slate-500">[{currentFile.explanation}]</span>
                            </div>
                          </div>
                          <div className="flex-1 p-4 overflow-auto font-mono text-xs leading-relaxed text-slate-300">
                            <pre className="whitespace-pre-wrap">
                              {currentFile.diffUnified?.split('\n').map((line, lIdx) => {
                                const isAdd = line.startsWith('+') && !line.startsWith('+++');
                                const isDel = line.startsWith('-') && !line.startsWith('---');
                                const isHdr = line.startsWith('@@') || line.startsWith('---') || line.startsWith('+++');

                                return (
                                  <div
                                    key={lIdx}
                                    className={
                                      isAdd
                                        ? 'bg-emerald-950/40 text-emerald-300 px-2 py-0.5'
                                        : isDel
                                        ? 'bg-rose-950/40 text-rose-300 px-2 py-0.5'
                                        : isHdr
                                        ? 'text-cyan-400 font-bold px-2 py-0.5'
                                        : 'px-2 py-0.5'
                                    }
                                  >
                                    {line}
                                  </div>
                                );
                              })}
                            </pre>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                )}

                {activeTab === 'RULES' && (
                  <div className="p-6 space-y-4">
                    {currentResponse.businessRuleImpact.map((r, i) => (
                      <div
                        key={i}
                        className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                              {r.ruleCode}
                            </span>
                            <h4 className="font-bold text-sm text-white">{r.title}</h4>
                          </div>
                          <span className="text-xs font-mono text-slate-400">{r.affectedClass}</span>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                          <div className="p-2.5 bg-slate-900 border border-slate-800 rounded">
                            <span className="font-semibold text-slate-400 block mb-1">Existing Rule:</span>
                            <p className="text-slate-300 leading-relaxed">{r.existingRule}</p>
                          </div>
                          <div className="p-2.5 bg-slate-900 border border-slate-800 rounded">
                            <span className="font-semibold text-emerald-400 block mb-1">Proposed Change & Verification:</span>
                            <p className="text-slate-300 leading-relaxed">{r.proposedChange}</p>
                          </div>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-500 pt-1 border-t border-slate-800/80">
                          <span>API Compatibility: {r.apiCompatibilityRisk}</span>
                          <span>Regression Test: {r.regressionTests}</span>
                          <span>Rollback Plan: {r.rollbackPlan}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {activeTab === 'API_DB' && (
                  <div className="p-6 space-y-6">
                    {/* API Compatibility Scorecard */}
                    <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <h4 className="text-xs uppercase font-bold text-slate-400">API Compatibility Impact</h4>
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                            currentResponse.apiCompatibilityImpact.isBackwardCompatible
                              ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                              : 'bg-rose-950 text-rose-400 border-rose-800'
                          }`}
                        >
                          {currentResponse.apiCompatibilityImpact.isBackwardCompatible
                            ? '100% BACKWARD COMPATIBLE'
                            : 'BREAKING CHANGE DETECTED'}
                        </span>
                      </div>

                      <div className="text-xs font-mono space-y-1 text-slate-300">
                        <div>
                          <strong>Compatible Additions:</strong>
                          <ul className="list-disc list-inside ml-2 text-emerald-400">
                            {currentResponse.apiCompatibilityImpact.compatibleAdditions.map((a, i) => (
                              <li key={i}>{a}</li>
                            ))}
                          </ul>
                        </div>
                        {currentResponse.apiCompatibilityImpact.breakingChanges.length > 0 && (
                          <div>
                            <strong>Breaking Changes:</strong>
                            <ul className="list-disc list-inside ml-2 text-rose-400">
                              {currentResponse.apiCompatibilityImpact.breakingChanges.map((b, i) => (
                                <li key={i}>{b}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Database Migration SQL Preview */}
                    <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <h4 className="text-xs uppercase font-bold text-slate-400">Database Migration (Flyway DDL)</h4>
                        <span className="text-[10px] font-mono text-cyan-400">Non-Destructive Additive</span>
                      </div>
                      <pre className="p-3 bg-slate-900 border border-slate-800 rounded text-xs font-mono text-slate-300 overflow-x-auto">
                        {currentResponse.migrationImpact}
                      </pre>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 text-xs p-8 text-center">
              <GitBranch className="h-10 w-10 text-slate-600 mb-3" />
              <p>Select a change set from the left or synthesize a new patch.</p>
            </div>
          )}
        </div>
      </div>

      {/* Sandboxed Test Runner Modal */}
      {testModalOpen && testReport && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-3xl w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Terminal className="h-5 w-5 text-emerald-400" />
                <h3 className="font-bold text-white text-base">Sandboxed Test Execution Results</h3>
              </div>
              <button onClick={() => setTestModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-slate-950 border border-slate-800 rounded-xl text-xs font-mono">
              <div>
                <span className="text-slate-400">Environment: </span>
                <span className="text-white font-bold">{testReport.environment}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-emerald-400 font-bold">{testReport.execution.passedCount} Passed</span>
                <span className="text-rose-400 font-bold">{testReport.execution.failedCount} Failed</span>
                <span className="text-slate-400 font-bold">{testReport.execution.skippedCount} Skipped</span>
              </div>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 font-mono text-xs text-emerald-400/90 max-h-[350px] overflow-auto leading-relaxed shadow-inner">
              <pre className="whitespace-pre-wrap">{testReport.execution.consoleOutput}</pre>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setTestModalOpen(false)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-xs font-semibold"
              >
                Close Report
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
