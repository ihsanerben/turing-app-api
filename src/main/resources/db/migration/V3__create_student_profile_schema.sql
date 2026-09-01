CREATE TABLE universities (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_universities_name_country ON universities (LOWER(name), country_code);
CREATE INDEX idx_universities_active_name ON universities (active, name);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    faculty VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_departments_university_name ON departments (university_id, LOWER(name));
CREATE INDEX idx_departments_university_active ON departments (university_id, active, name);

CREATE TABLE student_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    national_id_encrypted BYTEA,
    birth_date DATE,
    phone VARCHAR(32),
    address_line VARCHAR(300),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country_code VARCHAR(2),
    university_id UUID REFERENCES universities(id) ON DELETE RESTRICT,
    department_id UUID REFERENCES departments(id) ON DELETE RESTRICT,
    other_university VARCHAR(200),
    other_department VARCHAR(200),
    education_level VARCHAR(32),
    study_year INTEGER,
    gpa NUMERIC(4,2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT ck_profiles_country_code CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_profiles_education_level CHECK (education_level IS NULL OR education_level IN ('HIGH_SCHOOL', 'ASSOCIATE', 'BACHELOR', 'MASTER', 'DOCTORATE')),
    CONSTRAINT ck_profiles_study_year CHECK (study_year IS NULL OR study_year BETWEEN 1 AND 8),
    CONSTRAINT ck_profiles_gpa CHECK (gpa IS NULL OR gpa BETWEEN 0 AND 4),
    CONSTRAINT ck_profiles_university_choice CHECK ((university_id IS NOT NULL AND other_university IS NULL) OR (university_id IS NULL AND other_university IS NOT NULL) OR (university_id IS NULL AND other_university IS NULL)),
    CONSTRAINT ck_profiles_department_choice CHECK ((department_id IS NOT NULL AND other_department IS NULL) OR (department_id IS NULL AND other_department IS NOT NULL) OR (department_id IS NULL AND other_department IS NULL))
);
CREATE INDEX idx_profiles_university_department ON student_profiles (university_id, department_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB NOT NULL,
    new_values JSONB NOT NULL,
    ip_reference VARCHAR(64),
    request_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_actor_created ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_entity_created ON audit_logs (entity_type, entity_id, created_at DESC);

INSERT INTO universities (id, name, country_code, active, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', 'Boğaziçi Üniversitesi', 'TR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000002', 'İstanbul Teknik Üniversitesi', 'TR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000003', 'Orta Doğu Teknik Üniversitesi', 'TR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO departments (id, university_id, name, faculty, active, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Bilgisayar Mühendisliği', 'Mühendislik Fakültesi', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Bilgisayar Mühendisliği', 'Bilgisayar ve Bilişim Fakültesi', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Bilgisayar Mühendisliği', 'Mühendislik Fakültesi', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
