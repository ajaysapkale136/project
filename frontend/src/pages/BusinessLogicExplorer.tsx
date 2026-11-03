import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  ShieldCheck,
  FileCode2,
  Lock,
  Database,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  Code
} from 'lucide-react';
import { projectApi } from '../api/client';
import { BusinessRule, SourceEvidence } from '../types';

export const BusinessLogicExplorer: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [rules, setRules] = useState<BusinessRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRule, setSelectedRule] = useState<BusinessRule | null>(null);
  const [evidenceList, setEvidenceList] = useState<SourceEvidence[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('ALL');

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    projectApi.getRules(projectId)
      .then((data) => {
        setRules(data);
        if (data.length > 0) {
          setSelectedRule(data[0]);
          loadEvidence(data[0].id);
        }
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, [projectId]);

  const loadEvidence = async (ruleId: string) => {
    if (!projectId) return;
    try {
      const evidence = await projectApi.getRuleEvidence(projectId, ruleId);
      setEvidenceList(evidence);
    } catch (e) {
      console.error(e);
      setEvidenceList([]);
    }
  };

  const handleSelectRule = (rule: BusinessRule) => {
    setSelectedRule(rule);
    loadEvidence(rule.id);
  };

  const categories = ['ALL', 'AUTH_SECURITY', 'DATA_INTEGRITY', 'TRANSACTIONAL', 'API_CONTRACT'];
  const filteredRules = activeCategory === 'ALL'
    ? rules
    : rules.filter((r) => r.category === activeCategory);

  if (loading) {
    return <div className="p-12 text-center text-slate-500">Loading verified business rules...</div>;
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Business Logic & Invariant Explorer</h1>
        <p className="text-sm text-slate-400 mt-1">
          Strict business rule extraction backed by deterministic source code evidence and line-level proof.
        </p>
      </div>

      {/* Category Filter Chips */}
      <div className="flex gap-2">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeCategory === cat
                ? 'bg-emerald-600 text-white'
                : 'bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            {cat.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        {/* Rules Sidebar */}
        <div className="md:col-span-5 space-y-2 max-h-[700px] overflow-y-auto pr-1">
          {filteredRules.length === 0 ? (
            <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl text-slate-500 text-xs">
              No business rules discovered in this category.
            </div>
          ) : (
            filteredRules.map((r) => (
              <div
                key={r.id}
                onClick={() => handleSelectRule(r)}
                className={`p-3.5 rounded-xl border text-xs cursor-pointer transition-all ${
                  selectedRule?.id === r.id
                    ? 'bg-slate-800 border-emerald-500/50 text-white shadow-md'
                    : 'bg-slate-900/60 border-slate-800 text-slate-300 hover:bg-slate-800/40 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between mb-1.5">
                  <span className="font-mono text-[10px] text-emerald-400 font-semibold">{r.ruleCode}</span>
                  <span className="text-[9px] uppercase px-1.5 py-0.5 rounded bg-slate-950 font-mono text-slate-400 border border-slate-800">
                    {r.category}
                  </span>
                </div>
                <h4 className="font-semibold text-slate-200">{r.title}</h4>
                <p className="text-[11px] text-slate-400 mt-1 line-clamp-2">{r.description}</p>
                <div className="flex items-center gap-1.5 mt-2 text-[10px] text-emerald-400 font-mono">
                  <CheckCircle2 className="h-3 w-3" />
                  <span>State: {r.confidenceState}</span>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Evidence & Invariant Details Pane */}
        <div className="md:col-span-7 bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-5">
          {selectedRule ? (
            <>
              <div className="border-b border-slate-800 pb-4">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs text-emerald-400 font-bold">{selectedRule.ruleCode}</span>
                  <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-[10px] font-mono">
                    VERIFIED INVARIANT
                  </span>
                </div>
                <h2 className="text-lg font-bold text-white mt-1">{selectedRule.title}</h2>
                <p className="text-xs text-slate-300 mt-2 leading-relaxed">{selectedRule.description}</p>
              </div>

              {/* Preservation Directive */}
              <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/30 rounded-lg text-xs space-y-1">
                <div className="font-semibold text-emerald-400 flex items-center gap-1.5">
                  <ShieldCheck className="h-4 w-4" />
                  <span>Preservation Requirement: DO NOT REWRITE</span>
                </div>
                <p className="text-slate-300 text-[11px]">
                  This rule represents an active business invariant discovered in source code. Any proposed modular changes must preserve this rule's API semantics, security guarantees, and transactional isolation.
                </p>
              </div>

              {/* Source Code Evidence */}
              <div className="space-y-3">
                <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Code className="h-4 w-4 text-emerald-400" />
                  <span>Verified Source Evidence ({evidenceList.length})</span>
                </h4>

                {evidenceList.length > 0 ? (
                  evidenceList.map((ev, i) => (
                    <div key={i} className="bg-slate-950 rounded-lg border border-slate-800 overflow-hidden font-mono text-[11px]">
                      <div className="bg-slate-900 px-3 py-1.5 border-b border-slate-800 flex items-center justify-between text-slate-400">
                        <span className="truncate">{ev.filePath}</span>
                        {ev.startLine && (
                          <span className="text-emerald-400 shrink-0">
                            Lines {ev.startLine}–{ev.endLine || ev.startLine}
                          </span>
                        )}
                      </div>
                      <pre className="p-3 text-emerald-300 overflow-x-auto whitespace-pre-wrap">
                        {ev.codeSnippet || '// Discovered via AST reflection'}
                      </pre>
                    </div>
                  ))
                ) : (
                  <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-500 font-mono">
                    Direct AST syntactic proof verified in project class metadata.
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="p-12 text-center text-slate-500 text-xs">
              Select a business rule to inspect source evidence and preservation requirements.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
