CREATE TABLE professionals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    document VARCHAR(80) NOT NULL,
    team VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'VIEWER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE contract_rules (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES professionals(id),
    absence_type VARCHAR(40) NOT NULL CHECK (absence_type IN ('VACATION', 'MEDICAL_LEAVE', 'PERSONAL_LEAVE', 'BONUS_DAY', 'OTHER')),
    days_allowed INTEGER NOT NULL CHECK (days_allowed >= 0),
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_contract_rule_professional_type UNIQUE (professional_id, absence_type),
    CONSTRAINT ck_contract_rule_dates CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE TABLE absence_requests (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES professionals(id),
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    absence_type VARCHAR(40) NOT NULL CHECK (absence_type IN ('VACATION', 'MEDICAL_LEAVE', 'PERSONAL_LEAVE', 'BONUS_DAY', 'OTHER')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    requested_days INTEGER NOT NULL CHECK (requested_days > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_absence_request_dates CHECK (end_date >= start_date)
);

CREATE TABLE approval_history (
    id BIGSERIAL PRIMARY KEY,
    absence_request_id BIGINT NOT NULL REFERENCES absence_requests(id),
    changed_by_id BIGINT NOT NULL REFERENCES users(id),
    from_status VARCHAR(30) NOT NULL CHECK (from_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    to_status VARCHAR(30) NOT NULL CHECK (to_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    comment VARCHAR(1000),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_absence_requests_professional_period ON absence_requests (professional_id, start_date, end_date);
CREATE INDEX ix_absence_requests_status ON absence_requests (status);
CREATE INDEX ix_absence_requests_report ON absence_requests (absence_type, start_date);
CREATE INDEX ix_approval_history_request ON approval_history (absence_request_id, changed_at);
