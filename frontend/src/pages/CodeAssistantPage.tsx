import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Code,
  FileCode,
  GitPullRequest,
  CheckCircle,
  XCircle,
  ArrowRight,
  ShieldCheck,
  Plus,
  Play,
  RotateCw,
  Terminal,
  FileCheck,
  Check,
  Copy,
  FolderGit2
} from 'lucide-react';
import { projectApi } from '../api/client';
import { CodeProposal, CodeChange, GeneratedTest, ProposalDetailResponse } from '../types';

export const CodeAssistantPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  // State
  const [proposals, setProposals] = useState<CodeProposal[]>([]);
  const [selectedProposal, setSelectedProposal] = useState<CodeProposal | null>(null);
  const [changes, setChanges] = useState<CodeChange[]>([]);
  const [tests, setTests] = useState<GeneratedTest[]>([]);
  const [selectedChangeIndex, setSelectedChangeIndex] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<'DIFF' | 'TESTS' | 'NEW'>('DIFF');
  const [copied, setCopied] = useState<boolean>(false);

  // Form State
  const [moduleName, setModuleName] = useState<string>('Inventory');
  const [moduleDesc, setModuleDesc] = useState<string>('Stock allocation and warehouse reorder threshold validation');
  const [targetLang, setTargetLang] = useState<string>('JAVA');
  const [providerType, setProviderType] = useState<string>('AUTO');

  useEffect(() => {
    if (!projectId) return;
    loadProposals();
  }, [projectId]);

  const loadProposals = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const list = await projectApi.getProposals(projectId);
      setProposals(list);
      if (list.length > 0) {
        loadProposalDetails(list[0].id);
      } else {
        setSelectedProposal(null);
        setChanges([]);
        setTests([]);
        setActiveTab('NEW');
      }
    } catch (err) {
      console.error('Failed to load proposals', err);
    } finally {
      setLoading(false);
    }
  };

  const loadProposalDetails = async (proposalId: string) => {
    if (!projectId) return;
    try {
      const details: ProposalDetailResponse = await projectApi.getProposalDetails(projectId, proposalId);
      setSelectedProposal(details.proposal);
      setChanges(details.changes);
      setTests(details.tests);
      setSelectedChangeIndex(0);
      setActiveTab('DIFF');
    } catch (err) {
      console.error('Failed to load proposal details', err);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !moduleName.trim()) return;

    setLoading(true);
    try {
      const res = await projectApi.generateProposal(projectId, {
        moduleName,
        title: `Implementation: ${moduleName} Domain Module`,
        description: moduleDesc,
        targetLanguage: targetLang,
        providerType
      });
      setProposals((prev) => [res.proposal, ...prev]);
      setSelectedProposal(res.proposal);
      setChanges(res.changes);
      setTests(res.tests);
      setSelectedChangeIndex(0);
      setActiveTab('DIFF');
    } catch (err: any) {
      alert(`Generation failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!projectId || !selectedProposal) return;
    setActionLoading(true);
    try {
      const updated = await projectApi.approveProposal(projectId, selectedProposal.id);
      setSelectedProposal(updated);
      setProposals((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    } catch (err: any) {
      alert(`Approval failed: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!projectId || !selectedProposal) return;
    setActionLoading(true);
    try {
      const updated = await projectApi.rejectProposal(projectId, selectedProposal.id, 'User rejected change');
      setSelectedProposal(updated);
      setProposals((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    } catch (err: any) {
      alert(`Rejection failed: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleApply = async () => {
    if (!projectId || !selectedProposal) return;
    if (selectedProposal.status !== 'APPROVED') {
      alert('Security Guardrail: Proposal must be APPROVED before applying changes to disk.');
      return;
    }

    if (!confirm('Apply these changes directly to your project workspace files?')) return;

    setActionLoading(true);
    try {
      const updated = await projectApi.applyProposal(projectId, selectedProposal.id, 'DEVELOPER');
      setSelectedProposal(updated);
      setProposals((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      alert('Success! Code changes applied to project storage.');
    } catch (err: any) {
      alert(`Application failed: ${err.response?.data?.message || err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const currentChange = changes[selectedChangeIndex];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <Code className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold tracking-tight text-white">AI Development Assistant</h1>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            AST-informed module generation, diff previews, automated unit tests, and explicit user-approval guardrails.
          </p>
        </div>

        <button
          onClick={() => setActiveTab('NEW')}
          className="flex items-center gap-2 px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold shadow-md transition-all self-start"
        >
          <Plus className="h-4 w-4" />
          Implement New Module
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Column: Proposals Sidebar */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col h-[750px]">
          <div className="flex items-center justify-between mb-3 border-b border-slate-800 pb-2">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <GitPullRequest className="h-4 w-4 text-emerald-400" />
              Proposals ({proposals.length})
            </h2>
            <button
              onClick={loadProposals}
              title="Refresh"
              className="text-slate-400 hover:text-white transition-colors"
            >
              <RotateCw className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {proposals.length === 0 ? (
              <div className="text-center py-10 text-slate-500 text-xs">
                No proposals yet. Generate your first module to preview code diffs.
              </div>
            ) : (
              proposals.map((p) => {
                const isSelected = selectedProposal?.id === p.id;
                const statusColor =
                  p.status === 'APPLIED'
                    ? 'bg-purple-900/40 text-purple-400 border-purple-700/50'
                    : p.status === 'APPROVED'
                    ? 'bg-emerald-900/40 text-emerald-400 border-emerald-700/50'
                    : p.status === 'REJECTED'
                    ? 'bg-rose-900/40 text-rose-400 border-rose-700/50'
                    : 'bg-amber-900/40 text-amber-400 border-amber-700/50';

                return (
                  <div
                    key={p.id}
                    onClick={() => loadProposalDetails(p.id)}
                    className={`p-3 rounded-lg border text-left cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-slate-800/90 border-emerald-500/70 shadow-sm'
                        : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="font-semibold text-xs text-white truncate max-w-[140px]">
                        {p.moduleName}
                      </span>
                      <span className={`px-2 py-0.5 text-[10px] font-mono rounded border ${statusColor}`}>
                        {p.status}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-400 line-clamp-2 mb-2">{p.title}</p>
                    <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono">
                      <span>{p.targetLanguage}</span>
                      <span>{new Date(p.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right 3 Columns: Proposal Detail / Diff / Generator */}
        <div className="lg:col-span-3 bg-slate-900 border border-slate-800 rounded-xl flex flex-col h-[750px] overflow-hidden">
          {activeTab === 'NEW' ? (
            /* New Module Generator Form */
            <div className="p-6 overflow-y-auto">
              <div className="max-w-2xl mx-auto space-y-6">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <FolderGit2 className="h-5 w-5 text-emerald-400" />
                    Generate New Domain Module
                  </h2>
                  <p className="text-xs text-slate-400 mt-1">
                    AI Project Architect will synthesize clean domain models, repositories, business-rule-verified services,
                    REST controllers, and automated unit tests.
                  </p>
                </div>

                <form onSubmit={handleGenerate} className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Module Name</label>
                    <input
                      type="text"
                      value={moduleName}
                      onChange={(e) => setModuleName(e.target.value)}
                      placeholder="e.g. Billing, Notification, Shipping"
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Functional Description & Requirements</label>
                    <textarea
                      value={moduleDesc}
                      onChange={(e) => setModuleDesc(e.target.value)}
                      rows={3}
                      placeholder="Describe the module's responsibilities, entities, and business constraints..."
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1">Target Language / Framework</label>
                      <select
                        value={targetLang}
                        onChange={(e) => setTargetLang(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                      >
                        <option value="JAVA">Java (Spring Boot 3 + JPA)</option>
                        <option value="TYPESCRIPT">TypeScript (React + State Hook)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1">AI Provider Mode</label>
                      <select
                        value={providerType}
                        onChange={(e) => setProviderType(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                      >
                        <option value="AUTO">Automatic Routing (Best Model)</option>
                        <option value="GEMINI">Google Gemini 1.5 Pro</option>
                        <option value="OPENAI">OpenAI GPT-4o</option>
                        <option value="CLAUDE">Anthropic Claude 3.5 Sonnet</option>
                      </select>
                    </div>
                  </div>

                  <div className="p-3 bg-emerald-950/20 border border-emerald-800/40 rounded-lg flex items-start gap-2.5">
                    <ShieldCheck className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
                    <p className="text-xs text-emerald-300/90 leading-relaxed">
                      <strong>Business Rule Preservation:</strong> The generator automatically binds existing verified
                      project rules to the generated service validation pipeline to prevent regressions.
                    </p>
                  </div>

                  <div className="flex justify-end gap-3 pt-2">
                    {selectedProposal && (
                      <button
                        type="button"
                        onClick={() => setActiveTab('DIFF')}
                        className="px-4 py-2 border border-slate-700 text-slate-300 hover:bg-slate-800 rounded-lg text-xs font-medium"
                      >
                        Cancel
                      </button>
                    )}
                    <button
                      type="submit"
                      disabled={loading}
                      className="flex items-center gap-2 px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold transition-all disabled:opacity-50"
                    >
                      {loading ? <RotateCw className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
                      Synthesize Proposal
                    </button>
                  </div>
                </form>
              </div>
            </div>
          ) : selectedProposal ? (
            /* Proposal Detail & Diff View */
            <div className="flex flex-col h-full">
              {/* Proposal Banner & Action Bar */}
              <div className="p-4 border-b border-slate-800 bg-slate-950/40 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2.5">
                    <h2 className="text-base font-bold text-white">{selectedProposal.title}</h2>
                    <span className="px-2 py-0.5 text-xs font-mono rounded bg-slate-800 text-slate-300 border border-slate-700">
                      {selectedProposal.targetLanguage}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1 max-w-xl truncate">
                    {selectedProposal.description}
                  </p>
                </div>

                {/* Guardrail Approval Buttons */}
                <div className="flex items-center gap-2 shrink-0">
                  {selectedProposal.status === 'PENDING_REVIEW' && (
                    <>
                      <button
                        onClick={handleReject}
                        disabled={actionLoading}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-rose-950/50 hover:bg-rose-900 border border-rose-800/60 text-rose-300 rounded-lg text-xs font-medium transition-all"
                      >
                        <XCircle className="h-4 w-4" />
                        Reject
                      </button>
                      <button
                        onClick={handleApprove}
                        disabled={actionLoading}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold shadow-md transition-all"
                      >
                        <CheckCircle className="h-4 w-4" />
                        Approve Proposal
                      </button>
                    </>
                  )}

                  {selectedProposal.status === 'APPROVED' && (
                    <button
                      onClick={handleApply}
                      disabled={actionLoading}
                      className="flex items-center gap-1.5 px-4 py-1.5 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-semibold shadow-md transition-all"
                    >
                      <FileCheck className="h-4 w-4" />
                      Apply to Workspace
                    </button>
                  )}

                  {selectedProposal.status === 'APPLIED' && (
                    <div className="flex items-center gap-1.5 text-xs font-mono text-purple-400 bg-purple-950/50 px-3 py-1.5 rounded-lg border border-purple-800/60">
                      <Check className="h-4 w-4" />
                      Applied to Workspace by {selectedProposal.appliedBy || 'USER'}
                    </div>
                  )}
                </div>
              </div>

              {/* Sub-Tabs: Code Diffs vs Generated Tests */}
              <div className="flex items-center justify-between px-4 border-b border-slate-800 bg-slate-900/80">
                <div className="flex gap-2">
                  <button
                    onClick={() => setActiveTab('DIFF')}
                    className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                      activeTab === 'DIFF'
                        ? 'border-emerald-500 text-white'
                        : 'border-transparent text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    File Diffs ({changes.length})
                  </button>
                  <button
                    onClick={() => setActiveTab('TESTS')}
                    className={`px-3 py-2 text-xs font-semibold border-b-2 transition-all ${
                      activeTab === 'TESTS'
                        ? 'border-emerald-500 text-white'
                        : 'border-transparent text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    Automated Unit Tests ({tests.length})
                  </button>
                </div>

                {activeTab === 'DIFF' && currentChange && (
                  <div className="flex items-center gap-3 text-xs font-mono">
                    <span className="text-emerald-400">+{currentChange.additionsCount}</span>
                    <span className="text-rose-400">-{currentChange.deletionsCount}</span>
                  </div>
                )}
              </div>

              {/* Tab Contents */}
              {activeTab === 'DIFF' ? (
                <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
                  {/* File Selector */}
                  <div className="w-full md:w-64 border-r border-slate-800 bg-slate-950/50 p-2 overflow-y-auto">
                    <div className="text-[10px] uppercase font-semibold text-slate-500 px-2 py-1 mb-1">
                      Modified Files
                    </div>
                    {changes.map((c, idx) => (
                      <button
                        key={c.id}
                        onClick={() => setSelectedChangeIndex(idx)}
                        className={`w-full text-left px-2.5 py-2 rounded text-xs font-mono truncate flex items-center justify-between mb-1 ${
                          selectedChangeIndex === idx
                            ? 'bg-slate-800 text-emerald-400 border border-slate-700'
                            : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                        }`}
                      >
                        <span className="truncate">{c.filePath.split('/').pop()}</span>
                        <span
                          className={`text-[9px] px-1 rounded ${
                            c.changeType === 'CREATE' ? 'bg-emerald-950 text-emerald-400' : 'bg-blue-950 text-blue-400'
                          }`}
                        >
                          {c.changeType}
                        </span>
                      </button>
                    ))}
                  </div>

                  {/* Diff Viewer */}
                  <div className="flex-1 flex flex-col overflow-hidden bg-slate-950">
                    {currentChange && (
                      <>
                        <div className="px-4 py-2 border-b border-slate-800 bg-slate-900/60 flex items-center justify-between text-xs">
                          <span className="font-mono text-slate-300">{currentChange.filePath}</span>
                          <span className="text-[11px] text-emerald-400/90 font-mono">
                            {currentChange.businessRulesPreserved}
                          </span>
                        </div>
                        <div className="flex-1 p-4 overflow-auto font-mono text-xs leading-relaxed text-slate-300">
                          <pre className="whitespace-pre-wrap">
                            {currentChange.diffUnified?.split('\n').map((line, lIdx) => {
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
              ) : (
                /* Generated Tests Tab */
                <div className="flex-1 p-6 overflow-y-auto space-y-4">
                  {tests.length === 0 ? (
                    <div className="text-center py-12 text-slate-500 text-xs">
                      No automated tests generated for this proposal.
                    </div>
                  ) : (
                    tests.map((t) => (
                      <div key={t.id} className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="text-sm font-bold text-white font-mono">{t.testFilePath}</div>
                            <div className="text-xs text-slate-400">
                              Framework: <span className="font-mono text-emerald-400">{t.testFramework}</span> | Target: {t.targetFilePath}
                            </div>
                          </div>
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(t.testCode);
                              setCopied(true);
                              setTimeout(() => setCopied(false), 2000);
                            }}
                            className="flex items-center gap-1.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs font-mono"
                          >
                            {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                            {copied ? 'Copied' : 'Copy Test'}
                          </button>
                        </div>
                        <pre className="bg-slate-900 border border-slate-800/80 rounded-lg p-3 text-xs font-mono text-slate-300 overflow-x-auto">
                          {t.testCode}
                        </pre>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 text-xs p-8 text-center">
              <Code className="h-10 w-10 text-slate-600 mb-3" />
              <p>Select a proposal on the left or generate a new module.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
