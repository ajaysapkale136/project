import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Sparkles, Layers, Play, Pause, SkipBack, SkipForward,
  Download, Send, ArrowRight, ShieldCheck, CheckCircle2,
  FileCode, Terminal, HelpCircle, RefreshCw, Cpu, Box,
  ChevronRight, ExternalLink, Sliders, Eye, Database, Code2, UploadCloud
} from 'lucide-react';
import { apiService } from '../api/client';
import { AnatomyCanvas } from '../anatomy/AnatomyCanvas';
import { WebGLArchitectureCanvas } from '../anatomy/WebGLArchitectureCanvas';
import {
  Project,
  AnatomyScene,
  IsometricAssembly,
  AnimationStep,
  StudioContextResponse,
  StudioChatMessage,
  StudioActionCard,
  StudioEvidenceDto,
  SourceEvidence
} from '../types';

export const AiEngineeringStudio: React.FC = () => {
  const { projectId: routeProjectId } = useParams<{ projectId?: string }>();
  const navigate = useNavigate();

  // Project & Studio Context
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [studioContext, setStudioContext] = useState<StudioContextResponse | null>(null);
  const [isLoadingContext, setIsLoadingContext] = useState<boolean>(true);
  const [showProjectModal, setShowProjectModal] = useState<boolean>(false);

  // Visualization Modes & Canvas State
  const [activeMode, setActiveMode] = useState<'MODULE_ASSEMBLY' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE'>('MODULE_ASSEMBLY');
  const [activeModuleId, setActiveModuleId] = useState<string>('default');
  const [activeOperation, setActiveOperation] = useState<string>('');
  const [currentScene, setCurrentScene] = useState<AnatomyScene | null>(null);
  const [isLoadingScene, setIsLoadingScene] = useState<boolean>(false);
  const [currentStepIndex, setCurrentStepIndex] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0);
  const [subtitlesEnabled, setSubtitlesEnabled] = useState<boolean>(true);
  const [explodedOffset, setExplodedOffset] = useState<number>(65); // 0 to 100%
  const [selectedAssembly, setSelectedAssembly] = useState<IsometricAssembly | null>(null);
  const [selectedEvidence, setSelectedEvidence] = useState<SourceEvidence[]>([]);
  const [isolatedAssemblyId, setIsolatedAssemblyId] = useState<string | undefined>(undefined);
  const [renderEngine, setRenderEngine] = useState<'WEBGL' | 'SVG'>('WEBGL');

  // AI Conversational Workspace
  const [messages, setMessages] = useState<StudioChatMessage[]>([]);
  const [inputPrompt, setInputPrompt] = useState<string>('');
  const [isAiResponding, setIsAiResponding] = useState<boolean>(false);
  const [preferredProvider, setPreferredProvider] = useState<'AUTO' | 'OPENAI' | 'CLAUDE' | 'GEMINI'>('AUTO');
  const chatScrollRef = useRef<HTMLDivElement>(null);

  const selectWebGlAssembly = useCallback((assemblyId: string) => {
    const assembly = currentScene?.assemblies.find((item) => item.id === assemblyId);
    if (assembly) setSelectedAssembly(assembly);
  }, [currentScene]);

  // 1. Initial Load: Fetch Projects & Select Active
  useEffect(() => {
    const init = async () => {
      try {
        setIsLoadingContext(true);
        const allProjects = await apiService.getProjects();
        setProjects(allProjects);

        let selected = allProjects.find((p) => p.id === routeProjectId);
        if (!selected && allProjects.length > 0) {
          selected = allProjects[0];
        }
        setCurrentProject(selected || null);

        if (selected) {
          await loadStudioData(selected.id);
        }
      } catch (err) {
        console.error('Failed to initialize studio:', err);
      } finally {
        setIsLoadingContext(false);
      }
    };
    init();
  }, [routeProjectId]);

  // 2. Load Studio Context & Initial Scene
  const loadStudioData = async (projId: string) => {
    try {
      const ctx = await apiService.getStudioContext(projId);
      setStudioContext(ctx);

      if (ctx.defaultModuleId) {
        setActiveModuleId(ctx.defaultModuleId);
      }
      if (ctx.defaultOperation) {
        setActiveOperation(ctx.defaultOperation);
      }

      // Initial AI greeting message
      setMessages([
        {
          id: 'welcome_msg',
          role: 'assistant',
          content: `Welcome to **AI Engineering Studio** for **${ctx.projectName}** (${ctx.primaryLanguage} • ${ctx.frameworkFamily}).

The canvas only shows source-discovered components and parser-backed relationships. Select a discovered module or operation to inspect its evidence.`,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          actionCards: [
            {
              id: 'c1',
              title: 'Inspect selected module',
              mode: 'INTERNAL_EXPLODED',
              targetId: ctx.defaultModuleId || 'default',
              description: 'Expose source-discovered internal components',
              icon: 'Layers'
            },
            {
              id: 'c2',
              title: 'Inspect selected operation',
              mode: 'EXECUTION_TRACE',
              targetId: ctx.defaultOperation || '',
              description: 'Show only source-supported execution evidence',
              icon: 'Play'
            },
            {
              id: 'c3',
              title: 'View System Bedplate',
              mode: 'MODULE_ASSEMBLY',
              targetId: 'root',
              description: 'Isometric machine model of all discovered modules',
              icon: 'Grid'
            }
          ]
        }
      ]);

      await fetchScene(projId, activeMode, ctx.defaultModuleId, ctx.defaultOperation);
    } catch (err) {
      console.error('Error loading studio data:', err);
    }
  };

  // 3. Fetch Scene for active mode
  const fetchScene = async (
    projId: string,
    mode: 'MODULE_ASSEMBLY' | 'INTERNAL_EXPLODED' | 'EXECUTION_TRACE',
    modId?: string,
    operation?: string
  ) => {
    try {
      setIsLoadingScene(true);
      let scene: AnatomyScene;
      if (mode === 'MODULE_ASSEMBLY') {
        scene = await apiService.getModuleAnatomyScene(projId);
      } else if (mode === 'INTERNAL_EXPLODED') {
        scene = await apiService.getExplodedAnatomyScene(projId, modId);
      } else {
        scene = await apiService.getExecutionAnatomyScene(projId, operation);
      }

      setCurrentScene(scene);
      setCurrentStepIndex(0);
      setIsPlaying(false);

      if (scene.assemblies.length > 0) {
        setSelectedAssembly(scene.assemblies[0]);
      }
    } catch (err) {
      console.error('Failed to fetch anatomy scene:', err);
    } finally {
      setIsLoadingScene(false);
    }
  };

  // 4. Step Playback Loop for Execution Trace
  useEffect(() => {
    let timer: any;
    if (isPlaying && currentScene && currentScene.steps.length > 0) {
      const step = currentScene.steps[currentStepIndex];
      const duration = (step?.durationMs || 1500) / playbackSpeed;

      timer = setTimeout(() => {
        if (currentStepIndex < currentScene.steps.length - 1) {
          setCurrentStepIndex((prev) => prev + 1);
        } else {
          setIsPlaying(false);
        }
      }, duration);
    }
    return () => clearTimeout(timer);
  }, [isPlaying, currentStepIndex, currentScene, playbackSpeed]);

  // Synchronize selected assembly when execution step changes
  useEffect(() => {
    if (currentScene && currentScene.steps.length > 0) {
      const step = currentScene.steps[currentStepIndex];
      if (step?.activeAssemblyId) {
        const found = currentScene.assemblies.find((a) => a.id === step.activeAssemblyId);
        if (found) {
          setSelectedAssembly(found);
        }
      }
    }
  }, [currentStepIndex, currentScene]);

  useEffect(() => {
    if (!currentProject || !selectedAssembly?.componentId) {
      setSelectedEvidence([]);
      return;
    }
    apiService.getComponentEvidence(currentProject.id, selectedAssembly.componentId)
      .then(setSelectedEvidence)
      .catch(() => setSelectedEvidence([]));
  }, [currentProject, selectedAssembly?.componentId]);

  // Auto-scroll chat
  useEffect(() => {
    chatScrollRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isAiResponding]);

  // 5. Handle AI Chat Query
  const handleSendPrompt = async (promptText?: string) => {
    const textToSend = promptText || inputPrompt;
    if (!textToSend.trim() || !currentProject || isAiResponding) return;

    const userMsg: StudioChatMessage = {
      id: 'usr_' + Date.now(),
      role: 'user',
      content: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputPrompt('');
    setIsAiResponding(true);

    try {
      const response = await apiService.queryStudio(currentProject.id, {
        prompt: textToSend,
        activeModuleId,
        activeOperation,
        preferredProvider
      });

      const assistantMsg: StudioChatMessage = {
        id: 'ast_' + Date.now(),
        role: 'assistant',
        content: response.answer,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        actionCards: response.actionCards,
        sourceEvidence: response.sourceEvidence,
        trace: response.orchestrationTrace,
        recommendedMode: response.recommendedMode
      };

      setMessages((prev) => [...prev, assistantMsg]);

      // Automatically switch 3D canvas to recommended mode & target
      if (response.recommendedMode) {
        setActiveMode(response.recommendedMode);
        const modToUse = response.targetModuleId || activeModuleId;
        const opToUse = response.targetOperation || activeOperation;

        if (response.targetModuleId) setActiveModuleId(response.targetModuleId);
        if (response.targetOperation) setActiveOperation(response.targetOperation);

        await fetchScene(currentProject.id, response.recommendedMode, modToUse, opToUse);

        if (response.stepIndex !== undefined) {
          setCurrentStepIndex(response.stepIndex);
        }
      }
    } catch (err: any) {
      console.error('Failed to query studio AI:', err);
      setMessages((prev) => [
        ...prev,
        {
          id: 'err_' + Date.now(),
          role: 'assistant',
          content: `⚠️ Encountered an issue connecting to AI services: ${err.message || 'Unknown error'}. Switching to local AST analysis mode.`,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } finally {
      setIsAiResponding(false);
    }
  };

  // 6. Action Card Click Handler
  const handleActionCardClick = async (card: StudioActionCard) => {
    if (!currentProject) return;

    if (card.mode === 'INTERNAL_EXPLODED') {
      setActiveMode('INTERNAL_EXPLODED');
      if (card.targetId && card.targetId !== 'auth') {
        setActiveModuleId(card.targetId);
      }
      await fetchScene(currentProject.id, 'INTERNAL_EXPLODED', card.targetId);
    } else if (card.mode === 'EXECUTION_TRACE') {
      setActiveMode('EXECUTION_TRACE');
      if (card.targetId) {
        setActiveOperation(card.targetId);
      }
      await fetchScene(currentProject.id, 'EXECUTION_TRACE', activeModuleId, card.targetId);
      setIsPlaying(true);
    } else if (card.mode === 'MODULE_ASSEMBLY') {
      setActiveMode('MODULE_ASSEMBLY');
      await fetchScene(currentProject.id, 'MODULE_ASSEMBLY');
    }
  };

  // 7. Export Current Scene
  const handleExport = async (format: 'STANDALONE_HTML' | 'SVG' | 'PDF') => {
    if (!currentProject) return;
    try {
      const res = await apiService.exportAnatomyScene(currentProject.id, {
        animationFormat: activeMode,
        targetIdentifier: activeMode === 'INTERNAL_EXPLODED' ? activeModuleId : activeOperation,
        exportFormat: format
      });

      const blob = new Blob([res.content], { type: res.mimeType });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = res.fileName;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export failed:', err);
    }
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-slate-950 text-slate-100 font-sans overflow-hidden">
      {/* ==================================================================== */}
      {/* 1. TOP HEADER: PROJECT CONTEXT & MULTI-AI SELECTOR                    */}
      {/* ==================================================================== */}
      <header className="h-14 border-b border-slate-800 bg-slate-900/90 backdrop-blur-md px-4 flex items-center justify-between shrink-0 z-30">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <Box className="w-4 h-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm tracking-tight text-white">AI Engineering Studio</span>
              <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-300 border border-slate-700">
                Anatomy Engine
              </span>
            </div>
            <p className="text-[10px] text-slate-400 hidden sm:block">
              Interactive 3D Software Architecture & Execution Explainer
            </p>
          </div>
        </div>

        {/* Project Context & Switcher */}
        <div className="flex items-center gap-2">
          {studioContext && (
            <div className="hidden md:flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800 text-xs">
              <span className="font-semibold text-slate-200">{studioContext.projectName}</span>
              <span className="text-slate-600">•</span>
              <span className="font-mono text-[10px] text-emerald-400">{studioContext.frameworkFamily}</span>
              <span className="text-slate-600">•</span>
              <span className="font-mono text-[10px] text-slate-400">{studioContext.commitHash}</span>
            </div>
          )}

          <button
            onClick={() => setShowProjectModal(true)}
            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-lg border border-slate-700 flex items-center gap-1.5 transition"
          >
            <UploadCloud className="w-3.5 h-3.5 text-emerald-400" />
            <span>Switch / Ingest</span>
          </button>

          {/* Model Routing Selector */}
          <div className="flex items-center gap-1 bg-slate-900 border border-slate-800 rounded-lg p-1 text-xs">
            <Sparkles className="w-3.5 h-3.5 text-amber-400 ml-1.5" />
            <select
              value={preferredProvider}
              onChange={(e) => setPreferredProvider(e.target.value as any)}
              className="bg-transparent text-slate-300 text-xs border-none outline-none pr-2 cursor-pointer font-medium"
            >
              <option value="AUTO" className="bg-slate-900 text-slate-200">Auto (GPT + Claude + Gemini)</option>
              <option value="OPENAI" className="bg-slate-900 text-slate-200">OpenAI GPT-6 Astra</option>
              <option value="CLAUDE" className="bg-slate-900 text-slate-200">Anthropic Claude Opus 5</option>
              <option value="GEMINI" className="bg-slate-900 text-slate-200">Google Gemini 4 Argon</option>
            </select>
          </div>
        </div>
      </header>

      {/* ==================================================================== */}
      {/* 2. MAIN SPLIT WORKSPACE: AI CHAT (LEFT) & 3D CANVAS (RIGHT)          */}
      {/* ==================================================================== */}
      <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
        {/* ------------------------------------------------------------------ */}
        {/* LEFT PANE: CONVERSATIONAL AI & REASONING (40% width)               */}
        {/* ------------------------------------------------------------------ */}
        <div className="w-full md:w-[42%] lg:w-[38%] border-r border-slate-800 flex flex-col justify-between bg-slate-950 shrink-0">
          {/* Quick Module Selection Pills */}
          <div className="p-3 border-b border-slate-800/80 bg-slate-900/40">
            <div className="text-[11px] font-mono font-medium text-slate-400 uppercase tracking-wider mb-2 flex items-center justify-between">
              <span>Discovered Architecture Modules</span>
              <span className="text-emerald-400">{studioContext?.modules.length || 0} Modules</span>
            </div>
            <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-thin">
              <button
                onClick={() => {
                  setActiveMode('MODULE_ASSEMBLY');
                  if (currentProject) fetchScene(currentProject.id, 'MODULE_ASSEMBLY');
                }}
                className={`px-2.5 py-1 rounded-full text-xs font-mono font-medium whitespace-nowrap border transition ${
                  activeMode === 'MODULE_ASSEMBLY'
                    ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                    : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-white'
                }`}
              >
                All Modules
              </button>

              {studioContext?.modules.map((m) => (
                <button
                  key={m.id}
                  onClick={() => {
                    setActiveModuleId(m.id);
                    setActiveMode('INTERNAL_EXPLODED');
                    if (currentProject) fetchScene(currentProject.id, 'INTERNAL_EXPLODED', m.id);
                    handleSendPrompt(`Explain the ${m.name}`);
                  }}
                  className={`px-2.5 py-1 rounded-full text-xs font-mono font-medium whitespace-nowrap border transition ${
                    activeModuleId === m.id && activeMode === 'INTERNAL_EXPLODED'
                      ? 'bg-sky-500/20 text-sky-400 border-sky-500/40'
                      : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-white'
                  }`}
                >
                  {m.name}
                </button>
              ))}
            </div>
          </div>

          {/* Chat Messages History */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div className="flex items-center gap-1.5 mb-1 px-1 text-[10px] text-slate-500 font-mono">
                  {msg.role === 'assistant' ? (
                    <>
                      <Sparkles className="w-3 h-3 text-emerald-400" />
                      <span className="font-semibold text-emerald-400">AI Assistant</span>
                    </>
                  ) : (
                    <span>Engineer</span>
                  )}
                  <span>•</span>
                  <span>{msg.timestamp}</span>
                </div>

                <div
                  className={`p-3.5 rounded-xl max-w-[95%] text-xs leading-relaxed ${
                    msg.role === 'user'
                      ? 'bg-emerald-600/90 text-white rounded-br-none shadow-md'
                      : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-bl-none shadow-md'
                  }`}
                >
                  <div className="whitespace-pre-wrap font-sans prose prose-invert prose-xs">
                    {msg.content}
                  </div>

                  {/* Interactive Action Cards */}
                  {msg.actionCards && msg.actionCards.length > 0 && (
                    <div className="mt-3 pt-3 border-t border-slate-800 flex flex-col gap-1.5">
                      <div className="text-[10px] font-mono text-slate-400 uppercase tracking-wide">
                        Interactive Canvas Actions:
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                        {msg.actionCards.map((card) => (
                          <button
                            key={card.id}
                            onClick={() => handleActionCardClick(card)}
                            className="p-2 rounded-lg bg-slate-950 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-left transition flex items-center justify-between group"
                          >
                            <div>
                              <div className="font-medium text-slate-200 group-hover:text-emerald-400 text-[11px]">
                                {card.title}
                              </div>
                              <div className="text-[10px] text-slate-500 line-clamp-1">{card.description}</div>
                            </div>
                            <ChevronRight className="w-3.5 h-3.5 text-slate-500 group-hover:text-emerald-400 transition" />
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Multi-AI Provenance Trace */}
                  {msg.trace && (
                    <div className="mt-2.5 pt-2 border-t border-slate-800/80 text-[10px] font-mono text-slate-500 flex items-center justify-between">
                      <span className="truncate">{msg.trace.taskDistribution}</span>
                      <span className="text-emerald-400 shrink-0 ml-2">{msg.trace.durationMs}ms</span>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {isAiResponding && (
              <div className="flex items-center gap-2 p-3 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-400 w-fit">
                <RefreshCw className="w-3.5 h-3.5 animate-spin text-emerald-400" />
                <span>Validating available providers against source evidence...</span>
              </div>
            )}
            <div ref={chatScrollRef} />
          </div>

          {/* Recommended Prompt Suggestions */}
          <div className="px-3 py-2 border-t border-slate-800/80 bg-slate-900/30">
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-thin">
              {[
                'Explain the authentication module',
                'Show me how the login operation works',
                'Explode the database & repository layers',
                'Show module assembly & data flows'
              ].map((suggestion, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendPrompt(suggestion)}
                  disabled={isAiResponding}
                  className="px-2.5 py-1 rounded bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-slate-200 text-[11px] font-mono border border-slate-800 whitespace-nowrap transition disabled:opacity-50"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>

          {/* Chat Prompt Input Bar */}
          <div className="p-3 border-t border-slate-800 bg-slate-900/60">
            <div className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 focus-within:border-emerald-500/60 transition">
              <input
                type="text"
                value={inputPrompt}
                onChange={(e) => setInputPrompt(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendPrompt()}
                placeholder="Ask to explain a module, trace an operation, or inspect code..."
                disabled={isAiResponding}
                className="bg-transparent flex-1 text-xs text-white placeholder-slate-500 outline-none"
              />
              <button
                onClick={() => handleSendPrompt()}
                disabled={!inputPrompt.trim() || isAiResponding}
                className="p-1.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 rounded-lg disabled:opacity-40 transition"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* ------------------------------------------------------------------ */}
        {/* RIGHT PANE: INTERACTIVE 3D ANATOMY ENGINE & EXPLANATION (60% width)*/}
        {/* ------------------------------------------------------------------ */}
        <div className="flex-1 flex flex-col justify-between overflow-y-auto bg-slate-950">
          {/* Canvas Mode Header & Switcher */}
          <div className="p-3 border-b border-slate-800 bg-slate-900/50 flex flex-wrap items-center justify-between gap-3 shrink-0">
            {/* 3 Visualization Modes */}
            <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800">
              <button
                onClick={() => {
                  setActiveMode('MODULE_ASSEMBLY');
                  if (currentProject) fetchScene(currentProject.id, 'MODULE_ASSEMBLY');
                }}
                className={`px-3 py-1.5 rounded-md text-xs font-mono font-medium flex items-center gap-1.5 transition ${
                  activeMode === 'MODULE_ASSEMBLY'
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 shadow-sm'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Box className="w-3.5 h-3.5" />
                <span>1. Module Rig</span>
              </button>

              <button
                onClick={() => {
                  setActiveMode('INTERNAL_EXPLODED');
                  if (currentProject) fetchScene(currentProject.id, 'INTERNAL_EXPLODED', activeModuleId);
                }}
                className={`px-3 py-1.5 rounded-md text-xs font-mono font-medium flex items-center gap-1.5 transition ${
                  activeMode === 'INTERNAL_EXPLODED'
                    ? 'bg-sky-500/20 text-sky-400 border border-sky-500/40 shadow-sm'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                <span>2. Exploded Assembly</span>
              </button>

              <button
                onClick={() => {
                  setActiveMode('EXECUTION_TRACE');
                  if (currentProject) fetchScene(currentProject.id, 'EXECUTION_TRACE', activeModuleId, activeOperation);
                }}
                className={`px-3 py-1.5 rounded-md text-xs font-mono font-medium flex items-center gap-1.5 transition ${
                  activeMode === 'EXECUTION_TRACE'
                    ? 'bg-amber-500/20 text-amber-400 border border-amber-500/40 shadow-sm'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Play className="w-3.5 h-3.5" />
                <span>3. Execution Trace</span>
              </button>
            </div>

            {/* Target Selectors & Export Dropdown */}
            <div className="flex items-center gap-2">
              {activeMode === 'INTERNAL_EXPLODED' && studioContext && (
                <select
                  value={activeModuleId}
                  onChange={(e) => {
                    setActiveModuleId(e.target.value);
                    if (currentProject) fetchScene(currentProject.id, 'INTERNAL_EXPLODED', e.target.value);
                  }}
                  className="bg-slate-900 text-xs font-mono text-slate-200 border border-slate-800 rounded-lg px-2.5 py-1.5 outline-none cursor-pointer"
                >
                  {studioContext.modules.map((m) => (
                    <option key={m.id} value={m.id}>{m.name}</option>
                  ))}
                </select>
              )}

              {activeMode === 'EXECUTION_TRACE' && studioContext && (
                <select
                  value={activeOperation}
                  onChange={(e) => {
                    setActiveOperation(e.target.value);
                    if (currentProject) fetchScene(currentProject.id, 'EXECUTION_TRACE', activeModuleId, e.target.value);
                  }}
                  className="bg-slate-900 text-xs font-mono text-slate-200 border border-slate-800 rounded-lg px-2.5 py-1.5 outline-none cursor-pointer"
                >
                  <option value="POST /api/auth/login">POST /api/auth/login (Authentication)</option>
                  <option value="POST /api/orders">POST /api/orders (Checkout)</option>
                </select>
              )}

              {/* Export Button */}
              <div className="relative group">
                <button className="px-2.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs rounded-lg border border-slate-800 flex items-center gap-1.5 transition">
                  <Download className="w-3.5 h-3.5" />
                  <span>Export</span>
                </button>
                <div className="absolute right-0 top-full mt-1 hidden group-hover:flex flex-col bg-slate-900 border border-slate-800 rounded-lg shadow-2xl py-1 w-36 z-30">
                  <button
                    onClick={() => handleExport('STANDALONE_HTML')}
                    className="px-3 py-1.5 text-left text-xs text-slate-300 hover:bg-slate-800 hover:text-white"
                  >
                    Standalone HTML
                  </button>
                  <button
                    onClick={() => handleExport('SVG')}
                    className="px-3 py-1.5 text-left text-xs text-slate-300 hover:bg-slate-800 hover:text-white"
                  >
                    Vector SVG
                  </button>
                  <button
                    onClick={() => handleExport('PDF')}
                    className="px-3 py-1.5 text-left text-xs text-slate-300 hover:bg-slate-800 hover:text-white"
                  >
                    A4 PDF Still
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Interactive 3D Anatomy Canvas */}
          <div className="p-4 flex-1 flex flex-col justify-between">
            {isLoadingScene ? (
              <div className="w-full h-[580px] bg-slate-950 border border-slate-800 rounded-xl flex items-center justify-center text-slate-400 gap-2">
                <RefreshCw className="w-5 h-5 animate-spin text-emerald-400" />
                <span className="text-xs font-mono">Generating verified isometric scene geometry...</span>
              </div>
            ) : currentScene ? (
              <>
                <div className="mb-2 flex justify-end gap-2 text-[10px] font-mono">
                  <button onClick={() => setRenderEngine('WEBGL')} className={`rounded px-2 py-1 ${renderEngine === 'WEBGL' ? 'bg-sky-500/20 text-sky-300' : 'text-slate-400'}`}>WEBGL 3D</button>
                  <button onClick={() => setRenderEngine('SVG')} className={`rounded px-2 py-1 ${renderEngine === 'SVG' ? 'bg-sky-500/20 text-sky-300' : 'text-slate-400'}`}>SVG fallback</button>
                </div>
                {renderEngine === 'WEBGL' ? (
                  <WebGLArchitectureCanvas scene={currentScene} activeAssemblyId={selectedAssembly?.id} onSelect={selectWebGlAssembly} />
                ) : (
                  <AnatomyCanvas
                    scene={currentScene}
                    currentStepIndex={currentStepIndex}
                    explodedOffset={explodedOffset}
                    onSelectAssembly={(asm) => setSelectedAssembly(asm)}
                    selectedAssemblyId={selectedAssembly?.id}
                    subtitlesEnabled={subtitlesEnabled}
                    isolatedAssemblyId={isolatedAssemblyId}
                    onToggleIsolate={(id) => setIsolatedAssemblyId((prev) => (prev ? undefined : id))}
                  />
                )}
              </>
            ) : null}

            {/* Playback & Scrubber Controls */}
            {currentScene && (
              <div className="mt-3 p-3 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-4">
                {activeMode === 'INTERNAL_EXPLODED' ? (
                  /* Separation Slider for Exploded View */
                  <div className="flex items-center gap-3 w-full">
                    <Sliders className="w-4 h-4 text-sky-400" />
                    <span className="text-xs font-mono text-slate-300 font-medium">Vertical Separation:</span>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={explodedOffset}
                      onChange={(e) => setExplodedOffset(parseInt(e.target.value))}
                      className="flex-1 accent-sky-500 cursor-pointer"
                    />
                    <span className="font-mono text-xs font-bold text-sky-400 w-12 text-right">{explodedOffset}%</span>
                  </div>
                ) : (
                  /* Timeline Playback Bar for Execution Trace */
                  <>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setCurrentStepIndex((prev) => Math.max(0, prev - 1))}
                        disabled={currentStepIndex === 0}
                        className="p-1.5 text-slate-400 hover:text-white disabled:opacity-30 rounded transition"
                      >
                        <SkipBack className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => setIsPlaying((p) => !p)}
                        className="p-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 rounded-lg transition"
                      >
                        {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                      </button>

                      <button
                        onClick={() => setCurrentStepIndex((prev) => Math.min(currentScene.steps.length - 1, prev + 1))}
                        disabled={currentStepIndex === currentScene.steps.length - 1}
                        className="p-1.5 text-slate-400 hover:text-white disabled:opacity-30 rounded transition"
                      >
                        <SkipForward className="w-4 h-4" />
                      </button>

                      <span className="font-mono text-xs text-slate-400 ml-2">
                        Step {currentStepIndex + 1} of {currentScene.steps.length}
                      </span>
                    </div>

                    {/* Step Scrubber */}
                    <div className="flex-1 max-w-xs mx-4">
                      <input
                        type="range"
                        min="0"
                        max={Math.max(0, currentScene.steps.length - 1)}
                        value={currentStepIndex}
                        onChange={(e) => setCurrentStepIndex(parseInt(e.target.value))}
                        className="w-full accent-emerald-500 cursor-pointer"
                      />
                    </div>

                    <div className="flex items-center gap-3 text-xs font-mono text-slate-400">
                      <select
                        value={playbackSpeed}
                        onChange={(e) => setPlaybackSpeed(parseFloat(e.target.value))}
                        className="bg-slate-950 border border-slate-800 rounded px-2 py-1 text-slate-200 outline-none"
                      >
                        <option value="0.5">0.5x</option>
                        <option value="1.0">1.0x</option>
                        <option value="2.0">2.0x</option>
                      </select>

                      <label className="flex items-center gap-1.5 cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={subtitlesEnabled}
                          onChange={(e) => setSubtitlesEnabled(e.target.checked)}
                          className="accent-emerald-500"
                        />
                        <span>Subtitles</span>
                      </label>
                    </div>
                  </>
                )}
              </div>
            )}

            {/* Synchronized Technical Explanation & Source Evidence Panel */}
            {selectedAssembly && (
              <div className="mt-3 p-4 bg-slate-900 border border-slate-800 rounded-xl">
                <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                      {selectedAssembly.role}
                    </span>
                    <h3 className="font-bold text-sm text-white">{selectedAssembly.name}</h3>
                  </div>

                  <span className="text-[10px] font-mono text-emerald-400 border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 rounded">
                    PROVENANCE: {selectedAssembly.verificationState}
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3 text-xs">
                  <div>
                    <span className="text-[10px] font-mono uppercase text-slate-400 font-medium">Source Evidence:</span>
                    <div className="mt-1 font-mono text-slate-300 bg-slate-950 p-2.5 rounded-lg border border-slate-800 truncate">
                      {selectedAssembly.filePath || 'Core Substrate Component'}
                    </div>

                    {selectedEvidence[0] && (
                      <pre className="mt-2 max-h-40 overflow-auto whitespace-pre-wrap font-mono text-[10px] leading-relaxed text-slate-300 bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                        {selectedEvidence[0].filePath}:{selectedEvidence[0].startLine}-{selectedEvidence[0].endLine}{'\n'}{selectedEvidence[0].codeSnippet}
                      </pre>
                    )}

                    <div className="mt-2 text-slate-400 text-[11px] leading-relaxed">
                      Material: <strong className="text-slate-300">{selectedAssembly.material}</strong> • Depth Offset: <strong className="text-slate-300">{selectedAssembly.explodedOffsetZ}px</strong>
                    </div>
                  </div>

                  <div>
                    <span className="text-[10px] font-mono uppercase text-slate-400 font-medium">Inspection note:</span>
                    <p className="mt-1 text-slate-300 text-[11px] leading-relaxed bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                      {selectedAssembly.metadata?.description || `Detected role: ${selectedAssembly.role}. Inspect the linked source evidence before treating any relationship as proven.`}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 3. SWITCH / INGEST PROJECT MODAL                                     */}
      {/* ==================================================================== */}
      {showProjectModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 max-w-md w-full shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
              <h3 className="font-bold text-sm text-white">Switch or Ingest Project</h3>
              <button
                onClick={() => setShowProjectModal(false)}
                className="text-slate-500 hover:text-white text-xs"
              >
                ✕
              </button>
            </div>

            <div className="space-y-2 mb-4">
              <span className="text-[11px] font-mono text-slate-400 uppercase">Managed Projects:</span>
              <div className="max-h-48 overflow-y-auto space-y-1.5 scrollbar-thin">
                {projects.map((p) => (
                  <button
                    key={p.id}
                    onClick={() => {
                      setCurrentProject(p);
                      setShowProjectModal(false);
                      loadStudioData(p.id);
                      navigate(`/studio/${p.id}`);
                    }}
                    className={`w-full p-2.5 rounded-lg text-left text-xs flex items-center justify-between border transition ${
                      currentProject?.id === p.id
                        ? 'bg-emerald-500/10 border-emerald-500/40 text-emerald-400'
                        : 'bg-slate-950 border-slate-800 text-slate-300 hover:bg-slate-800'
                    }`}
                  >
                    <div>
                      <div className="font-medium text-white">{p.name}</div>
                      <div className="text-[10px] text-slate-500 truncate">{p.repositoryUrl || 'ZIP Ingested'}</div>
                    </div>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">
                      {p.status}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-between">
              <button
                onClick={() => {
                  setShowProjectModal(false);
                  navigate('/import');
                }}
                className="px-3 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-medium text-xs rounded-lg transition"
              >
                + Ingest New Project ZIP / GitHub
              </button>
              <button
                onClick={() => setShowProjectModal(false)}
                className="px-3 py-2 bg-slate-800 text-slate-300 text-xs rounded-lg hover:bg-slate-700 transition"
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
