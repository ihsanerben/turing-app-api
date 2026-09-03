CREATE TABLE application_notes (
    id uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    admin_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    content varchar(2000) NOT NULL,
    visibility varchar(16) NOT NULL DEFAULT 'INTERNAL' CHECK (visibility = 'INTERNAL'),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_application_notes_application UNIQUE (application_id)
);
CREATE INDEX idx_application_notes_application_created ON application_notes(application_id, created_at DESC);
