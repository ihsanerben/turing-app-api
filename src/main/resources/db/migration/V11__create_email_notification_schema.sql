CREATE TABLE email_campaigns (
    id uuid PRIMARY KEY,
    subject varchar(200) NOT NULL,
    body text NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT','SENDING','COMPLETED')),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE email_recipients (
    id uuid PRIMARY KEY,
    campaign_id uuid NOT NULL REFERENCES email_campaigns(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    email varchar(320) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    failure_message varchar(1000),
    sent_at timestamptz,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_email_recipient_campaign_user UNIQUE (campaign_id, user_id)
);

CREATE INDEX idx_email_campaigns_created_at ON email_campaigns(created_at DESC);
CREATE INDEX idx_email_recipients_campaign_status ON email_recipients(campaign_id, status);

CREATE TABLE notifications (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title varchar(200) NOT NULL,
    message varchar(1000) NOT NULL,
    type varchar(50) NOT NULL,
    related_type varchar(50),
    related_id uuid,
    read_at timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

CREATE FUNCTION notify_application_status_change() RETURNS trigger AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO notifications(id,user_id,title,message,type,related_type,related_id,created_at)
        SELECT gen_random_uuid(),p.user_id,'Başvuru durumunuz güncellendi','Başvurunuzun yeni durumu: ' || NEW.status,
               'APPLICATION_STATUS','APPLICATION',NEW.id,NEW.updated_at
        FROM student_profiles p WHERE p.id=NEW.profile_id;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_application_status_notification AFTER UPDATE OF status ON applications
FOR EACH ROW EXECUTE FUNCTION notify_application_status_change();

CREATE FUNCTION notify_interview_change() RETURNS trigger AS $$
BEGIN
    INSERT INTO notifications(id,user_id,title,message,type,related_type,related_id,created_at)
    SELECT gen_random_uuid(),p.user_id,
           CASE WHEN TG_OP='INSERT' THEN 'Mülakat planlandı' ELSE 'Mülakatınız güncellendi' END,
           CASE WHEN TG_OP='INSERT' THEN 'Yeni bir mülakat planlandı.' ELSE 'Mülakat bilgileriniz veya durumu güncellendi.' END,
           'INTERVIEW','INTERVIEW',NEW.id,NEW.updated_at
    FROM applications a JOIN student_profiles p ON p.id=a.profile_id WHERE a.id=NEW.application_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_interview_notification AFTER INSERT OR UPDATE OF starts_at,ends_at,status,location_type,location,meeting_url ON interviews
FOR EACH ROW EXECUTE FUNCTION notify_interview_change();
