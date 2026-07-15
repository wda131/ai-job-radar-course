USE ai_job_radar;

ALTER TABLE match_results
  ADD COLUMN rule_score INT NOT NULL DEFAULT 0 AFTER score,
  ADD COLUMN semantic_score INT NULL AFTER rule_score,
  ADD COLUMN ai_used TINYINT(1) NOT NULL DEFAULT 0 AFTER semantic_score,
  ADD COLUMN strengths VARCHAR(1000) NOT NULL DEFAULT '' AFTER summary,
  ADD COLUMN gaps VARCHAR(1000) NOT NULL DEFAULT '' AFTER strengths,
  ADD COLUMN suggestions VARCHAR(1000) NOT NULL DEFAULT '' AFTER gaps;

UPDATE match_results SET rule_score = score WHERE rule_score = 0;

ALTER TABLE interview_answers
  ADD COLUMN strengths VARCHAR(1000) NOT NULL DEFAULT '' AFTER feedback,
  ADD COLUMN weaknesses VARCHAR(1000) NOT NULL DEFAULT '' AFTER strengths,
  ADD COLUMN suggestion VARCHAR(1000) NOT NULL DEFAULT '' AFTER weaknesses,
  ADD COLUMN ai_used TINYINT(1) NOT NULL DEFAULT 0 AFTER suggestion;
