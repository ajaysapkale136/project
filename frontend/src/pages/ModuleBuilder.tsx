import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Boxes,
  Copy,
  Check,
  FolderTree,
  ShieldCheck,
  CheckCircle2,
  FileCode,
  Terminal,
  Send
} from 'lucide-react';
import { projectApi } from '../api/client';
import { ModuleBlueprint } from '../types';

export const ModuleBuilder: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [moduleName, setModuleName] = useState('PaymentProcessingService');
  const [blueprint, setBlueprint] = useState<ModuleBlueprint | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  const fetchBlueprint = (name: string) => {
    if (!projectId) return;
    setLoading(true);
    projectApi.getModuleBlueprint(projectId, name)
      .then((data) => {
        setBlueprint(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchBlueprint(moduleName);
  }, [projectId]);

  const handleGenerate = (e: React.FormEvent) => {
    e.preventDefault();
    if (moduleName.trim()) {
      fetchBlueprint(moduleName.trim());
    }
  };

  const handleCopy = () => {
    if (blueprint?.antigravityPrompt) {
      navigator.clipboard.writeText(blueprint.antigravityPrompt);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">AI Module Architect & Prompt Generator</h1>
        <p className="text-sm text-slate-400 mt-1">
          Synthesize full architectural blueprints and generate verified Antigravity agent prompts for new modules.
        </p>
      </div>

      {/* Module Selector Bar */}
      <form onSubmit={handleGenerate} className="flex gap-3 bg-slate-900/70 p-4 border border-slate-800 rounded-xl">
        <div className="flex-1">
          <label className="block text-xs font-medium text-slate-400 mb-1">Target Module Name</label>
          <input
            type="text"
            value={moduleName}
            onChange={(e) => setModuleName(e.target.value)}
            placeholder="e.g. AuditAndComplianceService"
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500 font-semibold"
          />
        </div>
        <div className="flex items-end">
          <button
            type="submit"
            disabled={loading}
            className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center gap-2 shadow-lg shadow-emerald-900/20"
          >
            <Boxes className="h-4 w-4" />
            <span>Generate Blueprint</span>
          </button>
        </div>
      </form>

      {loading ? (
        <div className="p-12 text-center text-slate-500">Generating module architecture blueprint...</div>
      ) : blueprint ? (
        <div className="space-y-6">
          {/* Module Header Card */}
          <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono text-emerald-400 font-bold uppercase">MODULE BLUEPRINT SPECIFICATION</span>
              <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 text-[10px] font-mono border border-emerald-500/30">
                CLEAN ARCHITECTURE
              </span>
            </div>
            <h2 className="text-xl font-bold text-white">{blueprint.moduleName}</h2>
            <p className="text-xs text-slate-300 leading-relaxed">{blueprint.purpose}</p>
          </div>

          {/* Requirements & Non-Functional Spec */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-3">
              <h3 className="text-xs font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                <span>Business & Functional Invariants</span>
              </h3>
              <ul className="space-y-2 text-xs text-slate-300">
                {blueprint.businessRequirements.map((r, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-emerald-400 font-bold">•</span>
                    <span>{r}</span>
                  </li>
                ))}
                {blueprint.functionalRequirements.map((r, i) => (
                  <li key={`f-${i}`} className="flex items-start gap-2">
                    <span className="text-sky-400 font-bold">•</span>
                    <span>{r}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-3">
              <h3 className="text-xs font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
                <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                <span>Quality & Acceptance Criteria</span>
              </h3>
              <ul className="space-y-2 text-xs text-slate-300">
                {blueprint.nonFunctionalRequirements.map((r, i) => (
                  <li key={i} className="flex items-start gap-2">
                    <span className="text-purple-400 font-bold">•</span>
                    <span>{r}</span>
                  </li>
                ))}
                {blueprint.acceptanceCriteria.map((r, i) => (
                  <li key={`a-${i}`} className="flex items-start gap-2">
                    <span className="text-emerald-400 font-bold">•</span>
                    <span>{r}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Folder Structure & Sequence */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-3">
              <h3 className="text-xs font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
                <FolderTree className="h-4 w-4 text-emerald-400" />
                <span>Proposed Package & Folder Structure</span>
              </h3>
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 font-mono text-[11px] text-slate-300 space-y-1">
                {blueprint.folderStructure.map((path, i) => (
                  <div key={i} className="text-emerald-300">📁 {path}</div>
                ))}
              </div>
            </div>

            <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-3">
              <h3 className="text-xs font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
                <FileCode className="h-4 w-4 text-emerald-400" />
                <span>Implementation Execution Sequence</span>
              </h3>
              <div className="space-y-2 text-xs text-slate-300">
                {blueprint.implementationSequence.map((step, i) => (
                  <div key={i} className="p-2 bg-slate-950/80 rounded border border-slate-800 font-mono text-[11px] text-slate-300">
                    {step}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Antigravity Implementation Prompt Box */}
          <div className="p-5 bg-slate-900/90 border border-emerald-500/30 rounded-xl space-y-3 shadow-xl">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Terminal className="h-4 w-4 text-emerald-400" />
                <h3 className="text-xs font-bold text-white uppercase tracking-wider">
                  Antigravity Agent Implementation Prompt
                </h3>
              </div>

              <button
                type="button"
                onClick={handleCopy}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs font-medium flex items-center gap-1.5 border border-slate-700"
              >
                {copied ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-400" />
                    <span className="text-emerald-400 font-semibold">Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-3.5 w-3.5" />
                    <span>Copy Implementation Prompt</span>
                  </>
                )}
              </button>
            </div>

            <pre className="p-4 bg-slate-950 rounded-lg border border-slate-800 font-mono text-xs text-emerald-300 whitespace-pre-wrap max-h-72 overflow-y-auto">
              {blueprint.antigravityPrompt}
            </pre>
          </div>
        </div>
      ) : null}
    </div>
  );
};
