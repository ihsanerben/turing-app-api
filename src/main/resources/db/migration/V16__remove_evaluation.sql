DROP TABLE evaluation_scores;
DROP TABLE evaluation_criteria;

DROP INDEX idx_applications_period_status_score;

CREATE INDEX idx_applications_period_status_created
    ON applications (period_id, status, created_at DESC, id);

UPDATE application_periods
SET status = 'CLOSED',
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE status = 'EVALUATION';

ALTER TABLE application_periods
    DROP CONSTRAINT ck_period_status,
    ADD CONSTRAINT ck_period_status
        CHECK (status IN ('DRAFT','SCHEDULED','OPEN','CLOSED','COMPLETED','ARCHIVED'));

ALTER TABLE applications
    DROP CONSTRAINT ck_applications_calculated_score,
    DROP COLUMN calculated_score;
