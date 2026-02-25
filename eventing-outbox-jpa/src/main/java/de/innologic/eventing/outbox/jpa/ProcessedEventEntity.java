package de.innologic.eventing.outbox.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at_utc", nullable = false)
    private Instant processedAtUtc;

    public ProcessedEventId getId() {
        return id;
    }

    public void setId(ProcessedEventId id) {
        this.id = id;
    }

    public Instant getProcessedAtUtc() {
        return processedAtUtc;
    }

    public void setProcessedAtUtc(Instant processedAtUtc) {
        this.processedAtUtc = processedAtUtc;
    }
}
