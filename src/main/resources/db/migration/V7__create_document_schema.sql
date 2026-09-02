CREATE TABLE document_requirements (
    id uuid PRIMARY KEY,
    period_id uuid NOT NULL REFERENCES application_periods(id) ON DELETE RESTRICT,
    name varchar(200) NOT NULL,
    description varchar(1000),
    required boolean NOT NULL,
    allowed_mime_types jsonb NOT NULL,
    max_size_bytes bigint NOT NULL CHECK (max_size_bytes BETWEEN 1 AND 10485760),
    display_order integer NOT NULL CHECK (display_order >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uk_document_requirements_name UNIQUE (period_id, name),
    CONSTRAINT uk_document_requirements_order UNIQUE (period_id, display_order)
);

CREATE INDEX idx_document_requirements_period ON document_requirements(period_id);

ALTER TABLE form_fields ADD COLUMN requirement_id uuid REFERENCES document_requirements(id) ON DELETE RESTRICT;
CREATE INDEX idx_form_fields_requirement ON form_fields(requirement_id);

CREATE TABLE files (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    requirement_id uuid NOT NULL REFERENCES document_requirements(id) ON DELETE RESTRICT,
    original_name varchar(255) NOT NULL,
    storage_key varchar(500) NOT NULL,
    mime_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0),
    provider varchar(20) NOT NULL CHECK (provider IN ('MINIO')),
    checksum_sha256 varchar(64) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','REPLACED','DELETED')),
    uploaded_at timestamptz NOT NULL,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_files_storage_key UNIQUE (storage_key)
);

CREATE UNIQUE INDEX uk_files_active_requirement ON files(application_id, requirement_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_files_owner ON files(owner_id);
CREATE INDEX idx_files_application ON files(application_id);
CREATE INDEX idx_files_requirement ON files(requirement_id);
