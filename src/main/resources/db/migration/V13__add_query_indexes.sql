CREATE INDEX idx_applications_admin_created
    ON applications (created_at DESC, id);

CREATE INDEX idx_applications_period_status_score
    ON applications (period_id, status, calculated_score DESC, id);

CREATE INDEX idx_audit_action_created
    ON audit_logs (action, created_at DESC, id);

CREATE INDEX idx_audit_type_created
    ON audit_logs (entity_type, created_at DESC, id);
