ALTER TABLE users
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(11),
    ADD COLUMN IF NOT EXISTS birth_date DATE,
    ADD COLUMN IF NOT EXISTS mother_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS father_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_zip_code VARCHAR(8),
    ADD COLUMN IF NOT EXISTS address_street VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS address_complement VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_neighborhood VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS address_state VARCHAR(2);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_cpf_not_null ON users (cpf) WHERE cpf IS NOT NULL;

CREATE TABLE IF NOT EXISTS user_contacts (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_type VARCHAR(30) NOT NULL CHECK (contact_type IN ('RESIDENTIAL', 'MOBILE')),
    ddi VARCHAR(6) NOT NULL,
    ddd VARCHAR(4),
    phone VARCHAR(9) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_user_contacts_user ON user_contacts (user_id);
