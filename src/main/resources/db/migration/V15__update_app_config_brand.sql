UPDATE app_config
SET application_name = 'Turing Otomobil Kurumu',
    tagline = 'Eğitime destek, geleceğe yatırım.',
    footer_text = 'Turing Otomobil Kurumu',
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE id = '00000000-0000-0000-0000-000000000001'
  AND application_name = 'Turing Scholarship';
