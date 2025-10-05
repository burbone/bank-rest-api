CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    from_card_id BIGINT NOT NULL,
    to_card_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED', 'PENDING')),
    description VARCHAR(255),
    CONSTRAINT fk_transactions_from_card FOREIGN KEY (from_card_id) REFERENCES cards(id),
    CONSTRAINT fk_transactions_to_card FOREIGN KEY (to_card_id) REFERENCES cards(id),
    CONSTRAINT chk_different_cards CHECK (from_card_id != to_card_id)
);

CREATE INDEX idx_transactions_from_card ON transactions(from_card_id);
CREATE INDEX idx_transactions_to_card ON transactions(to_card_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_status ON transactions(status);