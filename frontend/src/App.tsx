import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AiEngineeringStudio } from './pages/AiEngineeringStudio';
import { ImportProject } from './pages/ImportProject';
import { DashboardLayout } from './layouts/DashboardLayout';
import { IntelligenceExplorer } from './pages/IntelligenceExplorer';
import { BusinessLogicExplorer } from './pages/BusinessLogicExplorer';
import { ArchitectureView } from './pages/ArchitectureView';
import { ArchitectureCompare } from './pages/ArchitectureCompare';
import { ModuleBuilder } from './pages/ModuleBuilder';
import { AnatomyVisualizerPage } from './pages/AnatomyVisualizerPage';
import { MultiAiSettings } from './pages/MultiAiSettings';
import { CodeAssistantPage } from './pages/CodeAssistantPage';
import { SecuritySandboxPage } from './pages/SecuritySandboxPage';
import { PatchWorktreePage } from './pages/PatchWorktreePage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Primary AI Engineering Studio: Single Main Screen */}
        <Route path="/" element={<AiEngineeringStudio />} />
        <Route path="/studio" element={<AiEngineeringStudio />} />
        <Route path="/studio/:projectId" element={<AiEngineeringStudio />} />
        <Route path="/import" element={<ImportProject />} />

        {/* Deep link routes wrapped in DashboardLayout if needed */}
        <Route path="/projects/:projectId" element={<DashboardLayout />}>
          <Route path="studio" element={<AiEngineeringStudio />} />
          <Route path="intelligence" element={<IntelligenceExplorer />} />
          <Route path="rules" element={<BusinessLogicExplorer />} />
          <Route path="architecture/existing" element={<ArchitectureView mode="EXISTING" />} />
          <Route path="architecture/proposed" element={<ArchitectureView mode="PROPOSED" />} />
          <Route path="architecture/compare" element={<ArchitectureCompare />} />
          <Route path="anatomy" element={<AnatomyVisualizerPage />} />
          <Route path="ai" element={<MultiAiSettings />} />
          <Route path="modules" element={<ModuleBuilder />} />
          <Route path="worktree" element={<PatchWorktreePage />} />
          <Route path="assistant" element={<CodeAssistantPage />} />
          <Route path="security" element={<SecuritySandboxPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
