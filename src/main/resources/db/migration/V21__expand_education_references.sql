WITH university_names(name) AS (
    VALUES
        ('Ankara Üniversitesi'),
        ('Ege Üniversitesi'),
        ('Gazi Üniversitesi'),
        ('Hacettepe Üniversitesi'),
        ('İstanbul Üniversitesi'),
        ('Marmara Üniversitesi'),
        ('Dokuz Eylül Üniversitesi'),
        ('Yıldız Teknik Üniversitesi'),
        ('Akdeniz Üniversitesi'),
        ('Çukurova Üniversitesi')
)
INSERT INTO universities (id, name, country_code, active, created_at, updated_at)
SELECT md5('university:' || name)::uuid, name, 'TR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM university_names
ON CONFLICT DO NOTHING;

WITH department_names(name) AS (
    VALUES
        ('Bilgisayar Mühendisliği'),
        ('Elektrik-Elektronik Mühendisliği'),
        ('Endüstri Mühendisliği'),
        ('Makine Mühendisliği'),
        ('İnşaat Mühendisliği'),
        ('İşletme'),
        ('İktisat'),
        ('Hukuk'),
        ('Psikoloji'),
        ('Tıp')
)
INSERT INTO departments (
    id,
    university_id,
    name,
    faculty,
    active,
    created_at,
    updated_at
)
SELECT
    md5('department:' || university.id::text || ':' || department.name)::uuid,
    university.id,
    department.name,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM universities university
CROSS JOIN department_names department
ON CONFLICT DO NOTHING;
