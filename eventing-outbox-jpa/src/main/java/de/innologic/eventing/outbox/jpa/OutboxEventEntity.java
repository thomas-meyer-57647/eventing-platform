package de.innologic.eventing.outbox.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "outbox_event",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(name = "uniq_outbox_company_event", columnNames = {"company_id", "event_id"})
        },
        indexes = {
                @Index(name = "idx_outbox_status_next_attempt", columnList = "status,next_attempt_at_utc")
        }
)
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @Column(name = "occurred_at_utc", nullable = false)
    private Instant occurredAtUtc;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "causation_id", length = 36)
    private String causationId;

    @Column(name = "aggregate_type", length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_attempt_at_utc")
    private Instant nextAttemptAtUtc;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;

    @Column(name = "dedup_key", length = 200)
    private String dedupKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Instant getOccurredAtUtc() {
        return occurredAtUtc;
    }

    public void setOccurredAtUtc(Instant occurredAtUtc) {
        this.occurredAtUtc = occurredAtUtc;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextAttemptAtUtc() {
        return nextAttemptAtUtc;
    }

    public void setNextAttemptAtUtc(Instant nextAttemptAtUtc) {
        this.nextAttemptAtUtc = nextAttemptAtUtc;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }
}
