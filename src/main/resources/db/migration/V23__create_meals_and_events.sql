CREATE TABLE meal_weeks (
    id uuid PRIMARY KEY,
    week_start date NOT NULL UNIQUE CHECK (extract(isodow FROM week_start) = 1),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL
);

CREATE TABLE participation_activities (
    id uuid PRIMARY KEY,
    week_id uuid REFERENCES meal_weeks(id) ON DELETE RESTRICT,
    title varchar(200) NOT NULL CHECK (length(trim(title)) > 0),
    description text NOT NULL,
    meal_date date,
    starts_at timestamptz,
    location varchar(500) NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_participation_activity_kind CHECK (
        (week_id IS NOT NULL AND meal_date IS NOT NULL AND starts_at IS NULL)
        OR (week_id IS NULL AND meal_date IS NULL AND starts_at IS NOT NULL)
    ),
    CONSTRAINT uk_meal_date UNIQUE (meal_date)
);

CREATE INDEX idx_participation_activities_week ON participation_activities(week_id, meal_date);
CREATE INDEX idx_participation_events_start ON participation_activities(starts_at, id) WHERE week_id IS NULL;

CREATE TABLE participation_registrations (
    activity_id uuid NOT NULL REFERENCES participation_activities(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (activity_id, user_id)
);

CREATE INDEX idx_participation_registrations_user ON participation_registrations(user_id, activity_id);

CREATE TABLE participation_selection_versions (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scope varchar(40) NOT NULL,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    PRIMARY KEY (user_id, scope)
);
