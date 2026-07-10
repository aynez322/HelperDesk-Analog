CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    subject         VARCHAR(150) NOT NULL,
    description     TEXT,
    type            VARCHAR(10)  NOT NULL
                    CHECK (type IN ('LIVE','FORM')),
    status          VARCHAR(15)  NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    priority        VARCHAR(10)  NOT NULL DEFAULT 'NORMAL'
                    CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    category_id     BIGINT REFERENCES categories(id),
    created_by      BIGINT REFERENCES users(id),
    assigned_to     BIGINT REFERENCES users(id),
    requester_email VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP
);

CREATE TABLE messages (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT NOT NULL REFERENCES tickets(id),
    sender_id   BIGINT REFERENCES users(id),
    sender_type VARCHAR(10) NOT NULL
                CHECK (sender_type IN ('CLIENT','AGENT','SYSTEM')),
    body        TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_status      ON tickets(status);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_messages_ticket_id  ON messages(ticket_id);
