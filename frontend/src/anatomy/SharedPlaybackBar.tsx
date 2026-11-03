import React from 'react';
import {
  Play,
  Pause,
  RotateCcw,
  SkipBack,
  SkipForward,
  Gauge,
  Sliders,
  Subtitles,
  Maximize2
} from 'lucide-react';
import { AnimationStep } from '../types';

interface Props {
  isPlaying: boolean;
  onTogglePlay: () => void;
  onRestart: () => void;
  onPrevStep: () => void;
  onNextStep: () => void;
  currentStepIndex: number;
  totalSteps: number;
  onSelectStep: (idx: number) => void;
  steps: AnimationStep[];
  playbackSpeed: number;
  onChangeSpeed: (spd: number) => void;
  explodedOffset: number;
  onChangeExplodedOffset: (val: number) => void;
  subtitlesEnabled: boolean;
  onToggleSubtitles: () => void;
  showExplodedSlider?: boolean;
}

export const SharedPlaybackBar: React.FC<Props> = ({
  isPlaying,
  onTogglePlay,
  onRestart,
  onPrevStep,
  onNextStep,
  currentStepIndex,
  totalSteps,
  onSelectStep,
  steps,
  playbackSpeed,
  onChangeSpeed,
  explodedOffset,
  onChangeExplodedOffset,
  subtitlesEnabled,
  onToggleSubtitles,
  showExplodedSlider = false
}) => {
  const currentStep = steps[currentStepIndex];

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3 shadow-xl">
      {/* Top Scrubber & Timeline Progress */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <span className="font-mono text-emerald-400 font-bold">
              STEP {currentStepIndex + 1} OF {totalSteps}
            </span>
            <span className="text-slate-300 font-semibold">{currentStep?.title || 'Execution Step'}</span>
          </div>
          <span className="text-[11px] font-mono text-slate-400">
            {currentStep?.durationMs ? `${currentStep.durationMs}ms` : ''} • Speed {playbackSpeed}x
          </span>
        </div>

        {/* Step Scrubber Bar */}
        <div className="grid grid-cols-8 gap-1.5 h-2 w-full bg-slate-950 p-0.5 rounded-full border border-slate-800">
          {Array.from({ length: totalSteps }).map((_, i) => (
            <button
              key={i}
              type="button"
              onClick={() => onSelectStep(i)}
              className={`h-full rounded-full transition-all ${
                i === currentStepIndex
                  ? 'bg-emerald-400 shadow-sm shadow-emerald-500'
                  : i < currentStepIndex
                  ? 'bg-emerald-600/60'
                  : 'bg-slate-800 hover:bg-slate-700'
              }`}
              title={`Step ${i + 1}: ${steps[i]?.title}`}
            />
          ))}
        </div>
      </div>

      {/* Main Controls Row */}
      <div className="flex flex-wrap items-center justify-between gap-4 pt-1">
        {/* Left: Transport Controls */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onRestart}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-all"
            title="Restart Timeline"
          >
            <RotateCcw className="h-4 w-4" />
          </button>

          <button
            type="button"
            onClick={onPrevStep}
            disabled={currentStepIndex <= 0}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-300 hover:text-white transition-all"
            title="Previous Step"
          >
            <SkipBack className="h-4 w-4" />
          </button>

          <button
            type="button"
            onClick={onTogglePlay}
            className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center gap-1.5 shadow-md shadow-emerald-900/30 transition-all"
          >
            {isPlaying ? (
              <>
                <Pause className="h-4 w-4 fill-white" />
                <span>Pause</span>
              </>
            ) : (
              <>
                <Play className="h-4 w-4 fill-white" />
                <span>Play Animation</span>
              </>
            )}
          </button>

          <button
            type="button"
            onClick={onNextStep}
            disabled={currentStepIndex >= totalSteps - 1}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-slate-300 hover:text-white transition-all"
            title="Next Step"
          >
            <SkipForward className="h-4 w-4" />
          </button>
        </div>

        {/* Center: Exploded Separation Slider (if in Format 2 or enabled) */}
        {showExplodedSlider && (
          <div className="flex items-center gap-2.5 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 text-xs">
            <Sliders className="h-4 w-4 text-emerald-400 shrink-0" />
            <span className="text-slate-400 font-medium">Exploded Separation:</span>
            <input
              type="range"
              min="0"
              max="100"
              value={explodedOffset}
              onChange={(e) => onChangeExplodedOffset(Number(e.target.value))}
              className="w-28 accent-emerald-500 cursor-pointer"
            />
            <span className="font-mono text-emerald-400 font-bold w-9 text-right">{explodedOffset}%</span>
          </div>
        )}

        {/* Right: Speed & Subtitles */}
        <div className="flex items-center gap-2">
          {/* Speed Buttons */}
          <div className="flex items-center bg-slate-950 rounded-lg border border-slate-800 p-0.5 text-xs font-mono">
            {[0.5, 1, 1.5, 2].map((spd) => (
              <button
                key={spd}
                type="button"
                onClick={() => onChangeSpeed(spd)}
                className={`px-2 py-1 rounded text-[10px] font-bold transition-all ${
                  playbackSpeed === spd ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                {spd}x
              </button>
            ))}
          </div>

          {/* Subtitles Button */}
          <button
            type="button"
            onClick={onToggleSubtitles}
            className={`p-2 rounded-lg border transition-all ${
              subtitlesEnabled
                ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-white'
            }`}
            title="Toggle Live Step Subtitles"
          >
            <Subtitles className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
