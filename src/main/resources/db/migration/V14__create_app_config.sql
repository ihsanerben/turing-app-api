CREATE TABLE app_config (
    id uuid PRIMARY KEY,
    application_name varchar(100) NOT NULL,
    tagline varchar(240) NOT NULL,
    logo_url varchar(500),
    primary_color varchar(7) NOT NULL,
    support_email varchar(320) NOT NULL,
    support_phone varchar(40),
    contact_address varchar(500),
    footer_text varchar(300) NOT NULL,
    maintenance_notice_enabled boolean NOT NULL DEFAULT false,
    maintenance_notice varchar(500),
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_app_config_singleton CHECK (id = '00000000-0000-0000-0000-000000000001'),
    CONSTRAINT ck_app_config_primary_color CHECK (primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_app_config_maintenance_notice CHECK (
        NOT maintenance_notice_enabled OR maintenance_notice IS NOT NULL
    )
);

INSERT INTO app_config (
    id, application_name, tagline, primary_color, support_email, footer_text, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Turing Scholarship',
    'Potansiyelini geleceğe taşı.',
    '#3855CF',
    'info@turing.local',
    'Turing Scholarship',
    CURRENT_TIMESTAMP
);
