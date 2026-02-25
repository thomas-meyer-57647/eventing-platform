package de.innologic.eventing.outbox.jpa;

import com.fasterxml.jackson.databind.JsonNode;
import de.innologic.eventing.core.EventEnvelope;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class OutboxPublisher {

    private static final String STATUS_NEW = "NEW";
    private static final int SCHEMA_VERSION = 1;

    private final OutboxEventRepository repository;
    private final Clock clock;

    public OutboxPublisher(OutboxEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    OutboxPublisher(OutboxEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OutboxEventEntity enqueue(EventEnvelope envelope) {
        requireCompany(envelope);

        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setCompanyId(envelope.getCompanyId());
        entity.setEventId(resolveEventId(envelope));
        entity.setEventType(envelope.getType());
        entity.setSchemaVersion(SCHEMA_VERSION);
        entity.setPayloadJson(requirePayload(envelope.getPayload()));
        entity.setStatus(STATUS_NEW);
        entity.setRetryCount(0);
        entity.setOccurredAtUtc(Instant.now(clock));
        entity.setCorrelationId(envelope.getCorrelationId());
        entity.setCausationId(envelope.getCausationId());
        return repository.save(entity);
    }

    private static void requireCompany(EventEnvelope envelope) {
        if (envelope == null || envelope.getCompanyId() == null || envelope.getCompanyId().isBlank()) {
            throw new IllegalArgumentException("companyId is required");
        }
    }

    private static String resolveEventId(EventEnvelope envelope) {
        return envelope.getEventId() != null ? envelope.getEventId() : UUID.randomUUID().toString();
    }

    private static String requirePayload(JsonNode payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
        return payload.toString();
    }
}
