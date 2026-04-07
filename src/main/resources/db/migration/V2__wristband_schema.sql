CREATE TABLE wristband (
    id          BIGSERIAL PRIMARY KEY,
    rfid        VARCHAR(14) NOT NULL UNIQUE,
    active_date DATE        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE wristband_ownership (
    id           BIGSERIAL PRIMARY KEY,
    identity_id  UUID      NOT NULL,
    wristband_id BIGINT    NOT NULL UNIQUE REFERENCES wristband(id),
    linked_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_wristband_rfid ON wristband(rfid);
CREATE INDEX idx_wristband_active_date ON wristband(active_date);
CREATE INDEX idx_ownership_identity_id ON wristband_ownership(identity_id);
