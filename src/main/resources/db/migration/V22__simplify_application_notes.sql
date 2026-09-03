DELETE FROM application_notes older
USING application_notes newer
WHERE older.application_id = newer.application_id
  AND (older.created_at, older.id) < (newer.created_at, newer.id);

ALTER TABLE application_notes
    ADD CONSTRAINT uk_application_notes_application UNIQUE (application_id);
