import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Cpu,
  ShieldCheck,
  Zap,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Coins,
  Loader2,
  Clock,
  ArrowRight
} from 'lucide-react';
import { projectApi } from '../api/client';
import { ProviderHealth, MultiModelReviewResponse } from '../types';

export const MultiAiSettings: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  const [providers, setProviders] = useState<ProviderHealth[]>([]);
  const [loading, setLoading] = useState(true);

  // Mode 3: Multi-Model Review State
  const [reviewRunning, setReviewRunning] = useState(false);
  const [reviewResult, setReviewResult] = useState<MultiModelReviewResponse | null>(null);

  useEffect(() => {
    projectApi.getAiProviders()
      .then((data) => {
        setProviders(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const handleRunConsensusReview = async () => {
    if (!projectId) {
      alert('Please select an active project first.');
      return;
    }
    try {
      setReviewRunning(true);
      const res = await projectApi.triggerMultiModelReview(projectId, {
        primaryProvider: 'GEMINI',
        reviewerProviders: ['OPENAI', 'CLAUDE']
      });
      setReviewResult(res);
      setReviewRunning(false);
    } catch (e: any) {
      setReviewRunning(false);
      alert('Review failed: ' + (e.message || 'Error'));
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Multi-AI Intelligence & Consensus Engine</h1>
        <p className="text-sm text-slate-400 mt-1">
          Independent integrations with OpenAI GPT, Google Gemini, and Anthropic Claude with strict multi-model review.
        </p>
      </div>

      {/* Provider Health & Status Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {providers.map((p) => (
          <div
            key={p.providerType}
            className="p-5 bg-slate-900/70 border border-slate-800 rounded-xl space-y-3 relative overflow-hidden"
          >
            <div className="flex items-center justify-between">
              <span className="font-bold text-base text-white">{p.providerType}</span>
              <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                p.configured
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                  : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
              }`}>
                {p.configured ? 'LIVE CREDENTIAL' : 'DETERMINISTIC FALLBACK'}
              </span>
            </div>

            <div className="space-y-1 text-xs">
              <div className="text-slate-400">
                Default Model: <span className="font-mono text-white font-semibold">{p.defaultModel}</span>
              </div>
              <div className="text-slate-400">
                Latency: <span className="font-mono text-emerald-400">{p.latencyMs}ms</span>
              </div>
              <div className="text-[11px] text-slate-500 pt-1">
                Cost: ${p.costPer1kInput.toFixed(4)} / 1k in • ${p.costPer1kOutput.toFixed(4)} / 1k out
              </div>
            </div>

            <p className="text-[11px] text-slate-400 bg-slate-950 p-2 rounded border border-slate-800">
              {p.statusMessage}
            </p>
          </div>
        ))}
      </div>

      {/* Mode 3: Multi-Model Review & Consensus Synthesis */}
      <div className="p-6 bg-slate-900/80 border border-slate-800 rounded-xl space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-emerald-400" />
              <span>Multi-Model Review & Consensus Synthesis (Mode 3)</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              One model generates an architecture proposal. Other configured models independently review it. Conflicts and unsupported claims are explicitly surfaced.
            </p>
          </div>

          <button
            type="button"
            onClick={handleRunConsensusReview}
            disabled={reviewRunning}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center gap-2 shadow-lg shadow-emerald-900/20"
          >
            {reviewRunning ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Running 3-Model Consensus...</span>
              </>
            ) : (
              <>
                <ShieldCheck className="h-4 w-4" />
                <span>Trigger Multi-Model Review</span>
              </>
            )}
          </button>
        </div>

        {/* Review Results */}
        {reviewResult && (
          <div className="space-y-4 pt-4 border-t border-slate-800">
            <div className="flex items-center justify-between bg-slate-950 p-3 rounded-lg border border-slate-800 text-xs font-mono">
              <div>
                <span className="text-slate-500">Primary Generator:</span>{' '}
                <span className="text-emerald-400 font-bold">{reviewResult.primaryModel}</span>
              </div>
              <div>
                <span className="text-slate-500">Reviewers:</span>{' '}
                <span className="text-sky-400">{reviewResult.reviewerModels.join(', ')}</span>
              </div>
              <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                reviewResult.consensusStatus === 'CONSENSUS_REACHED'
                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                  : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
              }`}>
                {reviewResult.consensusStatus}
              </span>
            </div>

            <p className="text-xs text-slate-300 italic bg-slate-950/60 p-3 rounded-lg border border-slate-800">
              "{reviewResult.synthesisSummary}"
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Agreements */}
              <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
                  <CheckCircle2 className="h-4 w-4" />
                  <span>Consensus Agreements ({reviewResult.agreements.length})</span>
                </h4>
                <ul className="space-y-1.5 text-xs text-slate-300">
                  {reviewResult.agreements.map((ag, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="text-emerald-400 font-bold">✓</span>
                      <span>{ag}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Disagreements */}
              <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
                  <AlertTriangle className="h-4 w-4" />
                  <span>Flagged Model Disagreements ({reviewResult.disagreements.length})</span>
                </h4>
                <ul className="space-y-1.5 text-xs text-slate-300">
                  {reviewResult.disagreements.map((dis, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="text-amber-400 font-bold">!</span>
                      <span>{dis}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Unsupported Claims & Unresolved Risks */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-rose-400 uppercase tracking-wider">
                  Rejected Unsupported Claims (Missing AST Evidence)
                </h4>
                <ul className="space-y-1.5 text-xs text-slate-400">
                  {reviewResult.unsupportedClaims.map((claim, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="text-rose-400 font-bold">✗</span>
                      <span>{claim}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="p-4 bg-slate-950 rounded-lg border border-slate-800 space-y-2">
                <h4 className="text-xs font-semibold text-purple-400 uppercase tracking-wider">
                  Unresolved Technical Risks
                </h4>
                <ul className="space-y-1.5 text-xs text-slate-300">
                  {reviewResult.unresolvedRisks.map((risk, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <span className="text-purple-400 font-bold">•</span>
                      <span>{risk}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
