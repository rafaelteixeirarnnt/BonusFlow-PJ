ALTER TABLE users
    DROP CONSTRAINT IF EXISTS ck_users_professional_required;

DROP INDEX IF EXISTS ux_users_professional_not_null;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_active_professional_not_null
    ON users (professional_id)
    WHERE professional_id IS NOT NULL AND active = TRUE;
