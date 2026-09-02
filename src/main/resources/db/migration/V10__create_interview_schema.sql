CREATE TABLE interviews (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW','RESCHEDULED')),
    location_type varchar(16) NOT NULL CHECK (location_type IN ('ONLINE','IN_PERSON','PHONE')),
    location varchar(300),
    meeting_url varchar(1000),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_interviews_time CHECK (ends_at > starts_at),
    CONSTRAINT ck_interviews_location CHECK (
        (location_type = 'ONLINE' AND meeting_url IS NOT NULL) OR
        (location_type = 'IN_PERSON' AND location IS NOT NULL) OR
        location_type = 'PHONE'
    )
);

CREATE INDEX idx_interviews_application_time ON interviews(application_id, starts_at DESC);
CREATE INDEX idx_interviews_status_time ON interviews(status, starts_at);

CREATE TABLE interview_feedback (
    id uuid PRIMARY KEY,
    interview_id uuid NOT NULL REFERENCES interviews(id) ON DELETE RESTRICT,
    interviewer_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    score numeric(5,2) CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    notes varchar(4000) NOT NULL,
    recommendation varchar(2000),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_interview_feedback_interviewer UNIQUE (interview_id, interviewer_id)
);

CREATE INDEX idx_interview_feedback_interview ON interview_feedback(interview_id);
