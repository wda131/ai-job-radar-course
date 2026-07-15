# Classmate Run Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a complete Windows run guide so a classmate can clone the public repository, start the course project in IDEA, and optionally import real BOSS jobs through the Chrome extension.

**Architecture:** Keep README concise and place the full handoff procedure in a dedicated runbook. Document Docker as the recommended infrastructure path and native services as the alternative; keep the BOSS extension explicitly optional and isolated from the core startup path.

**Tech Stack:** Markdown, PowerShell, Git, IDEA, Maven, Docker Compose, Chrome extension

---

### Task 1: Add the full classmate runbook

**Files:**
- Create: `docs/CLASSMATE_RUN_GUIDE.md`

- [x] **Step 1: Document prerequisites and clone commands**

List Windows 10/11, Git, JDK 17, Maven 3.8+, Node.js 24, IDEA, and either Docker Desktop or native MySQL/Redis/Nacos/RabbitMQ. State that Ollama, Elasticsearch, and the Chrome extension are optional for the core project.

- [x] **Step 2: Document the recommended Docker infrastructure path**

Use `scripts/start-infrastructure.ps1`, `docker compose ps`, the MySQL initialization volume behavior, and `scripts/check-environment.ps1`. Explain that existing Docker volumes may require a reset or explicit SQL import.

- [x] **Step 3: Document the native infrastructure path**

Include `NACOS_HOME`, `ERLANG_HOME`, and `RABBITMQ_SERVER`; include the database initialization command using `sql/ai_job_radar.sql`; include native startup scripts and port checks.

- [x] **Step 4: Document IDEA and frontend startup**

List all eight Java main classes and ports, then use `scripts/start-frontend.ps1`. Include the `student / 123456` demonstration account and `node scripts/smoke-test.mjs` verification.

- [x] **Step 5: Document optional BOSS import**

Include unpacked extension installation, bridge environment variables, manual detection/import flow, 50-job limit, deduplication, and the fact that no BOSS cookie or credential is exported.

- [x] **Step 6: Add troubleshooting and a handoff checklist**

Cover port conflicts, database authentication, missing Nacos/RabbitMQ environment variables, frontend install failures, bridge connection failures, zero detected BOSS jobs, Ollama fallback, and the final list of URLs/ports to verify.

### Task 2: Make README point new users to the correct path

**Files:**
- Modify: `README.md`

- [x] **Step 1: Add a prominent classmate handoff section near the top**

Link `docs/CLASSMATE_RUN_GUIDE.md`, state that Docker infrastructure plus IDEA is recommended, and state that Chrome extension installation is optional.

- [x] **Step 2: Add the shortest successful sequence**

Show clone, infrastructure, IDEA, frontend, login, and smoke-test steps without duplicating the whole runbook.

### Task 3: Verify and publish

**Files:**
- Verify: `README.md`
- Verify: `docs/CLASSMATE_RUN_GUIDE.md`

- [x] **Step 1: Validate referenced repository paths**

Run a PowerShell path checklist for every script, SQL file, extension folder, POM, and source main class mentioned in the guide. Expected result: every entry prints `OK`.

- [x] **Step 2: Scan documentation quality**

Search for `TODO`, `TBD`, developer-specific absolute paths, and missing required headings. Expected result: no placeholders or `C:\\Users\\Administrator` paths in the delivered guide.

- [ ] **Step 3: Commit only the guide files**

```powershell
git add README.md docs/CLASSMATE_RUN_GUIDE.md docs/superpowers/plans/2026-07-15-classmate-run-guide.md
git commit -m "docs: add classmate setup guide"
```

- [ ] **Step 4: Publish the course-project subtree**

Split and fast-forward the public `main` branch using the repository's existing subtree publishing workflow, then verify the raw public README contains the classmate guide link.
