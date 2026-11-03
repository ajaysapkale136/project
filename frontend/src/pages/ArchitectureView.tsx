import React, { useState, useEffect } from 'react';
import { useParams, NavLink } from 'react-router-dom';
import ReactFlow, {
  Background,
  Controls,
  Node,
  Edge,
  MarkerType
} from 'reactflow';
import 'reactflow/dist/style.css';
import {
  Compass,
  Boxes,
  AlertTriangle,
  Lightbulb,
  GitBranch,
  ShieldAlert,
  CheckCircle2
} from 'lucide-react';
import { projectApi } from '../api/client';
import { ArchitectureGraph } from '../types';

interface Props {
  mode: 'EXISTING' | 'PROPOSED';
}

export const ArchitectureView: React.FC<Props> = ({ mode }) => {
  const { projectId } = useParams<{ projectId: string }>();
  const [graphData, setGraphData] = useState<ArchitectureGraph | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    const fetcher = mode === 'EXISTING'
      ? projectApi.getExistingArchitecture(projectId)
      : projectApi.getProposedArchitecture(projectId);

    fetcher
      .then((data) => {
        setGraphData(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [projectId, mode]);

  if (loading) {
    return <div className="p-12 text-center text-slate-500">Synthesizing architecture graph...</div>;
  }

  if (!graphData) {
    return <div className="p-12 text-center text-slate-500">No architecture synthesized yet.</div>;
  }

  // Convert to React Flow nodes and edges
  const flowNodes: Node[] = graphData.nodes.map((n) => ({
    id: n.nodeKey,
    position: { x: n.posX, y: n.posY },
    data: {
      label: (
        <div className="p-2.5 rounded-lg bg-slate-900 border border-slate-700 shadow-lg text-left text-xs min-w-[200px]">
          <div className="flex items-center justify-between mb-1">
            <span className={`text-[9px] uppercase font-mono px-1.5 py-0.5 rounded font-bold ${
              n.category === 'FRONTEND' ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' :
              n.category === 'INFRASTRUCTURE' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' :
              n.category === 'BACKEND' ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30' :
              'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
            }`}>
              {n.nodeType}
            </span>
            <span className="text-[10px] text-slate-500 font-mono">{n.category}</span>
          </div>
          <div className="font-semibold text-white mt-1">{n.label}</div>
        </div>
      )
    },
    style: { background: 'transparent', border: 'none' }
  }));

  const flowEdges: Edge[] = graphData.edges.map((e, idx) => ({
    id: `e-${idx}`,
    source: e.sourceNodeKey,
    target: e.targetNodeKey,
    label: e.label,
    labelStyle: { fill: '#94a3b8', fontSize: 10, fontFamily: 'monospace' },
    labelBgStyle: { fill: '#0f172a' },
    style: { stroke: mode === 'EXISTING' ? '#38bdf8' : '#10b981', strokeWidth: 2 },
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: mode === 'EXISTING' ? '#38bdf8' : '#10b981'
    }
  }));

  const antiPatterns = graphData.antiPatternsJson ? JSON.parse(graphData.antiPatternsJson) : [];
  const recommendations = graphData.recommendationsJson ? JSON.parse(graphData.recommendationsJson) : [];

  return (
    <div className="space-y-6 max-w-7xl mx-auto flex flex-col h-[calc(100vh-6rem)]">
      {/* Header and Toggle */}
      <div className="flex items-center justify-between shrink-0">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-white tracking-tight">
              {mode === 'EXISTING' ? 'Existing Architecture Topology' : 'Proposed Modular Target Architecture'}
            </h1>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
              mode === 'EXISTING' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
            }`}>
              {mode} VIEW
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl">{graphData.summary}</p>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
            <NavLink
              to={`/projects/${projectId}/architecture/existing`}
              className={`px-3 py-1.5 rounded-md font-medium transition-all ${
                mode === 'EXISTING' ? 'bg-slate-800 text-sky-400 shadow-sm' : 'text-slate-400 hover:text-white'
              }`}
            >
              Existing Architecture
            </NavLink>
            <NavLink
              to={`/projects/${projectId}/architecture/proposed`}
              className={`px-3 py-1.5 rounded-md font-medium transition-all ${
                mode === 'PROPOSED' ? 'bg-slate-800 text-emerald-400 shadow-sm' : 'text-slate-400 hover:text-white'
              }`}
            >
              Proposed Architecture
            </NavLink>
          </div>

          <NavLink
            to={`/projects/${projectId}/architecture/compare`}
            className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
          >
            <GitBranch className="h-3.5 w-3.5 text-emerald-400" />
            <span>Compare Views</span>
          </NavLink>
        </div>
      </div>

      {/* Main Graph Canvas */}
      <div className="flex-1 bg-slate-950 border border-slate-800 rounded-xl overflow-hidden relative shadow-2xl">
        <ReactFlow
          nodes={flowNodes}
          edges={flowEdges}
          fitView
          className="bg-slate-950"
        >
          <Background color="#1e293b" gap={20} size={1} />
          <Controls className="bg-slate-900 border-slate-800 fill-slate-300" />
        </ReactFlow>
      </div>

      {/* Bottom Insights Drawer: Anti-patterns & Recommendations */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 shrink-0 max-h-48 overflow-y-auto">
        {mode === 'EXISTING' ? (
          <>
            <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl space-y-2">
              <h4 className="text-xs font-semibold text-rose-400 uppercase tracking-wider flex items-center gap-1.5">
                <AlertTriangle className="h-4 w-4" />
                <span>Identified Deficiencies & Anti-Patterns ({antiPatterns.length})</span>
              </h4>
              <div className="space-y-1.5">
                {antiPatterns.map((ap: any, i: number) => (
                  <div key={i} className="text-xs bg-slate-950/70 p-2 rounded border border-rose-500/20 text-slate-300">
                    <span className="font-mono text-[10px] text-rose-400 font-bold block">{ap.type}</span>
                    <span className="text-[11px]">{ap.description}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-4 bg-slate-900/80 border border-slate-800 rounded-xl space-y-2">
              <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
                <Lightbulb className="h-4 w-4" />
                <span>Target Remediation Strategy</span>
              </h4>
              <div className="space-y-1.5">
                {recommendations.map((rec: any, i: number) => (
                  <div key={i} className="text-xs bg-slate-950/70 p-2 rounded border border-emerald-500/20 text-slate-300">
                    <span className="font-mono text-[10px] text-emerald-400 font-bold block">{rec.action}</span>
                    <span className="text-[11px]">{rec.benefit}</span>
                  </div>
                ))}
              </div>
            </div>
          </>
        ) : (
          <div className="md:col-span-2 p-4 bg-slate-900/80 border border-slate-800 rounded-xl space-y-2">
            <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
              <CheckCircle2 className="h-4 w-4" />
              <span>Proposed Modular Monolith Enhancements</span>
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {recommendations.map((enh: any, i: number) => (
                <div key={i} className="text-xs bg-slate-950/70 p-2.5 rounded border border-emerald-500/20 text-slate-300">
                  <span className="font-semibold text-emerald-400 block text-xs">{enh.enhancement}</span>
                  <span className="text-[11px] text-slate-400 mt-1 block">{enh.reason}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
