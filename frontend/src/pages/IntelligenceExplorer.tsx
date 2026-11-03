import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Cpu,
  Layers,
  ShieldCheck,
  Code2,
  Database,
  CheckCircle,
  Play,
  Loader2,
  ChevronDown,
  ChevronRight,
  ExternalLink,
  FileText
} from 'lucide-react';
import { projectApi, subscribeJobProgress } from '../api/client';
import { IntelligenceOverview, ModuleSummary, AnalysisComponent } from '../types';

export const IntelligenceExplorer: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [data, setData] = useState<IntelligenceOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [activeJobProgress, setActiveJobProgress] = useState<number | null>(null);
  const [jobStep, setJobStep] = useState<string>('');
  const [selectedComponent, setSelectedComponent] = useState<AnalysisComponent | null>(null);
  const [selectedModuleId, setSelectedModuleId] = useState<string>('');

  const loadData = () => {
    if (!projectId) return;
    setLoading(true);
    projectApi.getIntelligence(projectId)
      .then((res) => {
        setData(res);
        if (res.modules && res.modules.length > 0) {
          setSelectedModuleId(res.modules[0].id);
        }
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };

  useEffect(() => {
    loadData();
  }, [projectId]);

  const handleRunAnalysis = async () => {
    if (!projectId) return;
    try {
      setAnalyzing(true);
      const job = await projectApi.triggerAnalysis(projectId);
      setActiveJobProgress(10);
      setJobStep('Starting AST Code Intelligence parser...');

      subscribeJobProgress(
        job.id,
        (evt) => {
          setActiveJobProgress(evt.progressPercent);
          setJobStep(evt.currentStep);
          if (evt.status === 'COMPLETED') {
            setAnalyzing(false);
            setActiveJobProgress(null);
            loadData();
          } else if (evt.status === 'FAILED') {
            setAnalyzing(false);
            alert('Analysis failed: ' + evt.errorMessage);
          }
        },
        (err) => console.error(err)
      );
    } catch (err: any) {
      setAnalyzing(false);
      alert('Could not trigger analysis: ' + (err.message || 'Error'));
    }
  };

  if (loading) {
    return <div className="p-12 text-center text-slate-500">Loading code intelligence model...</div>;
  }

  const isAnalyzed = data && data.status === 'COMPLETED';

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Code Intelligence & AST Engine</h1>
          <p className="text-sm text-slate-400 mt-1">
            Deterministic AST parsing, endpoint extraction, Spring/React dependency graph, and business rule discovery.
          </p>
        </div>

        <button
          onClick={handleRunAnalysis}
          disabled={analyzing}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center gap-2 shadow-lg shadow-emerald-900/20"
        >
          {analyzing ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Analyzing ({activeJobProgress}%)</span>
            </>
          ) : (
            <>
              <Play className="h-4 w-4 fill-white" />
              <span>{isAnalyzed ? 'Re-run Deep Analysis' : 'Run Deep Analysis'}</span>
            </>
          )}
        </button>
      </div>

      {/* Progress banner if analyzing */}
      {analyzing && (
        <div className="p-4 bg-slate-900 border border-slate-800 rounded-xl space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-300 font-mono">{jobStep}</span>
            <span className="text-emerald-400 font-bold">{activeJobProgress}%</span>
          </div>
          <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden">
            <div
              className="bg-emerald-500 h-2 transition-all duration-300"
              style={{ width: `${activeJobProgress}%` }}
            ></div>
          </div>
        </div>
      )}

      {/* Metrics Row */}
      {data && (
        <div className="grid grid-cols-2 md:grid-cols-6 gap-3">
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">Total Files</p>
            <p className="text-lg font-bold text-white mt-0.5">{data.totalFiles || 0}</p>
          </div>
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">Lines of Code</p>
            <p className="text-lg font-bold text-white mt-0.5">{(data.totalLinesOfCode || 0).toLocaleString()}</p>
          </div>
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">Controllers</p>
            <p className="text-lg font-bold text-blue-400 mt-0.5">{data.totalControllers || 0}</p>
          </div>
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">API Endpoints</p>
            <p className="text-lg font-bold text-emerald-400 mt-0.5">{data.totalEndpoints || 0}</p>
          </div>
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">Domain Services</p>
            <p className="text-lg font-bold text-purple-400 mt-0.5">{data.totalServices || 0}</p>
          </div>
          <div className="p-3 bg-slate-900/70 border border-slate-800 rounded-lg">
            <p className="text-[11px] text-slate-400">Entities / Tables</p>
            <p className="text-lg font-bold text-amber-400 mt-0.5">{data.totalEntities || 0}</p>
          </div>
        </div>
      )}

      {/* Detected Tech Stack */}
      <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-3">
        <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
          Detected Technology Stack (Strict Provenance)
        </h3>
        <div className="flex flex-wrap gap-2">
          {data?.technologies && data.technologies.length > 0 ? (
            data.technologies.map((t) => (
              <div
                key={t.id}
                className="flex items-center gap-2 px-3 py-1.5 bg-slate-800/90 border border-slate-700 rounded-lg text-xs"
              >
                <span className="font-semibold text-white">{t.name}</span>
                {t.version && <span className="font-mono text-[10px] text-slate-400">v{t.version}</span>}
                <span className="text-[9px] uppercase px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-mono">
                  {t.confidenceState}
                </span>
              </div>
            ))
          ) : (
            <p className="text-xs text-slate-500">Run analysis to detect technologies.</p>
          )}
        </div>
      </div>

      {/* Modules & Components Explorer */}
      <div className="space-y-4">
        <h3 className="text-sm font-semibold text-white">Architectural Modules & Discovered Components</h3>

        {data?.modules && data.modules.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {/* Module List Sidebar */}
            <div className="space-y-2 md:col-span-1">
              {data.modules.map((mod) => (
                <button
                  key={mod.id}
                  onClick={() => setSelectedModuleId(mod.id)}
                  className={`w-full text-left p-3 rounded-lg border text-xs transition-all ${
                    selectedModuleId === mod.id
                      ? 'bg-slate-800 border-emerald-500/50 text-white shadow-sm'
                      : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`}
                >
                  <div className="font-semibold text-slate-200">{mod.name}</div>
                  <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between">
                    <span className="font-mono text-[10px] text-emerald-400">{mod.moduleType}</span>
                    <span>{mod.componentCount} components</span>
                  </div>
                </button>
              ))}
            </div>

            {/* Components Table */}
            <div className="md:col-span-3 bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden">
              {(() => {
                const currentModule = data.modules.find((m) => m.id === selectedModuleId) || data.modules[0];
                return (
                  <div>
                    <div className="p-3 bg-slate-850 border-b border-slate-800 flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-200">
                        {currentModule.name} — Components ({currentModule.components.length})
                      </span>
                      <span className="text-[11px] text-slate-400 font-mono">{currentModule.basePath}</span>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-slate-950/50 border-b border-slate-800 text-[11px] text-slate-400 uppercase font-mono">
                          <tr>
                            <th className="p-3">Component</th>
                            <th className="p-3">Role</th>
                            <th className="p-3">File Path</th>
                            <th className="p-3">Endpoints</th>
                            <th className="p-3">State</th>
                            <th className="p-3 text-right">Action</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60">
                          {currentModule.components.map((comp) => (
                            <tr key={comp.id} className="hover:bg-slate-800/30 transition-all">
                              <td className="p-3 font-semibold text-white">{comp.name}</td>
                              <td className="p-3">
                                <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                                  comp.componentType === 'CONTROLLER' ? 'bg-blue-500/10 text-blue-400 border-blue-500/30' :
                                  comp.componentType === 'SERVICE' ? 'bg-purple-500/10 text-purple-400 border-purple-500/30' :
                                  comp.componentType === 'REPOSITORY' ? 'bg-amber-500/10 text-amber-400 border-amber-500/30' :
                                  'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                                }`}>
                                  {comp.componentType}
                                </span>
                              </td>
                              <td className="p-3 font-mono text-[11px] text-slate-400 truncate max-w-xs">{comp.filePath}</td>
                              <td className="p-3 font-mono text-slate-300">{comp.endpointCount}</td>
                              <td className="p-3">
                                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">
                                  {comp.confidenceState}
                                </span>
                              </td>
                              <td className="p-3 text-right">
                                <button
                                  onClick={() => setSelectedComponent(comp)}
                                  className="text-[11px] text-emerald-400 hover:text-emerald-300 underline"
                                >
                                  Details
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                );
              })()}
            </div>
          </div>
        ) : (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl text-slate-500 text-xs">
            No modules discovered yet. Click "Run Deep Analysis" to extract AST components.
          </div>
        )}
      </div>

      {/* Component Details Modal */}
      {selectedComponent && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="text-base font-bold text-white">{selectedComponent.name}</h3>
                <p className="text-xs text-slate-400 font-mono mt-0.5">{selectedComponent.filePath}</p>
              </div>
              <button
                onClick={() => setSelectedComponent(null)}
                className="text-slate-400 hover:text-white text-sm"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <span className="text-slate-400">Component Role:</span>{' '}
                <span className="font-semibold text-emerald-400 font-mono">{selectedComponent.componentType}</span>
              </div>

              {selectedComponent.detailsJson && (
                <div>
                  <span className="text-slate-400 block mb-1">AST Discovered Schema & Signatures:</span>
                  <pre className="p-3 bg-slate-950 rounded-lg text-emerald-300 font-mono text-[11px] overflow-x-auto max-h-60 border border-slate-800">
                    {JSON.stringify(JSON.parse(selectedComponent.detailsJson), null, 2)}
                  </pre>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedComponent(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
