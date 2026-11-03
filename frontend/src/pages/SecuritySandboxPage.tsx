import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  FileCode,
  Terminal,
  RotateCw,
  Play,
  Lock,
  Network,
  Bug,
  Server
} from 'lucide-react';
import { projectApi } from '../api/client';
import {
  SecurityScan,
  VulnerabilityFinding,
  ContractVerification,
  ContractVerificationMatrix,
  SandboxRunResponse
} from '../types';

export const SecuritySandboxPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  // State
  const [scan, setScan] = useState<SecurityScan | null>(null);
  const [findings, setFindings] = useState<VulnerabilityFinding[]>([]);
  const [contractMatrix, setContractMatrix] = useState<ContractVerificationMatrix | null>(null);
  const [sandboxResult, setSandboxResult] = useState<SandboxRunResponse | null>(null);

  const [loadingScan, setLoadingScan] = useState<boolean>(false);
  const [loadingContracts, setLoadingContracts] = useState<boolean>(false);
  const [loadingSandbox, setLoadingSandbox] = useState<boolean>(false);

  const [activeTab, setActiveTab] = useState<'VULNERABILITIES' | 'CONTRACTS' | 'SANDBOX'>('VULNERABILITIES');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');

  useEffect(() => {
    if (!projectId) return;
    loadSecurityData();
  }, [projectId]);

  const loadSecurityData = async () => {
    if (!projectId) return;
    // Load latest scan
    try {
      const scanRes = await projectApi.getLatestScan(projectId);
      setScan(scanRes.scan);
      setFindings(scanRes.findings);
    } catch (err) {
      console.error('Failed to load scan', err);
    }

    // Load contracts
    try {
      const contractsRes = await projectApi.getContracts(projectId);
      setContractMatrix(contractsRes);
    } catch (err) {
      console.error('Failed to load contracts', err);
    }
  };

  const handleRunScan = async () => {
    if (!projectId) return;
    setLoadingScan(true);
    try {
      const res = await projectApi.runSecurityScan(projectId);
      setScan(res.scan);
      setFindings(res.findings);
      setActiveTab('VULNERABILITIES');
    } catch (err: any) {
      alert(`Security scan failed: ${err.message}`);
    } finally {
      setLoadingScan(false);
    }
  };

  const handleVerifyContracts = async () => {
    if (!projectId) return;
    setLoadingContracts(true);
    try {
      const res = await projectApi.verifyContracts(projectId);
      setContractMatrix(res);
      setActiveTab('CONTRACTS');
    } catch (err: any) {
      alert(`Contract verification failed: ${err.message}`);
    } finally {
      setLoadingContracts(false);
    }
  };

  const handleRunSandbox = async () => {
    if (!projectId) return;
    setLoadingSandbox(true);
    try {
      const res = await projectApi.runSandbox(projectId, { testSuite: 'ALL', timeoutSeconds: 30 });
      setSandboxResult(res);
      setActiveTab('SANDBOX');
    } catch (err: any) {
      alert(`Sandbox execution failed: ${err.message}`);
    } finally {
      setLoadingSandbox(false);
    }
  };

  const filteredFindings = findings.filter((f) => {
    if (severityFilter === 'ALL') return true;
    return f.severity.toUpperCase() === severityFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <Lock className="h-6 w-6 text-emerald-400" />
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Testing & Security Validation Sandbox
            </h1>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Static vulnerability scanning, hardcoded secrets audit, API contract verification, and sandboxed test execution.
          </p>
        </div>

        {/* Action Triggers */}
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={handleRunScan}
            disabled={loadingScan}
            className="flex items-center gap-2 px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold shadow-md transition-all disabled:opacity-50"
          >
            {loadingScan ? <RotateCw className="h-4 w-4 animate-spin" /> : <ShieldAlert className="h-4 w-4" />}
            Run Security Audit
          </button>

          <button
            onClick={handleVerifyContracts}
            disabled={loadingContracts}
            className="flex items-center gap-2 px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold shadow-md transition-all disabled:opacity-50"
          >
            {loadingContracts ? <RotateCw className="h-4 w-4 animate-spin" /> : <Network className="h-4 w-4" />}
            Verify API Contracts
          </button>

          <button
            onClick={handleRunSandbox}
            disabled={loadingSandbox}
            className="flex items-center gap-2 px-3.5 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-semibold shadow-md transition-all disabled:opacity-50"
          >
            {loadingSandbox ? <RotateCw className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
            Run Sandboxed Tests
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Security Gate Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
          <div>
            <div className="text-xs uppercase font-semibold text-slate-400">Security Gate</div>
            <div
              className={`text-lg font-bold mt-1 ${
                scan ? (scan.passed ? 'text-emerald-400' : 'text-rose-400') : 'text-slate-400'
              }`}
            >
              {scan ? (scan.passed ? 'PASSED (CLEAN)' : 'BLOCKED (ACTION REQ)') : 'NOT AUDITED'}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              {scan ? `${scan.scannedFilesCount} files analyzed` : 'Run audit to evaluate'}
            </div>
          </div>
          {scan?.passed ? (
            <ShieldCheck className="h-9 w-9 text-emerald-400/80" />
          ) : (
            <ShieldAlert className="h-9 w-9 text-rose-400/80" />
          )}
        </div>

        {/* Critical & High Findings */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
          <div>
            <div className="text-xs uppercase font-semibold text-slate-400">Critical / High Issues</div>
            <div className="text-2xl font-bold mt-1 text-rose-400">
              {(scan?.criticalCount || 0) + (scan?.highCount || 0)}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              {scan?.criticalCount || 0} Critical, {scan?.highCount || 0} High
            </div>
          </div>
          <AlertTriangle className="h-9 w-9 text-rose-400/80" />
        </div>

        {/* Medium & Low Findings */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
          <div>
            <div className="text-xs uppercase font-semibold text-slate-400">Medium / Low Risks</div>
            <div className="text-2xl font-bold mt-1 text-amber-400">
              {(scan?.mediumCount || 0) + (scan?.lowCount || 0)}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              {scan?.mediumCount || 0} Medium, {scan?.lowCount || 0} Low
            </div>
          </div>
          <Bug className="h-9 w-9 text-amber-400/80" />
        </div>

        {/* Contract Compliance Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
          <div>
            <div className="text-xs uppercase font-semibold text-slate-400">Contract Compliance</div>
            <div className="text-2xl font-bold mt-1 text-cyan-400">
              {contractMatrix?.totalEndpoints
                ? `${Math.round((contractMatrix.compliantCount / contractMatrix.totalEndpoints) * 100)}%`
                : '100%'}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              {contractMatrix?.compliantCount || 0} of {contractMatrix?.totalEndpoints || 0} endpoints
            </div>
          </div>
          <CheckCircle2 className="h-9 w-9 text-cyan-400/80" />
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-800 bg-slate-900 rounded-t-xl px-4 pt-2">
        <button
          onClick={() => setActiveTab('VULNERABILITIES')}
          className={`flex items-center gap-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all ${
            activeTab === 'VULNERABILITIES'
              ? 'border-emerald-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Bug className="h-4 w-4" />
          Vulnerability Findings ({findings.length})
        </button>

        <button
          onClick={() => setActiveTab('CONTRACTS')}
          className={`flex items-center gap-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all ${
            activeTab === 'CONTRACTS'
              ? 'border-emerald-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Network className="h-4 w-4" />
          API Contract Matrix ({contractMatrix?.totalEndpoints || 0})
        </button>

        <button
          onClick={() => setActiveTab('SANDBOX')}
          className={`flex items-center gap-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-all ${
            activeTab === 'SANDBOX'
              ? 'border-emerald-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Terminal className="h-4 w-4" />
          Sandboxed Test Runner
        </button>
      </div>

      {/* Main Tab Panels */}
      <div className="bg-slate-900 border border-slate-800 rounded-b-xl p-6 min-h-[500px]">
        {activeTab === 'VULNERABILITIES' && (
          <div className="space-y-4">
            {/* Filter Chips */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-semibold mr-2">Severity:</span>
              {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
                <button
                  key={sev}
                  onClick={() => setSeverityFilter(sev)}
                  className={`px-3 py-1 rounded-full text-xs font-mono font-semibold transition-all ${
                    severityFilter === sev
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
                  }`}
                >
                  {sev}
                </button>
              ))}
            </div>

            {filteredFindings.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs">
                <ShieldCheck className="h-10 w-10 text-emerald-500 mx-auto mb-2 opacity-80" />
                No vulnerabilities found for filter "{severityFilter}". Codebase passes all security criteria.
              </div>
            ) : (
              <div className="space-y-3">
                {filteredFindings.map((f) => {
                  const badgeColor =
                    f.severity === 'CRITICAL'
                      ? 'bg-rose-950/70 text-rose-300 border-rose-800'
                      : f.severity === 'HIGH'
                      ? 'bg-amber-950/70 text-amber-300 border-amber-800'
                      : 'bg-blue-950/70 text-blue-300 border-blue-800';

                  return (
                    <div
                      key={f.id}
                      className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2.5 transition-all hover:border-slate-700"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <span className={`px-2.5 py-0.5 text-[10px] font-mono font-bold rounded border ${badgeColor}`}>
                            {f.severity}
                          </span>
                          <span className="font-mono text-xs text-slate-400">{f.ruleId}</span>
                          <h3 className="font-bold text-sm text-white">{f.title}</h3>
                        </div>
                        <span className="text-xs font-mono text-slate-400">
                          {f.filePath}:{f.lineNumber}
                        </span>
                      </div>

                      <p className="text-xs text-slate-300 leading-relaxed">{f.description}</p>

                      {f.codeSnippet && (
                        <div className="bg-slate-900 border border-slate-800/80 rounded p-2 text-xs font-mono text-rose-300/90 overflow-x-auto">
                          {f.codeSnippet}
                        </div>
                      )}

                      <div className="bg-emerald-950/20 border border-emerald-900/40 rounded p-2.5 text-xs text-emerald-300/90 leading-relaxed">
                        <strong className="text-emerald-400">Remediation:</strong> {f.remediationRecommendation}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {activeTab === 'CONTRACTS' && (
          <div className="space-y-4">
            {contractMatrix?.verifications.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs">
                No contract verifications found. Click "Verify API Contracts" above to inspect discovered endpoints.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
                      <th className="py-2.5 px-3">Method</th>
                      <th className="py-2.5 px-3">Endpoint Path</th>
                      <th className="py-2.5 px-3">Expected Contract</th>
                      <th className="py-2.5 px-3">Actual Contract</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3">Verification Notes</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-mono">
                    {contractMatrix?.verifications.map((v) => (
                      <tr key={v.id} className="hover:bg-slate-950/50">
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              v.endpointMethod === 'GET'
                                ? 'bg-blue-950 text-blue-400 border border-blue-800'
                                : 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                            }`}
                          >
                            {v.endpointMethod}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-white font-semibold">{v.endpointPath}</td>
                        <td className="py-3 px-3 text-slate-400">{v.expectedContract}</td>
                        <td className="py-3 px-3 text-slate-400">{v.actualContract}</td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 text-[10px] rounded border ${
                              v.complianceStatus === 'COMPLIANT'
                                ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800'
                                : 'bg-rose-950/60 text-rose-400 border-rose-800'
                            }`}
                          >
                            {v.complianceStatus}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-slate-400 font-sans">{v.notes}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'SANDBOX' && (
          <div className="space-y-4">
            {sandboxResult ? (
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-4 p-4 bg-slate-950 border border-slate-800 rounded-xl">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs uppercase font-semibold text-slate-400">Execution Status:</span>
                      <span
                        className={`px-2 py-0.5 text-xs font-mono font-bold rounded ${
                          sandboxResult.status === 'PASSED'
                            ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                            : 'bg-rose-950 text-rose-400 border border-rose-800'
                        }`}
                      >
                        {sandboxResult.status}
                      </span>
                    </div>
                    <div className="text-xs text-slate-500 font-mono mt-1">
                      Execution ID: {sandboxResult.executionId} | Duration: {sandboxResult.durationMs}ms
                    </div>
                  </div>

                  <div className="flex items-center gap-4 text-xs font-mono">
                    <span className="text-emerald-400">{sandboxResult.testsPassed} Passed</span>
                    <span className="text-rose-400">{sandboxResult.testsFailed} Failed</span>
                    <span className="text-slate-400">{sandboxResult.testsRun} Total</span>
                  </div>
                </div>

                {/* Console Log Terminal */}
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 font-mono text-xs text-emerald-400/90 overflow-x-auto leading-relaxed shadow-inner">
                  <div className="flex items-center gap-1.5 text-slate-500 mb-3 border-b border-slate-800 pb-2">
                    <Terminal className="h-4 w-4" />
                    <span>Isolated Sandbox Runner Log</span>
                  </div>
                  <pre className="whitespace-pre-wrap">{sandboxResult.consoleOutput}</pre>
                </div>
              </div>
            ) : (
              <div className="p-12 text-center text-slate-500 text-xs">
                <Server className="h-10 w-10 text-slate-600 mx-auto mb-3" />
                <p>No sandbox tests executed yet.</p>
                <p className="mt-1">
                  Click "Run Sandboxed Tests" above to execute tests in an isolated, confined workspace.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
