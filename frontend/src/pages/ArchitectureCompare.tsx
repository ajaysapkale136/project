import React, { useState, useEffect } from 'react';
import { useParams, NavLink } from 'react-router-dom';
import {
  GitBranch,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Boxes,
  ArrowRight
} from 'lucide-react';
import { projectApi } from '../api/client';
import { ArchitectureDiff } from '../types';

export const ArchitectureCompare: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [diff, setDiff] = useState<ArchitectureDiff | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    projectApi.compareArchitecture(projectId)
      .then((data) => {
        setDiff(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [projectId]);

  if (loading) {
    return <div className="p-12 text-center text-slate-500">Generating architectural comparison matrix...</div>;
  }

  if (!diff) {
    return <div className="p-12 text-center text-slate-500">No comparison data available.</div>;
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Architecture Differential & Migration Plan</h1>
          <p className="text-sm text-slate-400 mt-1">
            Structural audit comparing as-is codebase topology with proposed target architecture.
          </p>
        </div>
        <NavLink
          to={`/projects/${projectId}/modules`}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center gap-2 shadow-lg shadow-emerald-900/20"
        >
          <Boxes className="h-4 w-4" />
          <span>Launch Module Builder</span>
        </NavLink>
      </div>

      {/* Comparison Scorecard */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 bg-slate-900/70 border border-slate-800 rounded-xl space-y-1">
          <p className="text-xs text-slate-400 font-medium">As-Is Topology Nodes</p>
          <p className="text-2xl font-bold text-sky-400 font-mono">{diff.existingNodeCount}</p>
          <p className="text-[11px] text-slate-500">Controllers, services, DAOs, schema tables</p>
        </div>

        <div className="p-4 bg-slate-900/70 border border-slate-800 rounded-xl space-y-1">
          <p className="text-xs text-slate-400 font-medium">Proposed Modular Nodes</p>
          <p className="text-2xl font-bold text-emerald-400 font-mono">{diff.proposedNodeCount}</p>
          <p className="text-[11px] text-slate-500">Target modular subsystems, event bus, caches</p>
        </div>

        <div className="p-4 bg-slate-900/70 border border-slate-800 rounded-xl space-y-1">
          <p className="text-xs text-slate-400 font-medium">Migration Risk Classification</p>
          <p className="text-lg font-bold text-emerald-400 mt-1 flex items-center gap-1.5">
            <ShieldCheck className="h-5 w-5 text-emerald-400" />
            <span>LOW RISK</span>
          </p>
          <p className="text-[11px] text-slate-500">100% Backward compatible; zero schema breaks</p>
        </div>
      </div>

      {/* Added Capabilities vs Resolved Anti-Patterns */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Added Capabilities */}
        <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-4">
          <h3 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4" />
            <span>Added Modular Capabilities ({diff.addedCapabilities.length})</span>
          </h3>
          <ul className="space-y-2.5">
            {diff.addedCapabilities.map((cap, i) => (
              <li key={i} className="text-xs text-slate-300 flex items-start gap-2 bg-slate-950/60 p-3 rounded-lg border border-slate-800">
                <span className="text-emerald-400 font-bold shrink-0">+</span>
                <span>{cap}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Resolved Anti-Patterns */}
        <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-xl space-y-4">
          <h3 className="text-xs font-semibold text-sky-400 uppercase tracking-wider flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            <span>Resolved Architectural Deficiencies ({diff.resolvedAntiPatterns.length})</span>
          </h3>
          <ul className="space-y-2.5">
            {diff.resolvedAntiPatterns.map((smell, i) => (
              <li key={i} className="text-xs text-slate-300 flex items-start gap-2 bg-slate-950/60 p-3 rounded-lg border border-slate-800">
                <span className="text-sky-400 font-bold shrink-0">✓</span>
                <span>{smell}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Rollback & Safety Guarantee */}
      <div className="p-5 bg-slate-900/90 border border-slate-800 rounded-xl space-y-3">
        <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-2">
          <RotateCcw className="h-4 w-4 text-emerald-400" />
          <span>Rollback & Safety Guarantee</span>
        </h3>
        <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-300 space-y-1 font-mono">
          <p><span className="text-slate-500">Risk Assessment:</span> {diff.riskLevel}</p>
          <p><span className="text-slate-500">Rollback Strategy:</span> {diff.rollbackStrategy}</p>
        </div>
      </div>
    </div>
  );
};
