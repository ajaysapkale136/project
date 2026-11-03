import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import {
  Layers,
  UploadCloud,
  FileCode,
  ShieldCheck,
  Cpu,
  ArrowRight,
  GitBranch,
  Boxes,
  Database
} from 'lucide-react';
import { projectApi } from '../api/client';
import { Project } from '../types';

export const Dashboard: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    projectApi.getProjects()
      .then((data) => {
        setProjects(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const totalFiles = projects.reduce((acc, p) => acc + (p.fileCount || 0), 0);
  const totalEndpoints = projects.reduce((acc, p) => acc + (p.totalEndpoints || 0), 0);
  const analyzedCount = projects.filter((p) => p.status === 'ANALYZED').length;

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Hero Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Engineering Architecture Console</h1>
          <p className="text-sm text-slate-400 mt-1">
            Analyze existing codebases, extract business logic, verify contracts, and synthesize clean target architectures.
          </p>
        </div>
        <NavLink
          to="/import"
          className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg font-medium text-xs shadow-lg shadow-emerald-900/30 transition-all"
        >
          <UploadCloud className="h-4 w-4" />
          <span>Upload Project Archive</span>
        </NavLink>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-400 font-medium">Total Projects</p>
            <p className="text-2xl font-bold text-white mt-1">{projects.length}</p>
          </div>
          <div className="h-10 w-10 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
            <Layers className="h-5 w-5" />
          </div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-400 font-medium">Analyzed Codebases</p>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{analyzedCount}</p>
          </div>
          <div className="h-10 w-10 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Cpu className="h-5 w-5" />
          </div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-400 font-medium">Cataloged Files</p>
            <p className="text-2xl font-bold text-white mt-1">{totalFiles.toLocaleString()}</p>
          </div>
          <div className="h-10 w-10 rounded-lg bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
            <FileCode className="h-5 w-5" />
          </div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-400 font-medium">Discovered Endpoints</p>
            <p className="text-2xl font-bold text-amber-400 mt-1">{totalEndpoints}</p>
          </div>
          <div className="h-10 w-10 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
            <ShieldCheck className="h-5 w-5" />
          </div>
        </div>
      </div>

      {/* Projects List */}
      <div className="space-y-4">
        <h2 className="text-base font-semibold text-white">Managed Projects</h2>

        {loading ? (
          <div className="p-12 text-center text-slate-500">Loading projects...</div>
        ) : projects.length === 0 ? (
          <div className="p-12 border border-dashed border-slate-800 rounded-xl text-center space-y-3">
            <div className="h-12 w-12 rounded-full bg-slate-800/80 mx-auto flex items-center justify-center text-slate-400">
              <UploadCloud className="h-6 w-6" />
            </div>
            <h3 className="text-sm font-semibold text-white">No projects ingested yet</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Upload a ZIP archive of an existing codebase or import a GitHub repository to begin deep architectural intelligence.
            </p>
            <NavLink
              to="/import"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium rounded-lg"
            >
              <span>Get Started</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </NavLink>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map((p) => (
              <div
                key={p.id}
                className="p-5 rounded-xl bg-slate-900/60 border border-slate-800 hover:border-slate-700 transition-all space-y-4 flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-mono text-[10px] uppercase px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                      {p.status}
                    </span>
                    <span className="text-[11px] text-slate-500">
                      {new Date(p.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <h3 className="text-base font-semibold text-white tracking-tight">{p.name}</h3>
                  <p className="text-xs text-slate-400 line-clamp-2 mt-1">
                    {p.description || 'No description provided.'}
                  </p>
                </div>

                <div className="space-y-3 pt-3 border-t border-slate-800/60">
                  <div className="grid grid-cols-2 gap-2 text-xs">
                    <div className="text-slate-400">
                      Files: <span className="text-white font-mono">{p.fileCount || 0}</span>
                    </div>
                    <div className="text-slate-400">
                      Branch: <span className="text-white font-mono">{p.defaultBranch || 'main'}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 pt-2">
                    <NavLink
                      to={`/projects/${p.id}/intelligence`}
                      className="flex-1 text-center py-1.5 px-2 bg-slate-800 hover:bg-slate-700 text-white rounded text-xs font-medium transition-all"
                    >
                      Intelligence
                    </NavLink>
                    <NavLink
                      to={`/projects/${p.id}/architecture/existing`}
                      className="flex-1 text-center py-1.5 px-2 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/30 rounded text-xs font-medium transition-all"
                    >
                      Architecture
                    </NavLink>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
