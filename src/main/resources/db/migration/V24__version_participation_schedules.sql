ALTER TABLE meal_weeks ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
ALTER TABLE participation_activities ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
