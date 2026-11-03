import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import {
  Boxes,
  Layers,
  Activity,
  Download,
  Share2,
  FileCode,
  FileText,
  Video,
  CheckCircle2,
  Code,
  Database
} from 'lucide-react';
import { projectApi } from '../api/client';
import { AnatomyScene, IsometricAssembly, ExportResponse } from '../types';
import { AnatomyCanvas } from '../anatomy/AnatomyCanvas';
import { WebGLArchitectureCanvas } from '../anatomy/WebGLArchitectureCanvas';
import { SharedPlaybackBar } from '../anatomy/SharedPlaybackBar';

export const AnatomyVisualizerPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();

  const [activeFormat, setActiveFormat] = useState<'MODULE_TO_MODULE' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE' | 'DATABASE_INTELLIGENCE'>('MODULE_TO_MODULE');
  const [renderEngine, setRenderEngine] = useState<'WEBGL' | 'SVG'>('WEBGL');
  const [scene, setScene] = useState<AnatomyScene | null>(null);
  const [loading, setLoading] = useState(true);

  // Playback state
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [explodedOffset, setExplodedOffset] = useState(60);
  const [subtitlesEnabled, setSubtitlesEnabled] = useState(true);

  // Inspection
  const [selectedAssembly, setSelectedAssembly] = useState<IsometricAssembly | null>(null);

  // Export State
  const [exporting, setExporting] = useState(false);
  const [exportResult, setExportResult] = useState<ExportResponse | null>(null);

  const timerRef = useRef<any>(null);

  const loadScene = () => {
    if (!projectId) return;
    setLoading(true);
    setIsPlaying(false);
    setCurrentStepIndex(0);

    const fetcher = activeFormat === 'MODULE_TO_MODULE'
      ? projectApi.getModuleAnatomyScene(projectId)
      : activeFormat === 'INTERNAL_EXPLODED'
      ? projectApi.getExplodedAnatomyScene(projectId)
      : activeFormat === 'DATABASE_INTELLIGENCE' ? projectApi.getDatabaseAnatomyScene(projectId) : projectApi.getExecutionAnatomyScene(projectId);

    fetcher
      .then((data) => {
        setScene(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };

  useEffect(() => {
    loadScene();
  }, [projectId, activeFormat]);

  // Deterministic state machine animation loop
  useEffect(() => {
    if (!isPlaying || !scene || scene.steps.length === 0) {
      if (timerRef.current) clearTimeout(timerRef.current);
      return;
    }

    const currentStep = scene.steps[currentStepIndex];
    const duration = (currentStep?.durationMs || 2000) / playbackSpeed;

    timerRef.current = setTimeout(() => {
      setCurrentStepIndex((prev) => {
        if (prev >= scene.steps.length - 1) {
          setIsPlaying(false);
          return 0; // Loop or stop
        }
        return prev + 1;
      });
    }, duration);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [isPlaying, currentStepIndex, scene, playbackSpeed]);

  const handleExport = async (format: 'HTML' | 'SVG' | 'PDF' | 'MP4') => {
    if (!projectId || !scene) return;
    try {
      setExporting(true);
      const res = await projectApi.exportAnatomyScene(projectId, {
        exportFormat: format,
        animationFormat: activeFormat,
        targetIdentifier: scene.targetIdentifier
      });
      setExportResult(res);
      setExporting(false);

      if (format === 'HTML' || format === 'SVG') {
        // Trigger direct browser download
        const blob = new Blob([res.content], { type: res.mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = res.fileName;
        a.click();
        URL.revokeObjectURL(url);
      }
    } catch (e: any) {
      setExporting(false);
      alert('Export failed: ' + (e.message || 'Error'));
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Visualizer Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Anatomy Interactive Engineering Visualizer</h1>
          <p className="text-sm text-slate-400 mt-1">
            Isometric physical engineering models, separated component assemblies, and step-by-step execution animation.
          </p>
        </div>

        {/* Export Actions */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => handleExport('HTML')}
            disabled={exporting}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
            title="Export Standalone Interactive HTML"
          >
            <Download className="h-3.5 w-3.5 text-emerald-400" />
            <span>Standalone HTML</span>
          </button>

          <button
            type="button"
            onClick={() => handleExport('SVG')}
            disabled={exporting}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
            title="Export Vector SVG"
          >
            <FileCode className="h-3.5 w-3.5 text-sky-400" />
            <span>SVG</span>
          </button>

          <button
            type="button"
            onClick={() => handleExport('PDF')}
            disabled={exporting}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
            title="Export PDF Document"
          >
            <FileText className="h-3.5 w-3.5 text-purple-400" />
            <span>PDF Spec</span>
          </button>

          <button
            type="button"
            onClick={() => handleExport('MP4')}
            disabled={exporting}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
            title="MP4 Capture Pipeline"
          >
            <Video className="h-3.5 w-3.5 text-amber-400" />
            <span>MP4 Capture</span>
          </button>
        </div>
      </div>

      {/* 3-Format Mode Tabs */}
      <div className="grid grid-cols-4 bg-slate-900/80 p-1 border border-slate-800 rounded-xl text-xs font-semibold">
        <button
          type="button"
          onClick={() => setActiveFormat('MODULE_TO_MODULE')}
          className={`py-2.5 px-3 rounded-lg flex items-center justify-center gap-2 transition-all ${
            activeFormat === 'MODULE_TO_MODULE'
              ? 'bg-emerald-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Layers className="h-4 w-4" />
          <span>Format 1: Module-to-Module System Rig</span>
        </button>
        <button type="button" onClick={() => setActiveFormat('DATABASE_INTELLIGENCE')} className={`py-2.5 px-3 rounded-lg flex items-center justify-center gap-2 transition-all ${activeFormat === 'DATABASE_INTELLIGENCE' ? 'bg-emerald-600 text-white shadow-md' : 'text-slate-400 hover:text-white'}`}>
          <Database className="h-4 w-4" /><span>Database Intelligence</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveFormat('INTERNAL_EXPLODED')}
          className={`py-2.5 px-3 rounded-lg flex items-center justify-center gap-2 transition-all ${
            activeFormat === 'INTERNAL_EXPLODED'
              ? 'bg-emerald-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Boxes className="h-4 w-4" />
          <span>Format 2: Internal Exploded Component View</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveFormat('EXECUTION_TRACE')}
          className={`py-2.5 px-3 rounded-lg flex items-center justify-center gap-2 transition-all ${
            activeFormat === 'EXECUTION_TRACE'
              ? 'bg-emerald-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white'
          }`}
        >
          <Activity className="h-4 w-4" />
          <span>Format 3: End-to-End Execution Animation</span>
        </button>
      </div>

      {/* Main Interactive Stage */}
      {loading ? (
        <div className="p-16 text-center text-slate-500">Synthesizing physical isometric scene model...</div>
      ) : scene ? (
        <div className="space-y-4">
          <div className="flex justify-end gap-2 text-xs"><button onClick={() => setRenderEngine('WEBGL')} className={renderEngine === 'WEBGL' ? 'text-sky-300' : 'text-slate-500'}>WebGL 3D</button><button onClick={() => setRenderEngine('SVG')} className={renderEngine === 'SVG' ? 'text-sky-300' : 'text-slate-500'}>SVG</button></div>
          {renderEngine === 'WEBGL' ? <WebGLArchitectureCanvas scene={scene} activeAssemblyId={selectedAssembly?.id} onSelect={(id) => setSelectedAssembly(scene.assemblies.find((item) => item.id === id) || null)} /> : <AnatomyCanvas scene={scene} currentStepIndex={currentStepIndex} explodedOffset={explodedOffset} onSelectAssembly={setSelectedAssembly} selectedAssemblyId={selectedAssembly?.id} subtitlesEnabled={subtitlesEnabled} />}

          {/* Shared Playback Controls */}
          <SharedPlaybackBar
            isPlaying={isPlaying}
            onTogglePlay={() => setIsPlaying(!isPlaying)}
            onRestart={() => {
              setCurrentStepIndex(0);
              setIsPlaying(true);
            }}
            onPrevStep={() => setCurrentStepIndex((p) => Math.max(0, p - 1))}
            onNextStep={() => setCurrentStepIndex((p) => Math.min(scene.steps.length - 1, p + 1))}
            currentStepIndex={currentStepIndex}
            totalSteps={scene.steps.length}
            onSelectStep={(idx) => setCurrentStepIndex(idx)}
            steps={scene.steps}
            playbackSpeed={playbackSpeed}
            onChangeSpeed={(s) => setPlaybackSpeed(s)}
            explodedOffset={explodedOffset}
            onChangeExplodedOffset={(val) => setExplodedOffset(val)}
            subtitlesEnabled={subtitlesEnabled}
            onToggleSubtitles={() => setSubtitlesEnabled(!subtitlesEnabled)}
            showExplodedSlider={activeFormat === 'INTERNAL_EXPLODED'}
          />
        </div>
      ) : null}

      {/* Assembly Source Evidence Inspection Modal */}
      {selectedAssembly && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-xl w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="text-base font-bold text-white">{selectedAssembly.name}</h3>
                <p className="text-xs font-mono text-emerald-400 mt-0.5">Role: {selectedAssembly.role}</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedAssembly(null)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 space-y-1 font-mono text-[11px]">
                <p><span className="text-slate-500">Physical Material:</span> {selectedAssembly.material}</p>
                <p><span className="text-slate-500">Dimensions:</span> {selectedAssembly.width}w × {selectedAssembly.depth}d × {selectedAssembly.height}h</p>
                <p><span className="text-slate-500">Exploded Z Offset:</span> {selectedAssembly.explodedOffsetZ}px</p>
                <p><span className="text-slate-500">File Path:</span> {selectedAssembly.filePath || 'Root Substrate'}</p>
                <p><span className="text-slate-500">Provenance:</span> <span className="text-emerald-400">{selectedAssembly.verificationState}</span></p>
              </div>

              {selectedAssembly.metadata && (
                <div>
                  <span className="text-slate-400 block mb-1">Discovered Component Metadata:</span>
                  <pre className="p-3 bg-slate-950 rounded-lg text-emerald-300 font-mono text-[11px] overflow-x-auto border border-slate-800">
                    {JSON.stringify(selectedAssembly.metadata, null, 2)}
                  </pre>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setSelectedAssembly(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded text-xs font-semibold"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
