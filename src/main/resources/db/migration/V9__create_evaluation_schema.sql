CREATE TABLE evaluation_criteria (
    id uuid PRIMARY KEY,
    period_id uuid NOT NULL REFERENCES application_periods(id) ON DELETE RESTRICT,
    name varchar(160) NOT NULL,
    description varchar(1000),
    max_score numeric(8,2) NOT NULL CHECK (max_score > 0),
    weight numeric(8,2) NOT NULL CHECK (weight > 0),
    display_order integer NOT NULL CHECK (display_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_evaluation_criteria_period_name UNIQUE (period_id, name),
    CONSTRAINT uk_evaluation_criteria_period_order UNIQUE (period_id, display_order)
);

CREATE INDEX idx_evaluation_criteria_period_order ON evaluation_criteria(period_id, display_order);

CREATE TABLE evaluation_scores (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    criterion_id uuid NOT NULL REFERENCES evaluation_criteria(id) ON DELETE RESTRICT,
    reviewer_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    score numeric(8,2) NOT NULL CHECK (score >= 0),
    comment varchar(2000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_evaluation_score_reviewer UNIQUE (application_id, criterion_id, reviewer_id)
);

CREATE INDEX idx_evaluation_scores_application ON evaluation_scores(application_id);
CREATE INDEX idx_evaluation_scores_criterion ON evaluation_scores(criterion_id);
CREATE INDEX idx_evaluation_scores_reviewer ON evaluation_scores(reviewer_id);

ALTER TABLE applications
    ADD CONSTRAINT ck_applications_calculated_score
    CHECK (calculated_score IS NULL OR calculated_score BETWEEN 0 AND 100);
