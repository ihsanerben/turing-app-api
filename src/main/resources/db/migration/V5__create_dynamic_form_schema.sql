CREATE TABLE forms (
    id uuid PRIMARY KEY,
    period_id uuid NOT NULL REFERENCES application_periods(id) ON DELETE RESTRICT,
    name varchar(200) NOT NULL,
    version_number integer NOT NULL CHECK (version_number > 0),
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    published_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_forms_period_version UNIQUE (period_id, version_number),
    CONSTRAINT ck_forms_published_at CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status <> 'PUBLISHED')
    )
);

CREATE UNIQUE INDEX uk_forms_one_published_per_period
    ON forms(period_id) WHERE status = 'PUBLISHED';
CREATE INDEX idx_forms_period_status ON forms(period_id, status);

CREATE TABLE form_sections (
    id uuid PRIMARY KEY,
    form_id uuid NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    title varchar(200) NOT NULL,
    description varchar(1000),
    display_order integer NOT NULL CHECK (display_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_form_sections_order UNIQUE (form_id, display_order) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_form_sections_form ON form_sections(form_id);

CREATE TABLE form_fields (
    id uuid PRIMARY KEY,
    form_id uuid NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    section_id uuid NOT NULL REFERENCES form_sections(id) ON DELETE CASCADE,
    field_key varchar(80) NOT NULL,
    label varchar(250) NOT NULL,
    field_type varchar(20) NOT NULL CHECK (field_type IN (
        'TEXT', 'TEXTAREA', 'INTEGER', 'DECIMAL', 'DATE', 'BOOLEAN',
        'SELECT', 'MULTI_SELECT', 'RADIO', 'CHECKBOX', 'EMAIL', 'PHONE', 'FILE'
    )),
    required boolean NOT NULL,
    display_order integer NOT NULL CHECK (display_order >= 0),
    placeholder varchar(250),
    validation_rules jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_form_fields_key UNIQUE (form_id, field_key) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uk_form_fields_order UNIQUE (section_id, display_order) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_form_fields_form ON form_fields(form_id);
CREATE INDEX idx_form_fields_section ON form_fields(section_id);

CREATE TABLE form_field_options (
    id uuid PRIMARY KEY,
    field_id uuid NOT NULL REFERENCES form_fields(id) ON DELETE CASCADE,
    label varchar(200) NOT NULL,
    option_value varchar(100) NOT NULL,
    display_order integer NOT NULL CHECK (display_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_form_options_value UNIQUE (field_id, option_value) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uk_form_options_order UNIQUE (field_id, display_order) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_form_options_field ON form_field_options(field_id);
