ALTER TABLE email_campaigns
    ADD COLUMN attachment_name varchar(255),
    ADD COLUMN attachment_data bytea;

ALTER TABLE email_campaigns
    ADD CONSTRAINT ck_email_campaign_attachment_pair CHECK (
        (attachment_name IS NULL AND attachment_data IS NULL)
        OR (attachment_name IS NOT NULL AND attachment_data IS NOT NULL)
    );
