import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  UploadCloud,
  Github,
  FolderCode,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Terminal,
  ArrowRight
} from 'lucide-react';
import { projectApi, subscribeJobProgress } from '../api/client';
import { JobProgressEvent } from '../types';

export const ImportProject: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'ZIP' | 'GITHUB' | 'WORKSPACE'>('ZIP');
  const [projectName, setProjectName] = useState('');
  const [description, setDescription] = useState('');

  // Tab A: ZIP
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Tab B: GitHub
  const [githubUrl, setGithubUrl] = useState('');
  const [branch, setBranch] = useState('main');
  const [accessToken, setAccessToken] = useState('');

  // Tab C: Workspace
  const [workspacePath, setWorkspacePath] = useState('');

  // Processing state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [jobEvents, setJobEvents] = useState<JobProgressEvent[]>([]);
  const [latestProgress, setLatestProgress] = useState<number>(0);
  const [currentStep, setCurrentStep] = useState<string>('');
  const [createdProjectId, setCreatedProjectId] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleStartIngestion = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!projectName.trim()) {
      setErrorMsg('Please specify a project name');
      return;
    }

    try {
      setIsSubmitting(true);
      // Step 1: Create project record
      const project = await projectApi.createProject({
        name: projectName.trim(),
        description: description.trim(),
        repositoryUrl: activeTab === 'GITHUB' ? githubUrl : undefined,
        defaultBranch: activeTab === 'GITHUB' ? branch : undefined
      });
      setCreatedProjectId(project.id);

      let jobId = '';

      // Step 2: Trigger ingestion method
      if (activeTab === 'ZIP') {
        if (!selectedFile) {
          throw new Error('Please select a .zip project file to upload');
        }
        const res = await projectApi.uploadZip(project.id, selectedFile);
        jobId = res.jobId;
      } else if (activeTab === 'GITHUB') {
        if (!githubUrl.trim()) {
          throw new Error('Please provide a valid GitHub repository URL');
        }
        const res = await projectApi.importGitHub(project.id, {
          repositoryUrl: githubUrl.trim(),
          branch: branch.trim(),
          accessToken: accessToken.trim() || undefined
        });
        jobId = res.jobId;
      } else {
        if (!workspacePath.trim()) {
          throw new Error('Please provide an existing workspace directory path');
        }
        const res = await projectApi.importWorkspace(project.id, workspacePath.trim());
        jobId = res.jobId;
      }

      setActiveJobId(jobId);

      // Step 3: Listen to realtime SSE events
      subscribeJobProgress(
        jobId,
        (event) => {
          setJobEvents((prev) => [...prev, event]);
          setLatestProgress(event.progressPercent);
          setCurrentStep(event.currentStep);
          if (event.status === 'COMPLETED') {
            setIsSubmitting(false);
          } else if (event.status === 'FAILED') {
            setIsSubmitting(false);
            setErrorMsg(event.errorMessage || 'Ingestion task encountered an error');
          }
        },
        (err) => {
          console.error('SSE Error:', err);
        }
      );

    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMsg(err.response?.data?.message || err.message || 'Failed to initiate ingestion');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Project Ingestion Pipeline</h1>
        <p className="text-sm text-slate-400 mt-1">
          Securely upload a project archive, clone from GitHub, or analyze an authorized local workspace.
        </p>
      </div>

      <div className="bg-slate-900/70 border border-slate-800 rounded-xl overflow-hidden">
        {/* Method Selector Tabs */}
        <div className="grid grid-cols-3 border-b border-slate-800 text-xs font-semibold">
          <button
            type="button"
            onClick={() => setActiveTab('ZIP')}
            className={`py-3 px-4 flex items-center justify-center gap-2 border-r border-slate-800 transition-all ${
              activeTab === 'ZIP' ? 'bg-slate-800/80 text-emerald-400 border-b-2 border-b-emerald-500' : 'text-slate-400 hover:text-white'
            }`}
          >
            <UploadCloud className="h-4 w-4" />
            <span>ZIP Upload (Anti-Slip Safe)</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('GITHUB')}
            className={`py-3 px-4 flex items-center justify-center gap-2 border-r border-slate-800 transition-all ${
              activeTab === 'GITHUB' ? 'bg-slate-800/80 text-emerald-400 border-b-2 border-b-emerald-500' : 'text-slate-400 hover:text-white'
            }`}
          >
            <Github className="h-4 w-4" />
            <span>GitHub Repository Import</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('WORKSPACE')}
            className={`py-3 px-4 flex items-center justify-center gap-2 transition-all ${
              activeTab === 'WORKSPACE' ? 'bg-slate-800/80 text-emerald-400 border-b-2 border-b-emerald-500' : 'text-slate-400 hover:text-white'
            }`}
          >
            <FolderCode className="h-4 w-4" />
            <span>Local Workspace Import</span>
          </button>
        </div>

        {/* Input Form */}
        <form onSubmit={handleStartIngestion} className="p-6 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Project Name *</label>
              <input
                type="text"
                value={projectName}
                onChange={(e) => setProjectName(e.target.value)}
                placeholder="e.g. Payment Gateway Service"
                required
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Description</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief summary of service responsibilities"
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          {/* Tab 1: ZIP Upload Area */}
          {activeTab === 'ZIP' && (
            <div className="space-y-3">
              <label className="block text-xs font-medium text-slate-300">Project Archive File (.zip) *</label>
              <div
                className={`border-2 border-dashed rounded-xl p-8 text-center transition-all ${
                  selectedFile ? 'border-emerald-500/60 bg-emerald-500/5' : 'border-slate-700 hover:border-slate-600 bg-slate-950/50'
                }`}
              >
                <input
                  type="file"
                  id="zipInput"
                  accept=".zip"
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      setSelectedFile(e.target.files[0]);
                    }
                  }}
                  className="hidden"
                />
                <label htmlFor="zipInput" className="cursor-pointer space-y-2 block">
                  <div className="h-12 w-12 rounded-full bg-slate-800 mx-auto flex items-center justify-center text-slate-400">
                    <UploadCloud className="h-6 w-6 text-emerald-400" />
                  </div>
                  {selectedFile ? (
                    <div>
                      <p className="text-sm font-semibold text-emerald-400">{selectedFile.name}</p>
                      <p className="text-xs text-slate-400 mt-0.5">
                        {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB — Ready for secure extraction
                      </p>
                    </div>
                  ) : (
                    <div>
                      <p className="text-sm font-medium text-slate-300">
                        Click to select archive or drag and drop
                      </p>
                      <p className="text-xs text-slate-500 mt-1">
                        Zip Slip guarded • Archive Bomb protection (500MB max decompressed) • Excludes node_modules, target, .git
                      </p>
                    </div>
                  )}
                </label>
              </div>
            </div>
          )}

          {/* Tab 2: GitHub Form */}
          {activeTab === 'GITHUB' && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="md:col-span-2">
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">Repository URL *</label>
                  <input
                    type="url"
                    value={githubUrl}
                    onChange={(e) => setGithubUrl(e.target.value)}
                    placeholder="https://github.com/organization/repository"
                    required={activeTab === 'GITHUB'}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">Branch</label>
                  <input
                    type="text"
                    value={branch}
                    onChange={(e) => setBranch(e.target.value)}
                    placeholder="main"
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Personal Access Token (Optional for Private Repositories)
                </label>
                <input
                  type="password"
                  value={accessToken}
                  onChange={(e) => setAccessToken(e.target.value)}
                  placeholder="ghp_************************************"
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500 font-mono"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  Tokens are never logged or stored in plain text and are used strictly during checkout.
                </p>
              </div>
            </div>
          )}

          {/* Tab 3: Local Workspace */}
          {activeTab === 'WORKSPACE' && (
            <div className="space-y-3">
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Local Directory Path *</label>
              <input
                type="text"
                value={workspacePath}
                onChange={(e) => setWorkspacePath(e.target.value)}
                placeholder="e.g. D:\project\samples\legacy-app"
                required={activeTab === 'WORKSPACE'}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500 font-mono"
              />
              <p className="text-[11px] text-slate-500">
                Direct snapshot copy from host filesystem into sandboxed snapshot storage with path validation.
              </p>
            </div>
          )}

          {errorMsg && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-xs text-rose-400 flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white text-xs font-medium flex items-center gap-2 shadow-lg shadow-emerald-900/20"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Ingesting Project...</span>
                </>
              ) : (
                <>
                  <ShieldCheck className="h-4 w-4" />
                  <span>Start Secure Ingestion</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* Real-time SSE Execution Terminal */}
      {activeJobId && (
        <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden font-mono text-xs">
          <div className="bg-slate-900 px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2 text-slate-400">
              <Terminal className="h-4 w-4 text-emerald-400" />
              <span>Real-time Ingestion Stream ({activeJobId.substring(0, 8)})</span>
            </div>
            <span className="text-emerald-400 font-bold">{latestProgress}%</span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-slate-900 h-1.5">
            <div
              className="bg-emerald-500 h-1.5 transition-all duration-300"
              style={{ width: `${latestProgress}%` }}
            ></div>
          </div>

          {/* Terminal Logs */}
          <div className="p-4 space-y-1.5 max-h-60 overflow-y-auto text-slate-300">
            <p className="text-slate-500 text-[11px]">&gt; Connected to Server-Sent Events broker.</p>
            {jobEvents.map((evt, idx) => (
              <div key={idx} className="flex items-start gap-2">
                <span className="text-emerald-400">&gt;</span>
                <span>[{evt.status}] {evt.currentStep}</span>
              </div>
            ))}
          </div>

          {latestProgress === 100 && createdProjectId && (
            <div className="p-4 bg-emerald-950/20 border-t border-emerald-500/20 flex items-center justify-between">
              <div className="flex items-center gap-2 text-emerald-400">
                <CheckCircle2 className="h-4 w-4" />
                <span className="font-semibold text-xs">Ingestion Successfully Finalized</span>
              </div>
              <button
                type="button"
                onClick={() => navigate(`/projects/${createdProjectId}/intelligence`)}
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold flex items-center gap-1.5"
              >
                <span>Launch Intelligence Engine</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
