CREATE TABLE announcements (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL,
    slug varchar(200) NOT NULL,
    summary varchar(500) NOT NULL,
    content text NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    published_at timestamptz,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_announcements_slug UNIQUE (slug),
    CONSTRAINT ck_announcements_slug CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    CONSTRAINT ck_announcements_publish CHECK ((status='PUBLISHED' AND published_at IS NOT NULL) OR status<>'PUBLISHED')
);
CREATE INDEX idx_announcements_public ON announcements(status,published_at DESC);
