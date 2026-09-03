CREATE TABLE audience_lists (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    program_id uuid NOT NULL REFERENCES scholarship_programs(id) ON DELETE RESTRICT,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE audience_list_members (
    list_id uuid NOT NULL REFERENCES audience_lists(id) ON DELETE CASCADE,
    application_id uuid NOT NULL REFERENCES applications(id) ON DELETE RESTRICT,
    PRIMARY KEY (list_id, application_id)
);

CREATE INDEX idx_audience_lists_created_at ON audience_lists(created_at DESC);
CREATE INDEX idx_audience_list_members_application ON audience_list_members(application_id);
