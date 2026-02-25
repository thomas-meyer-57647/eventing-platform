package de.innologic.eventing.outbox.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "processed_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_processed_company_consumer_event", columnNames = {"company_id", "consumer_name", "event_id"})
        }
)
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "processed_at_utc", nullable = false)
    private Instant processedAtUtc;

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

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getProcessedAtUtc() {
        return processedAtUtc;
    }

    public void setProcessedAtUtc(Instant processedAtUtc) {
        this.processedAtUtc = processedAtUtc;
    }
}
