CREATE TABLE es_outbox_events (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    index_name   VARCHAR(100) NOT NULL,
    document_id  VARCHAR(100) NOT NULL,
    action_type  VARCHAR(20)  NOT NULL,
    payload      TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count  INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    indexed_at   TIMESTAMPTZ
);

CREATE INDEX idx_es_outbox_pending ON es_outbox_events (status, created_at) WHERE status = 'PENDING';
