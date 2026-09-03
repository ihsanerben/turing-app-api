CREATE TABLE applications (
    id uuid PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES student_profiles(id) ON DELETE RESTRICT,
    period_id uuid NOT NULL REFERENCES application_periods(id) ON DELETE RESTRICT,
    form_id uuid NOT NULL REFERENCES forms(id) ON DELETE RESTRICT,
    status varchar(24) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','MISSING_DOCUMENT','SHORTLISTED','INTERVIEW','APPROVED','REJECTED','WAITLISTED','WITHDRAWN')),
    completion integer NOT NULL DEFAULT 0 CHECK (completion BETWEEN 0 AND 100),
    submitted_at timestamptz,
    decision_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_applications_profile_period UNIQUE (profile_id, period_id)
);

CREATE INDEX idx_applications_profile_created ON applications(profile_id, created_at DESC);
CREATE INDEX idx_applications_period_status ON applications(period_id, status);

CREATE TABLE application_answers (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    field_id uuid NOT NULL REFERENCES form_fields(id) ON DELETE RESTRICT,
    text_value text,
    number_value numeric(18,4),
    boolean_value boolean,
    date_value date,
    json_value jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_application_answers_field UNIQUE (application_id, field_id),
    CONSTRAINT ck_application_answers_one_value CHECK (
        num_nonnulls(text_value, number_value, boolean_value, date_value, json_value) = 1
    )
);

CREATE INDEX idx_application_answers_application ON application_answers(application_id);
CREATE INDEX idx_application_answers_field ON application_answers(field_id);

CREATE TABLE application_snapshots (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    schema_version integer NOT NULL CHECK (schema_version > 0),
    profile_data jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT uk_application_snapshots_application UNIQUE (application_id)
);

CREATE TABLE application_status_history (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    old_status varchar(24),
    new_status varchar(24) NOT NULL,
    changed_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reason varchar(500),
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_application_history_application_time ON application_status_history(application_id, created_at);
