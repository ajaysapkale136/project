# AI Project Architect & Multi-Agent Development Platform

**AI Project Architect** is an enterprise-grade, autonomous software engineering platform designed to understand an existing software project before recommending or implementing changes. 

The platform deterministically parses uploaded source code (via ZIP archives, public/private GitHub repositories, or local workspaces), extracts the underlying domain architecture and business rules with line-level AST provenance, animates the architecture using the official isometric **Anatomy** engine, conducts multi-model consensus architecture reviews across **OpenAI GPT-4o, Google Gemini 1.5 Pro, and Anthropic Claude 3.5 Sonnet**, safely generates isolated worktree patches with fine-grained file-level accept/reject controls, preserves business rules with full regression impact reporting, executes multi-stack tests (Maven, Jest, Pytest) in an isolated sandbox, hardens against prompt injections, dependency CVEs and secret leaks, and delivers atomic rollbacks and unified `.patch` exports.

---

## 1. System Architecture Overview

```
d:\project
├── backend/                             # Java 21 LTS / Spring Boot 3.3.5 Modular Monolith
│   ├── src/main/java/com/aiproject/architect/
│   │   ├── common/                      # 4-State Provenance Model, ApiResponse envelope, Global Exceptions
│   │   ├── config/                      # Stateless Security, Permissive CORS, X-Correlation-ID Filter, Actuator
│   │   ├── modules/
│   │   │   ├── project/                 # Project CRUD, Snapshot lifecycle, Status coordinator
│   │   │   ├── ingestion/               # Safe ZIP extraction, Anti-ZIP-Slip, GitHub clone, SHA-256 deduplication
│   │   │   ├── intelligence/            # JavaParser AST, TypeScript/React AST, SQL schema parser, Business rules
│   │   │   ├── architect/               # Dual-view architecture synthesizer, Anti-pattern detector, Diff engine
│   │   │   ├── ai/                      # Multi-AI Intelligence Engine (OpenAI, Gemini, Claude, Auto-routing, Consensus)
│   │   │   ├── anatomy/                 # Official Anatomy integration (3-format isometric animation & export engine)
│   │   │   ├── assistant/               # AI Development Assistant (Module code generator, Refactorer, LCS Diff Engine)
│   │   │   ├── patch/                   # Isolated Worktrees, ChangeSets, Staleness Detection, Atomic Rollback, Patch Export
│   │   │   ├── testing/                 # Multi-Stack Sandboxed Testing Engine (Maven, Jest, Pytest) with Timeout Guardrails
│   │   │   ├── security/                # Static Security Scanner, Hardcoded Secrets, API Contract Verifier, Sandbox
│   │   │   │   └── hardening/           # Prompt Sanitizer, Dependency CVE Scanner, AES-256-GCM Encryption, Audit Log
│   │   │   └── jobs/                    # Asynchronous Job Execution & Server-Sent Events (SSE) broadcaster
│   │   └── persistence/                 # Spring Data JPA repositories & Flyway database migrations
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   │   ├── V1__initial_schema.sql                   # Baseline project, AST intelligence & architecture schema
│   │   │   ├── V2__multi_ai_and_animation_schema.sql    # Multi-AI traces, Consensus reviews & Anatomy animation scenes
│   │   │   ├── V3__assistant_and_sandbox_schema.sql     # Code proposals, Diffs, Unit tests & Security findings
│   │   │   └── V4__patch_worktree_and_audit_schema.sql  # Isolated worktrees, ChangeSets, Test executions, Audit events
│   │   ├── application.yml              # Base Spring Boot configuration (Actuator, Logging, Correlation ID)
│   │   ├── application-dev.yml          # Local dev profile (H2 in PostgreSQL mode, Flyway enabled)
│   │   ├── application-prod.yml         # Production profile (PostgreSQL 16+, Redis)
│   │   └── application-test.yml         # Integration test profile
│   ├── Dockerfile                       # Multi-stage production build (Eclipse Temurin JDK 21 Alpine)
│   └── pom.xml                          # Maven build definition (Java 21 LTS, Spring Boot 3.3.5)
│
├── frontend/                            # React 18 / TypeScript / Vite / Tailwind CSS / React Flow
│   ├── src/
│   │   ├── anatomy/                     # Official Anatomy engine integration (iso-kit, draw, live canvas, playback bar)
│   │   ├── api/client.ts                # REST API client with X-Correlation-ID interceptor & SSE real-time stream listener
│   │   ├── layouts/DashboardLayout.tsx  # Responsive navigation sidebar & active project switcher
│   │   ├── pages/
│   │   │   ├── Dashboard.tsx            # KPI metrics, managed project cards, quick actions
│   │   │   ├── ImportProject.tsx        # Drag & Drop ZIP upload, GitHub import, SSE execution terminal
│   │   │   ├── IntelligenceExplorer.tsx # AST component table, detected technologies, endpoint statistics
│   │   │   ├── BusinessLogicExplorer.tsx# Business rules with verified line-level source code evidence
│   │   │   ├── ArchitectureView.tsx     # React Flow interactive graph (Existing vs Proposed)
│   │   │   ├── ArchitectureCompare.tsx  # Side-by-side diff, added capabilities, resolved smells
│   │   │   ├── AnatomyVisualizerPage.tsx# 3-Format Isometric Engineering Visualizer (Assemblies, Scrubber, Exports)
│   │   │   ├── MultiAiSettings.tsx      # Provider health status cards, Multi-Model Review, Consensus & Disputes
│   │   │   ├── ModuleBuilder.tsx        # Module blueprint & Antigravity implementation prompt generator
│   │   │   ├── CodeAssistantPage.tsx    # Interactive Module Implementation, Code Diff Viewer, Test Generator
│   │   │   ├── PatchWorktreePage.tsx    # Safe Development Assistant (Worktrees, Staleness, Accept/Reject Diffs, Tests)
│   │   │   └── SecuritySandboxPage.tsx  # Static Vulnerability Audit, Secrets Detection, API Contracts, Sandbox Runner
│   │   └── types/index.ts               # Complete TypeScript data contracts
│   ├── Dockerfile                       # Multi-stage production build (Node 20 Alpine -> Nginx Alpine SPA)
│   ├── package.json
│   └── vite.config.ts
│
├── third_party/
│   └── anatomy/                         # Cloned repository of official Anatomy isometric engine
├── .github/
│   └── workflows/
│       └── ci.yml                       # GitHub Actions CI (JDK 21, Maven test, Node 20, npm build, Docker verification)
├── docker-compose.yml                   # Production orchestration (PostgreSQL 16, Redis 7, Backend, Frontend)
├── .env.example                         # Environment configuration template (zero hardcoded secrets)
└── README.md
```

