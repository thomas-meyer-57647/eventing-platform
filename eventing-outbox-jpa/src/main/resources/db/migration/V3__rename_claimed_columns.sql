ALTER TABLE outbox_event
    CHANGE COLUMN claimed_at claimed_at_utc TIMESTAMP NULL,
    CHANGE COLUMN claimed_by claimed_by VARCHAR(100) NULL;
