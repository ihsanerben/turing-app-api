CREATE TABLE scholarship_programs (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_scholarship_programs_slug UNIQUE (slug),
    CONSTRAINT ck_scholarship_programs_slug CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$')
);
CREATE INDEX idx_scholarship_programs_active_name ON scholarship_programs (active, name);

CREATE TABLE application_periods (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES scholarship_programs(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    academic_year VARCHAR(9) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    max_recipients INTEGER,
    allow_withdrawal BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_period_program_year_name UNIQUE (program_id, academic_year, name),
    CONSTRAINT ck_period_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_period_academic_year CHECK (academic_year ~ '^[0-9]{4}-[0-9]{4}$'),
    CONSTRAINT ck_period_max_recipients CHECK (max_recipients IS NULL OR max_recipients > 0),
    CONSTRAINT ck_period_status CHECK (status IN ('DRAFT','SCHEDULED','OPEN','CLOSED','COMPLETED','ARCHIVED'))
);
CREATE INDEX idx_period_program_status ON application_periods (program_id, status);
CREATE INDEX idx_period_status_time ON application_periods (status, starts_at, ends_at);