---

## 2. Core Functional Modules

### Module 01 — Project Ingestion Engine
- **ZIP Upload:** Secure multi-part upload with anti-ZIP-slip directory containment checks (`destination.startsWith(canonicalBase)`).
- **Archive Bomb Protection:** Enforces maximum limits (500 MB uncompressed, 50,000 files, 100:1 compression ratio).
- **GitHub Import:** Clones public or authorized private Git repositories safely, recording commit hashes and branches.
- **Deduplication:** Computes SHA-256 cryptographic checksums for every cataloged file. Excludes build noise (`target/`, `node_modules/`, `.git/`, `.idea/`).
- **Real-time SSE:** Streams ingestion and cataloging progress to the frontend terminal via Server-Sent Events (`/api/v1/jobs/{jobId}/events`).

### Module 02 — Project Intelligence Engine
- **Deterministic AST Parsing:** Employs `JavaParser` for Java/Spring Boot code and regex AST tokenizers for TypeScript/React and SQL schemas.
- **Technology Stack Detection:** Automatic detection of Maven, Gradle, Spring Boot, Spring Security, Hibernate, PostgreSQL, React, Vite, Tailwind CSS, Jest, and JUnit.
- **Business Rule Extractor:** Extracts critical validation, authentication, and transactional rules directly from code annotations and method bodies, linking each rule to verified `SourceEvidence` with line ranges and syntax snippets.
- **4-State Confidence Provenance:** Every discovered element is labeled `VERIFIED` (syntactic proof), `INFERRED` (heuristic evidence), `UNVERIFIED` (hypothesized), or `CONFLICTING` (contract mismatch).

### Module 03 — AI Project Architect
- **Dual-View Architecture Generation:** Synthesizes the project's **Existing Architecture** (layered topology, discovered dependencies, bottlenecks) and generates a **Proposed Domain-Modular Architecture**.
- **Anti-Pattern & Smells Detection:** Flags monolithic God classes, circular dependencies, missing service layers, and unvalidated endpoints.
- **Architecture Diff Engine:** Computes structural deltas (added nodes, removed anti-patterns, resolved architectural smells, risk scores).
- **Antigravity Blueprint Export:** Generates structured implementation blueprints and prompts tailored for autonomous AI agents.

