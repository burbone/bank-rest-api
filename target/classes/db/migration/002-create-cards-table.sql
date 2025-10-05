CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    card_number_encrypted VARCHAR(500) NOT NULL,
    card_holder VARCHAR(100) NOT NULL,
    expire_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    balance NUMERIC(15, 2) DEFAULT 0.00 CHECK (balance >= 0),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_cards_owner ON cards(owner_id);
CREATE INDEX idx_cards_status ON cards(status);
CREATE INDEX idx_cards_expire_date ON cards(expire_date);