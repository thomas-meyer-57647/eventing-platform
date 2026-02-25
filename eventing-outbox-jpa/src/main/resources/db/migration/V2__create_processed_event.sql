CREATE TABLE processed_event (
    company_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(200) NOT NULL,
    event_id CHAR(36) NOT NULL,
    processed_at_utc TIMESTAMP NOT NULL,
    UNIQUE KEY uniq_processed_company_consumer_event (company_id, consumer_name, event_id)
);
