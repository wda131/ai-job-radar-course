USE ai_job_radar;

SET @salary_text_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = 'ai_job_radar'
    AND table_name = 'jobs'
    AND column_name = 'salary_text'
);
SET @salary_text_sql = IF(
  @salary_text_exists = 0,
  'ALTER TABLE jobs ADD COLUMN salary_text VARCHAR(50) NOT NULL DEFAULT '''' AFTER salary_max',
  'SELECT 1'
);
PREPARE salary_text_statement FROM @salary_text_sql;
EXECUTE salary_text_statement;
DEALLOCATE PREPARE salary_text_statement;