### Module 04 — Multi-AI Intelligence Engine
- **Provider Abstraction:** Unified `AIProvider` SPI supporting:
  - **OpenAI:** GPT-4o, GPT-4o-mini, o1.
  - **Google Gemini:** Gemini 1.5 Pro, Gemini 1.5 Flash, Gemini 2.0 Flash.
  - **Anthropic Claude:** Claude 3.5 Sonnet, Claude 3.5 Haiku.
- **3 Operating Modes:**
  1. *Single Provider Mode:* User-selected LLM provider with custom parameters.
  2. *Automatic Routing Mode:* Auto-selects model based on task complexity, token capacity, and latency requirements.
  3. *Multi-Model Review Mode:* One primary model synthesizes an architectural proposal; two independent secondary models review it. The engine generates a consensus matrix identifying agreements, disagreements, unsupported claims, and flags disputes without silent merging.
- **Trace Tracking:** Captures token counts, execution duration, and USD cost estimation for every AI invocation.

### Module 05 & 08 — Official Anatomy Integration & Animation Engine
Integrated directly with the official [Anatomy](https://github.com/wheresryan22/anatomy) isometric engine:
- **Format 1 — Module-to-Module Rig:** Physical machine representation on a slotted bedplate with extruded chassis, data bus rails, status LEDs, and pneumatic actuators.
- **Format 2 — Internal Exploded Component View:** Vertical Z-offset exploded assembly showing Controllers, Services, Repositories, Entities, and Database with interactive 0–100% separation slider.
- **Format 3 — End-to-End Execution Trace:** 8-phase isometric animation tracing HTTP Request -> Security Filter -> Dispatcher -> Controller -> Business Service -> Repository -> Database Engine -> JSON Response.
- **Multi-Format Export:** Export scenes directly as SVG, standalone self-contained HTML (zero external runtime dependencies), PDF, or MP4 metadata.

### Module 06 — Safe AI Development Assistant & Patch Engine
- **Target Selection:** Supports targeted generation for `MODULE`, `COMPONENT`, `BUG`, `FEATURE`, `REFACTOR`, and `API_OPERATION`.
- **Multi-Stack Dynamic Adaptation:** Generates code adhering to the target project's actual language and conventions (Java Spring Boot, TypeScript/React, Python, etc.).
- **Isolated Worktrees:** Changes are produced in an isolated working copy (`storage/worktrees/{workspaceId}`) rather than directly mutating the original project source.
- **Fine-Grained Diffs:** Character-accurate LCS unified diffs (`--- a/file`, `+++ b/file`, `@@ -1,x +1,y @@`) with individual file `ACCEPT` / `REJECT` decision toggles.
- **Staleness Conflict Guardrail:** Detects when the underlying project snapshot has mutated since change set generation (`isWorkspaceStale`), rejecting stale proposals with HTTP 409 Conflict.
- **Atomic Rollback:** Reverses applied changes cleanly using stored pre-modification snapshots.
- **Unified Patch Export:** Exports change sets as unified `.patch` / `.diff` files and downloads complete worktree ZIP packages.

### Module 07 — Sandboxed Testing & Security Hardening
- **Multi-Stack Sandboxed Testing Engine:**
  - Auto-detects project build tooling: Maven (`pom.xml`), Node/Jest (`package.json`), or Python/Pytest (`requirements.txt`).
  - Restricts execution strictly inside the isolated worktree directory.
  - Bounded execution timeouts (default 60s) to prevent infinite loops or hung processes.
  - Captures genuine console logs, test counts (`passed`, `failed`, `skipped`), and duration without faking results.
- **Security Hardening Subsystem:**
  - **Prompt Injection Sanitizer:** Detects jailbreak signatures, system prompt override attempts, and dangerous tags (`ignore previous instructions`, `system prompt override`, `<script>`).
  - **Dependency CVE Scanner:** Inspects build files against known vulnerability databases (e.g., Log4Shell CVE-2021-44228, Spring4Shell CVE-2022-22965, Lodash prototype pollution CVE-2019-10744).
  - **AES-256-GCM Credential Encryption:** Cryptographically secures third-party credentials and API tokens at rest using authenticated symmetric encryption.
  - **Security Audit Log:** Immutable event log tracking approvals, rejections, patch applications, rollbacks, and security scans with correlation IDs.
- **Production Observability:**
  - `CorrelationIdFilter`: Injects `X-Correlation-ID` header into all requests, propagates across SLF4J MDC, and coordinates with the frontend axios interceptor.
  - Spring Boot Actuator: Exposes `/actuator/health`, `/actuator/info`, and `/actuator/metrics` for container health checks and Prometheus metrics collection.

---

## 3. Database Schema (Flyway Migrations)

| Migration | Version | Description | Key Tables |
|---|---|---|---|
| `V1__initial_schema.sql` | v1 | Project foundation & AST intelligence | `projects`, `source_snapshots`, `project_files`, `project_analyses`, `detected_technologies`, `analysis_modules`, `analysis_components`, `module_dependencies`, `business_rules`, `source_evidence`, `architecture_versions`, `architecture_nodes`, `architecture_edges`, `generation_jobs`, `audit_logs` |
| `V2__multi_ai_and_animation_schema.sql` | v2 | Multi-AI traces & Anatomy scenes | `ai_provider_configs`, `ai_interaction_traces`, `multi_model_reviews`, `animation_scenes` |
| `V3__assistant_and_sandbox_schema.sql` | v3 | Code assistant & Security sandbox | `code_proposals`, `code_changes`, `generated_tests`, `security_scans`, `vulnerability_findings`, `contract_verifications` |
| `V4__patch_worktree_and_audit_schema.sql` | v4 | Isolated worktrees, ChangeSets, Sandboxed tests & Security audit | `isolated_workspaces`, `change_sets`, `change_set_files`, `test_executions`, `security_audit_events` |

---

## 4. Complete API Reference

### Projects & Ingestion
- `GET /api/v1/projects` — List all projects
- `POST /api/v1/projects` — Create project record
- `GET /api/v1/projects/{projectId}` — Get project metadata
- `POST /api/v1/projects/{projectId}/ingest/zip` — Secure ZIP upload
- `POST /api/v1/projects/{projectId}/ingest/github` — GitHub repository clone
- `POST /api/v1/projects/{projectId}/ingest/workspace` — Authorized workspace import
- `GET /api/v1/jobs/{jobId}/events` — SSE progress stream

### AST Intelligence & Architecture
- `POST /api/v1/projects/{projectId}/analyze` — Trigger AST code analysis
- `GET /api/v1/projects/{projectId}/intelligence` — Get full intelligence overview
- `GET /api/v1/projects/{projectId}/rules` — Get extracted business rules with evidence
- `GET /api/v1/projects/{projectId}/architecture/existing` — Existing topology graph
- `GET /api/v1/projects/{projectId}/architecture/proposed` — Proposed modular graph
- `GET /api/v1/projects/{projectId}/architecture/compare` — Structural diff scorecard
- `GET /api/v1/projects/{projectId}/architecture/blueprint` — Module specification

### Multi-AI & Anatomy Animation
- `GET /api/v1/ai/providers` — List AI provider health status
- `POST /api/v1/projects/{projectId}/ai/review` — Multi-model architecture consensus review
- `GET /api/v1/projects/{projectId}/anatomy/scenes/modules` — Format 1: Module-to-Module Rig
- `GET /api/v1/projects/{projectId}/anatomy/scenes/exploded` — Format 2: Exploded Component View
- `GET /api/v1/projects/{projectId}/anatomy/scenes/execution` — Format 3: Execution Trace
- `POST /api/v1/projects/{projectId}/anatomy/export` — Export scene as HTML, SVG, PDF, MP4

### AI Development Assistant & Isolated Worktree Patches
- `POST /api/v1/projects/{projectId}/assistant/proposals` — Generate domain module proposal
- `POST /api/v1/projects/{projectId}/assistant/refactor` — Refactor component preserving rules
- `POST /api/v1/projects/{projectId}/worktrees` — Create isolated worktree for snapshot
- `GET /api/v1/projects/{projectId}/worktrees` — Get active isolated worktrees
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets` — Generate targeted patch change set
- `GET /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets` — List change sets
- `GET /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}` — Get change set with unified diffs
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/approve` — Approve change set
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/reject` — Reject change set
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/apply` — Apply change set to worktree
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/rollback` — Atomic rollback of applied changes
- `PUT /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/files/{fileId}/decision` — Update individual file decision (`ACCEPTED` / `REJECTED`)
- `GET /api/v1/projects/{projectId}/worktrees/{workspaceId}/changesets/{changeSetId}/export` — Download unified `.patch` text file
- `GET /api/v1/projects/{projectId}/worktrees/{workspaceId}/download` — Download complete worktree ZIP archive

### Sandboxed Testing Engine
- `POST /api/v1/projects/{projectId}/worktrees/{workspaceId}/tests/run` — Run multi-stack tests in isolated sandbox
- `GET /api/v1/projects/{projectId}/worktrees/{workspaceId}/tests/latest` — Get latest test execution report

### Security Hardening & Observability
- `POST /api/v1/projects/{projectId}/security/scan` — Run static security & secrets scan
- `GET /api/v1/projects/{projectId}/security/scans/latest` — Get latest security audit findings
- `POST /api/v1/projects/{projectId}/security/contracts/verify` — Verify API contracts
- `GET /api/v1/projects/{projectId}/security/contracts` — Get contract compliance matrix
- `GET /api/v1/audit/events` — Query immutable security audit log with correlation IDs
- `GET /actuator/health` — Spring Boot Actuator health check (Liveness / Readiness)
- `GET /actuator/info` — Application build and environment info
- `GET /actuator/metrics` — JVM and HTTP performance metrics

---

## 5. Automated Verification & Test Results

The platform is backed by **35 automated unit, integration, and security tests** across 10 test suites, achieving a **100% pass rate**:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aiproject.architect.AiProjectArchitectIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 (E2E Ingestion, AST Intelligence & Architecture Pipeline)
[INFO] Running com.aiproject.architect.AnatomyAnimationEngineTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 (Format 1 Rig, Format 2 Exploded Assembly, Format 3 Execution Trace)
[INFO] Running com.aiproject.architect.CodeAssistantTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 (LCS DiffEngine, Business Rule Preservation, Approval Guardrails)
[INFO] Running com.aiproject.architect.MultiAiIntelligenceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 (Multi-Provider Registration, Auto-Routing & Cost Estimation)
[INFO] Running com.aiproject.architect.SandboxedTestingEngineTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (Maven/Java & Node/Jest Sandboxed Test Runner)
[INFO] Running com.aiproject.architect.SecurityHardeningTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 (Prompt Sanitizer, Dependency Scanner, AES-GCM Encryption)
[INFO] Running com.aiproject.architect.SecuritySandboxTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 (AWS Keys, SQL Injection, Path Traversal, Sandbox Isolation)
[INFO] Running com.aiproject.architect.TechnologyDetectorTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 (Spring Boot/PostgreSQL/React Stack Detection)
[INFO] Running com.aiproject.architect.WorktreeAndPatchTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 (Isolated Worktree, Staleness Conflict Rejection, Apply & Rollback)
[INFO] Running com.aiproject.architect.ZipSecurityValidatorTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 (Anti-ZIP-Slip, Bomb Limits, Malicious File Filtering)
[INFO] 
[INFO] Results:
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 6. Getting Started

### Prerequisites
- **Java:** JDK 21 LTS (`java -version`)
- **Maven:** 3.9+ (`mvn -version`)
- **Node.js:** v20+ (`node -v`)
- **Docker:** (Optional) Docker 24+ & Docker Compose for containerized deployment

### Option A: Local Development Setup

#### 1. Configuration
Copy `.env.example` to `.env`:
```bash
copy .env.example .env
```
*(Optional)* Add your `OPENAI_API_KEY`, `GEMINI_API_KEY`, or `ANTHROPIC_API_KEY` to enable live multi-model review. (The platform operates seamlessly in offline/fallback mode if keys are omitted).

#### 2. Run Backend Tests & Verify Build
```bash
cd d:\project\backend
& "d:\tools\apache-maven-3.9.6\bin\mvn.cmd" test
```

#### 3. Start Backend Server
```bash
cd d:\project\backend
& "d:\tools\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
```
Backend will start on `http://localhost:8080`.

#### 4. Build & Start Frontend UI
```bash
cd d:\project\frontend
npm run build
npm run dev
```
Open your browser at `http://localhost:5173`.

---

### Option B: Docker Compose Deployment

Run the complete production stack (PostgreSQL 16, Redis 7, Spring Boot Backend, Nginx Frontend) with one command:

```bash
docker-compose up --build -d
```

- **Frontend Application:** `http://localhost:3000`
- **Backend API:** `http://localhost:8080/api`
- **Actuator Health:** `http://localhost:8080/actuator/health`

---

## 7. Continuous Integration (CI/CD)

The project includes an enterprise GitHub Actions CI workflow (`.github/workflows/ci.yml`) providing:
1. **Backend CI:** Runs JDK 21 setup with PostgreSQL and Redis testcontainers, executing all 35 unit, integration, and security tests.
2. **Frontend CI:** Sets up Node.js 20, installs dependencies via `npm ci`, runs TypeScript type checking, and builds production bundles.
3. **Container Build Validation:** Builds Docker images for both backend and frontend using Docker Buildx to ensure containerization integrity.
