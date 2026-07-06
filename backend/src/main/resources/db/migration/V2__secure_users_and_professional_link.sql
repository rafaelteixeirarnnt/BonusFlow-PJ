ALTER TABLE professionals
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS professional_id BIGINT REFERENCES professionals(id),
    ADD COLUMN IF NOT EXISTS system_user_flag BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

UPDATE users
SET password = '{noop}change-me'
WHERE password IS NULL;

ALTER TABLE users
    ALTER COLUMN password SET NOT NULL;

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'users'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%role%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

UPDATE users
SET name = 'Super Admin',
    password = '$2a$10$bL5VQNtqMZebq0dXVNNlUuyCXgm80R.NLwWoNHQQonRWhdY7r2Y0.',
    role = 'SUPER_ADMIN',
    active = TRUE,
    professional_id = NULL,
    system_user_flag = TRUE,
    updated_at = now()
WHERE lower(email) = lower('admin@bonusflow.com');

INSERT INTO users (name, email, password, role, active, professional_id, system_user_flag, created_at, updated_at)
SELECT
    'Super Admin',
    'admin@bonusflow.com',
    '$2a$10$bL5VQNtqMZebq0dXVNNlUuyCXgm80R.NLwWoNHQQonRWhdY7r2Y0.',
    'SUPER_ADMIN',
    TRUE,
    NULL,
    TRUE,
    now(),
    now()
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE lower(email) = lower('admin@bonusflow.com')
);

INSERT INTO professionals (name, email, document, team, active, created_at, updated_at)
SELECT
    u.name,
    u.email,
    'LEGACY-USER-' || u.id,
    'Legado',
    u.active,
    now(),
    now()
FROM users u
WHERE u.role <> 'SUPER_ADMIN'
  AND u.professional_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM professionals p
      WHERE lower(p.email) = lower(u.email)
  );

UPDATE users u
SET professional_id = p.id,
    updated_at = now()
FROM professionals p
WHERE u.role <> 'SUPER_ADMIN'
  AND u.professional_id IS NULL
  AND lower(p.email) = lower(u.email);

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'PROFESSIONAL', 'VIEWER')),
    ADD CONSTRAINT ck_users_professional_required CHECK (role = 'SUPER_ADMIN' OR professional_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_professional_not_null
    ON users (professional_id)
    WHERE professional_id IS NOT NULL;
