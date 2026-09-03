ALTER TABLE student_profiles ADD COLUMN national_id varchar(64);

COMMENT ON COLUMN student_profiles.national_id_encrypted IS
    'Legacy encrypted value; cleared when the profile is next saved.';
