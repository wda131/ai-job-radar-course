USE ai_job_radar;

ALTER TABLE jobs
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER welfare_tags,
  ADD COLUMN external_id VARCHAR(100) NULL AFTER source,
  ADD COLUMN source_url VARCHAR(1000) NOT NULL DEFAULT '' AFTER external_id,
  ADD COLUMN imported_at DATETIME NULL AFTER source_url,
  ADD UNIQUE KEY uk_jobs_source_external (source, external_id);
