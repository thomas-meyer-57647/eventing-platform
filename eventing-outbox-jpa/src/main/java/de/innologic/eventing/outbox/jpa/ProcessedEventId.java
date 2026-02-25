package de.innologic.eventing.outbox.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProcessedEventId implements Serializable {

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "consumer_name", nullable = false, length = 200)
    private String consumerName;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    public ProcessedEventId() {
        // for JPA
    }

    public ProcessedEventId(String companyId, String consumerName, String eventId) {
        this.companyId = companyId;
        this.consumerName = consumerName;
        this.eventId = eventId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEventId that)) return false;
        return Objects.equals(companyId, that.companyId) &&
                Objects.equals(consumerName, that.consumerName) &&
                Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, consumerName, eventId);
    }
}
