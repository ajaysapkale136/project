import React, { useState, useEffect } from 'react';
import { Outlet, NavLink, useParams, useNavigate } from 'react-router-dom';
import {
  Layers,
  UploadCloud,
  Cpu,
  ShieldCheck,
  GitBranch,
  Boxes,
  Compass,
  Settings,
  FolderGit2,
  ChevronRight,
  Eye,
  Bot,
  Code,
  ShieldAlert
} from 'lucide-react';
import { projectApi } from '../api/client';
import { Project } from '../types';

export const DashboardLayout: React.FC = () => {
  const { projectId } = useParams<{ projectId?: string }>();
  const navigate = useNavigate();
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);

  useEffect(() => {
    projectApi.getProjects().then((data) => {
      setProjects(data);
      if (data.length > 0) {
        if (projectId) {
          const match = data.find((p) => p.id === projectId);
          setSelectedProject(match || data[0]);
        } else {
          setSelectedProject(data[0]);
        }
      }
    }).catch(console.error);
  }, [projectId]);

  const currentProjectId = selectedProject ? selectedProject.id : (projects[0]?.id || '');

  const navItems = [
    { name: 'Dashboard', path: '/', icon: Layers },
    { name: 'Import Project', path: '/import', icon: UploadCloud },
    {
      name: 'Project Intelligence',
      path: currentProjectId ? `/projects/${currentProjectId}/intelligence` : '#',
      icon: Cpu,
      disabled: !currentProjectId
    },
    {
      name: 'Business Rules',
      path: currentProjectId ? `/projects/${currentProjectId}/rules` : '#',
      icon: ShieldCheck,
      disabled: !currentProjectId
    },
    {
      name: 'Existing Architecture',
      path: currentProjectId ? `/projects/${currentProjectId}/architecture/existing` : '#',
      icon: Compass,
      disabled: !currentProjectId
    },
    {
      name: 'Proposed Architecture',
      path: currentProjectId ? `/projects/${currentProjectId}/architecture/proposed` : '#',
      icon: Boxes,
      disabled: !currentProjectId
    },
    {
      name: 'Architecture Diff',
      path: currentProjectId ? `/projects/${currentProjectId}/architecture/compare` : '#',
      icon: GitBranch,
      disabled: !currentProjectId
    },
    {
      name: 'Anatomy Visualizer',
      path: currentProjectId ? `/projects/${currentProjectId}/anatomy` : '#',
      icon: Eye,
      disabled: !currentProjectId
    },
    {
      name: 'Multi-AI Intelligence',
      path: currentProjectId ? `/projects/${currentProjectId}/ai` : '#',
      icon: Bot,
      disabled: !currentProjectId
    },
    {
      name: 'Module Builder',
      path: currentProjectId ? `/projects/${currentProjectId}/modules` : '#',
      icon: FolderGit2,
      disabled: !currentProjectId
    },
    {
      name: 'Worktree & Patches',
      path: currentProjectId ? `/projects/${currentProjectId}/worktree` : '#',
      icon: GitBranch,
      disabled: !currentProjectId
    },
    {
      name: 'AI Code Assistant',
      path: currentProjectId ? `/projects/${currentProjectId}/assistant` : '#',
      icon: Code,
      disabled: !currentProjectId
    },
    {
      name: 'Security & Sandbox',
      path: currentProjectId ? `/projects/${currentProjectId}/security` : '#',
      icon: ShieldAlert,
      disabled: !currentProjectId
    }
  ];

  return (
    <div className="flex h-screen bg-slate-950 text-slate-100 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 border-r border-slate-800 bg-slate-900/60 backdrop-blur-md flex flex-col justify-between p-4">
        <div>
          {/* Logo */}
          <div className="flex items-center gap-3 px-2 py-3 border-b border-slate-800 mb-4">
            <div className="h-9 w-9 rounded-lg bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 font-bold">
              AI
            </div>
            <div>
              <h1 className="font-bold text-sm tracking-wide text-white">Project Architect</h1>
              <p className="text-[10px] text-emerald-400 font-mono tracking-widest uppercase">Multi-AI & Anatomy</p>
            </div>
          </div>

          {/* Project Switcher */}
          {projects.length > 0 && (
            <div className="mb-4 px-1">
              <label className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1 block">
                Active Project
              </label>
              <select
                value={selectedProject?.id || ''}
                onChange={(e) => {
                  const p = projects.find((x) => x.id === e.target.value);
                  if (p) {
                    setSelectedProject(p);
                    navigate(`/projects/${p.id}/intelligence`);
                  }
                }}
                className="w-full bg-slate-800 border border-slate-700 rounded-md px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-emerald-500"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Navigation Items */}
          <nav className="space-y-1 overflow-y-auto max-h-[calc(100vh-17rem)]">
            {navItems.map((item) => (
              <NavLink
                key={item.name}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                    item.disabled ? 'opacity-40 pointer-events-none' : ''
                  } ${
                    isActive
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`
                }
              >
                <div className="flex items-center gap-2.5">
                  <item.icon className="h-4 w-4" />
                  <span>{item.name}</span>
                </div>
                <ChevronRight className="h-3 w-3 opacity-30" />
              </NavLink>
            ))}
          </nav>
        </div>

        {/* Footer info */}
        <div className="border-t border-slate-800 pt-3 px-2">
          <div className="flex items-center justify-between text-[11px] text-slate-400">
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Core Backend Online
            </span>
            <span className="font-mono text-[10px] text-slate-500">Port 8080</span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col overflow-y-auto">
        <header className="h-14 border-b border-slate-800 bg-slate-900/40 backdrop-blur-sm px-6 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <h2 className="text-sm font-semibold text-slate-200">
              {selectedProject ? selectedProject.name : 'AI Project Architect Workspace'}
            </h2>
            {selectedProject && (
              <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                {selectedProject.status}
              </span>
            )}
          </div>
          <div className="flex items-center gap-3 text-xs text-slate-400">
            {selectedProject?.primaryLanguage && (
              <span className="px-2 py-1 bg-slate-800 rounded border border-slate-700">
                {selectedProject.primaryLanguage}
              </span>
            )}
            <NavLink
              to="/import"
              className="px-3 py-1.5 rounded-md bg-emerald-600 hover:bg-emerald-500 text-white font-medium flex items-center gap-1.5 transition-all"
            >
              <UploadCloud className="h-3.5 w-3.5" />
              <span>Import Project</span>
            </NavLink>
          </div>
        </header>

        <div className="p-6 flex-1">
          <Outlet />
        </div>
      </main>
    </div>
  );
};
