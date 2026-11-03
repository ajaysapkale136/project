import React, { useState, useRef } from 'react';
import { AnatomyScene, IsometricAssembly, AnimationStep } from '../types';
import { ZoomIn, ZoomOut, RotateCcw, Maximize2, Minimize2, Eye, ShieldCheck, Layers, Play } from 'lucide-react';

interface Props {
  scene: AnatomyScene;
  currentStepIndex: number;
  explodedOffset: number; // 0 to 100
  onSelectAssembly: (asm: IsometricAssembly) => void;
  selectedAssemblyId?: string;
  subtitlesEnabled: boolean;
  isolatedAssemblyId?: string;
  onToggleIsolate?: (assemblyId?: string) => void;
}

export const AnatomyCanvas: React.FC<Props> = ({
  scene,
  currentStepIndex,
  explodedOffset,
  onSelectAssembly,
  selectedAssemblyId,
  subtitlesEnabled,
  isolatedAssemblyId,
  onToggleIsolate
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hoveredAssembly, setHoveredAssembly] = useState<IsometricAssembly | null>(null);
  const [zoom, setZoom] = useState<number>(1.0);
  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [dragStart, setDragStart] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);

  const currentStep: AnimationStep | undefined = scene.steps[currentStepIndex];

  // Isometric projection helper (Azimuth 45°, Elevation 30°)
  // Screen X = (x - y) * cos(30°)
  // Screen Y = (x + y) * sin(30°) - z
  const projectIso = (x: number, y: number, z: number) => {
    const originX = 420;
    const originY = 160;
    const cos30 = 0.866;
    const sin30 = 0.5;

    const sx = originX + (x - y) * cos30;
    const sy = originY + (x + y) * sin30 - z;
    return { sx, sy };
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button === 0) { // Left click drag
      setIsDragging(true);
      setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
    }
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging) {
      setPan({ x: e.clientX - dragStart.x, y: e.clientY - dragStart.y });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const resetCamera = () => {
    setZoom(1.0);
    setPan({ x: 0, y: 0 });
  };

  const toggleFullscreen = () => {
    if (!containerRef.current) return;
    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  const isIsolated = !!isolatedAssemblyId;

  return (
    <div
      ref={containerRef}
      className={`relative w-full ${isFullscreen ? 'h-screen' : 'h-[580px]'} bg-slate-950 border border-slate-800 rounded-xl overflow-hidden shadow-2xl flex flex-col justify-between select-none`}
    >
      {/* Top Anatomy Card Frame (Title, Hint, Readout) */}
      <div className="p-3.5 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/30">
            {scene.animationFormat === 'MODULE_TO_MODULE' ? 'RIG 01' : (scene.animationFormat === 'INTERNAL_EXPLODED' ? 'EXP 02' : 'TRACE 03')}
          </span>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide">{scene.title}</h2>
            <p className="text-[11px] text-slate-400 mt-0.5 line-clamp-1">{scene.subtitle}</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right hidden sm:block">
            <div className="text-xs font-mono font-bold text-emerald-400">{scene.readout}</div>
            <div className="text-[10px] text-slate-500 font-mono">ANATOMY ISOMETRIC RIG</div>
          </div>
        </div>
      </div>

      {/* Floating Canvas Controls Overlay */}
      <div className="absolute top-16 right-4 flex flex-col gap-1.5 z-20 bg-slate-900/80 backdrop-blur border border-slate-800 rounded-lg p-1.5 shadow-lg">
        <button
          onClick={() => setZoom((z) => Math.min(2.5, z + 0.2))}
          title="Zoom In"
          className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <ZoomIn className="w-4 h-4" />
        </button>
        <button
          onClick={() => setZoom((z) => Math.max(0.6, z - 0.2))}
          title="Zoom Out"
          className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <ZoomOut className="w-4 h-4" />
        </button>
        <button
          onClick={resetCamera}
          title="Reset Camera (100%)"
          className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
        <div className="w-full h-px bg-slate-800 my-0.5" />
        {onToggleIsolate && (
          <button
            onClick={() => onToggleIsolate(selectedAssemblyId)}
            title={isIsolated ? 'Show All Components' : 'Isolate Selected Component'}
            className={`p-1.5 rounded transition ${isIsolated ? 'bg-amber-500/20 text-amber-400 border border-amber-500/40' : 'text-slate-400 hover:text-white hover:bg-slate-800'}`}
          >
            <Eye className="w-4 h-4" />
          </button>
        )}
        <button
          onClick={toggleFullscreen}
          title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen Canvas'}
          className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition"
        >
          {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
        </button>
      </div>

      {/* Main SVG Engineering Stage */}
      <div
        className="flex-1 relative flex items-center justify-center overflow-hidden cursor-grab active:cursor-grabbing"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        <svg viewBox="0 0 850 500" className="w-full h-full select-none">
          <defs>
            {/* Ground Isometric Grid */}
            <pattern id="isoGrid" width="60" height="34.64" patternUnits="userSpaceOnUse">
              <path d="M 0 17.32 L 30 0 L 60 17.32 L 30 34.64 Z" fill="none" stroke="#1e293b" strokeWidth="0.8" />
            </pattern>

            {/* Gradients for four-tone shading */}
            <linearGradient id="chromeTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#475569" />
              <stop offset="100%" stopColor="#334155" />
            </linearGradient>
            <linearGradient id="chromeLeft" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#1e293b" />
              <stop offset="100%" stopColor="#0f172a" />
            </linearGradient>
            <linearGradient id="chromeRight" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#334155" />
              <stop offset="100%" stopColor="#1e293b" />
            </linearGradient>

            <linearGradient id="brassTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#b45309" />
              <stop offset="100%" stopColor="#92400e" />
            </linearGradient>
            <linearGradient id="brassLeft" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#78350f" />
              <stop offset="100%" stopColor="#451a03" />
            </linearGradient>

            <linearGradient id="steelTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#64748b" />
              <stop offset="100%" stopColor="#475569" />
            </linearGradient>
            <linearGradient id="steelLeft" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#334155" />
              <stop offset="100%" stopColor="#1e293b" />
            </linearGradient>

            <linearGradient id="glassTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#0284c7" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#0369a1" stopOpacity="0.9" />
            </linearGradient>

            <linearGradient id="matteDarkTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#1e293b" />
              <stop offset="100%" stopColor="#0f172a" />
            </linearGradient>
          </defs>

          {/* Grid Background */}
          <rect width="100%" height="100%" fill="url(#isoGrid)" />

          {/* Zoom and Pan Layer */}
          <g transform={`translate(${pan.x}, ${pan.y}) scale(${zoom})`} transform-origin="425 250">
            {/* Draw Visual Connections (Conduits / Pipes) */}
            {scene.connections.map((conn) => {
              const src = scene.assemblies.find((a) => a.id === conn.sourceAssemblyId);
              const tgt = scene.assemblies.find((a) => a.id === conn.targetAssemblyId);
              if (!src || !tgt) return null;

              const effectiveSrcZ = src.z + (explodedOffset * src.explodedOffsetZ) / 100;
              const effectiveTgtZ = tgt.z + (explodedOffset * tgt.explodedOffsetZ) / 100;

              const p1 = projectIso(src.x + src.width / 2, src.y + src.depth / 2, effectiveSrcZ);
              const p2 = projectIso(tgt.x + tgt.width / 2, tgt.y + tgt.depth / 2, effectiveTgtZ);

              const isActiveConn = currentStep?.activeConnectionId === conn.id;

              // Color-coded connection types
              const strokeColor = isActiveConn
                ? '#10b981'
                : conn.connectionType.includes('AUTH')
                ? '#38bdf8'
                : conn.connectionType.includes('JPA') || conn.connectionType.includes('SQL')
                ? '#f59e0b'
                : '#334155';

              // Connection Dimming if isolated
              const connDimmed = isIsolated &&
                src.id !== isolatedAssemblyId &&
                tgt.id !== isolatedAssemblyId;

              return (
                <g key={conn.id} opacity={connDimmed ? 0.15 : 1} className="transition-opacity duration-300">
                  {/* Conduit Shadow */}
                  <line
                    x1={p1.sx}
                    y1={p1.sy}
                    x2={p2.sx}
                    y2={p2.sy}
                    stroke="#0f172a"
                    strokeWidth="8"
                    strokeLinecap="round"
                  />
                  {/* Conduit Pipe Body */}
                  <line
                    x1={p1.sx}
                    y1={p1.sy}
                    x2={p2.sx}
                    y2={p2.sy}
                    stroke={strokeColor}
                    strokeWidth={isActiveConn ? '3.5' : '2'}
                    strokeDasharray={isActiveConn ? '6 4' : 'none'}
                    className={isActiveConn ? 'animate-pulse' : ''}
                  />

                  {/* Moving Animated Data Packet for Active/Animated Conduits */}
                  {(isActiveConn || conn.isAnimated) && (
                    <circle r="4" fill={isActiveConn ? '#34d399' : '#38bdf8'}>
                      <animateMotion
                        path={`M ${p1.sx} ${p1.sy} L ${p2.sx} ${p2.sy}`}
                        dur={isActiveConn ? '1.5s' : '3s'}
                        repeatCount="indefinite"
                      />
                    </circle>
                  )}

                  {/* Connection Label */}
                  <text
                    x={(p1.sx + p2.sx) / 2}
                    y={(p1.sy + p2.sy) / 2 - 8}
                    fill={isActiveConn ? '#34d399' : '#64748b'}
                    fontSize="9"
                    fontFamily="monospace"
                    textAnchor="middle"
                    fontWeight="bold"
                  >
                    {conn.label}
                  </text>
                </g>
              );
            })}

            {/* Draw Assemblies in Painter's Order (Sorted by Depth / Z) */}
            {[...scene.assemblies]
              .sort((a, b) => a.x + a.y - (b.x + b.y))
              .map((asm) => {
                const effectiveZ = asm.z + (explodedOffset * asm.explodedOffsetZ) / 100;
                const { sx, sy } = projectIso(asm.x, asm.y, effectiveZ);

                const isSelected = selectedAssemblyId === asm.id;
                const isActiveInStep = currentStep?.activeAssemblyId === asm.id;
                const isTargetInStep = currentStep?.targetAssemblyId === asm.id;

                const isDimmed = isIsolated && asm.id !== isolatedAssemblyId && asm.id !== 'rig_baseplate' && asm.id !== 'exp_bedplate';

                const w = asm.width * 0.866;
                const d = asm.depth * 0.5;
                const h = asm.height;

                // Top face polygon
                const topPts = `0,${d} ${w},0 ${w + d},${d} ${d},${d * 2}`;
                // Front left face
                const leftPts = `0,${d} 0,${d + h} ${d},${d * 2 + h} ${d},${d * 2}`;
                // Front right face
                const rightPts = `${d},${d * 2} ${d},${d * 2 + h} ${w + d},${d + h} ${w + d},${d}`;

                const topFill = asm.material === 'BRASS' ? 'url(#brassTop)' :
                                asm.material === 'GLASS' ? 'url(#glassTop)' :
                                asm.material === 'STEEL' ? 'url(#steelTop)' :
                                asm.material === 'MATTE_DARK' ? 'url(#matteDarkTop)' : 'url(#chromeTop)';

                const leftFill = asm.material === 'BRASS' ? 'url(#brassLeft)' :
                                 asm.material === 'STEEL' ? 'url(#steelLeft)' :
                                 asm.material === 'MATTE_DARK' ? '#0f172a' : 'url(#chromeLeft)';

                const rightFill = asm.material === 'BRASS' ? '#5c2207' :
                                  asm.material === 'STEEL' ? '#1e293b' :
                                  asm.material === 'MATTE_DARK' ? '#020617' : 'url(#chromeRight)';

                const strokeColor = isActiveInStep ? '#10b981' : isSelected ? '#38bdf8' : '#475569';
                const strokeWidth = isActiveInStep || isSelected ? '2.5' : '1.2';

                return (
                  <g
                    key={asm.id}
                    transform={`translate(${sx}, ${sy})`}
                    opacity={isDimmed ? 0.15 : 1}
                    className="cursor-pointer transition-all duration-300"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectAssembly(asm);
                    }}
                    onMouseEnter={() => setHoveredAssembly(asm)}
                    onMouseLeave={() => setHoveredAssembly(null)}
                  >
                    {/* Left Face */}
                    <polygon points={leftPts} fill={leftFill} stroke={strokeColor} strokeWidth={strokeWidth} />
                    {/* Right Face */}
                    <polygon points={rightPts} fill={rightFill} stroke={strokeColor} strokeWidth={strokeWidth} />
                    {/* Top Face */}
                    <polygon points={topPts} fill={topFill} stroke={strokeColor} strokeWidth={strokeWidth} />

                    {/* Mechanical Screws / Alignment Ticks on Top Corners */}
                    <circle cx="6" cy={d} r="1.5" fill="#94a3b8" />
                    <circle cx={w + d - 6} cy={d} r="1.5" fill="#94a3b8" />
                    <circle cx={d} cy={d * 2 - 4} r="1.5" fill="#94a3b8" />

                    {/* Assembly Label */}
                    <text
                      x={d + 8}
                      y={d + 16}
                      fill={isActiveInStep ? '#34d399' : '#f8fafc'}
                      fontSize="10"
                      fontFamily="monospace"
                      fontWeight="bold"
                      textAnchor="middle"
                    >
                      {asm.name.length > 22 ? asm.name.substring(0, 20) + '…' : asm.name}
                    </text>

                    {/* Active Step Indicator Pulse */}
                    {isActiveInStep && (
                      <circle cx={d} cy={d} r="8" fill="#10b981" fillOpacity="0.4" className="animate-ping" />
                    )}
                  </g>
                );
              })}
          </g>
        </svg>

        {/* Hover Tooltip Card */}
        {hoveredAssembly && (
          <div className="absolute top-4 left-4 p-3 bg-slate-900/95 border border-slate-700 rounded-lg shadow-xl text-xs max-w-xs z-20 pointer-events-none">
            <div className="flex items-center justify-between mb-1">
              <span className="font-bold text-white">{hoveredAssembly.name}</span>
              <span className="font-mono text-[9px] uppercase px-1 rounded bg-slate-800 text-emerald-400">
                {hoveredAssembly.role}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-mono truncate">{hoveredAssembly.filePath || 'Substrate assembly'}</p>
            <div className="mt-1 text-[10px] text-emerald-400 font-mono">
              Material: {hoveredAssembly.material} • Provenance: {hoveredAssembly.verificationState}
            </div>
          </div>
        )}
      </div>

      {/* Subtitles Overlay Bar if enabled */}
      {subtitlesEnabled && currentStep && (
        <div className="px-6 py-2.5 bg-slate-900/90 border-t border-slate-800 flex items-center justify-between text-xs z-10">
          <div className="flex items-center gap-2">
            <span className="font-mono text-emerald-400 font-bold">{currentStep.stepCode}:</span>
            <span className="text-white font-medium">{currentStep.title}</span>
            <span className="text-slate-400 text-[11px]">— {currentStep.description}</span>
          </div>
          {currentStep.dataPayload && (
            <span className="font-mono text-[10px] text-slate-300 bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
              {currentStep.dataPayload}
            </span>
          )}
        </div>
      )}

      {/* Bottom Hint Bar */}
      <div className="px-4 py-2 bg-slate-950 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500 z-10">
        <span>{scene.hint}</span>
        <span className="font-mono text-[10px] text-emerald-400">Phase: {currentStep?.phase || 'IDLE'}</span>
      </div>
    </div>
  );
};
