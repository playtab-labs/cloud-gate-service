CREATE TABLE stage (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    max_capacity INT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reader (
    id            BIGSERIAL PRIMARY KEY,
    serial_number VARCHAR(50)  NOT NULL UNIQUE,
    stage_id      BIGINT       NOT NULL REFERENCES stage(id),
    direction     VARCHAR(10)  NOT NULL CHECK (direction IN ('IN', 'OUT')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tag_event (
    id          BIGSERIAL PRIMARY KEY,
    chip_serial VARCHAR(14) NOT NULL,
    reader_id   BIGINT      NOT NULL REFERENCES reader(id),
    event_type  VARCHAR(10) NOT NULL CHECK (event_type IN ('ENTER', 'EXIT')),
    tagged_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_tag_event_chip_serial ON tag_event(chip_serial);
CREATE INDEX idx_tag_event_tagged_at ON tag_event(tagged_at);
CREATE INDEX idx_tag_event_reader_id ON tag_event(reader_id);
