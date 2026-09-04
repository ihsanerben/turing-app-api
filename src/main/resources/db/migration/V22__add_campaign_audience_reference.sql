ALTER TABLE email_campaigns
    ADD COLUMN audience_list_id uuid REFERENCES audience_lists(id) ON DELETE SET NULL,
    ADD COLUMN audience_list_name varchar(200);

CREATE INDEX idx_email_campaigns_audience_list
    ON email_campaigns(audience_list_id);
