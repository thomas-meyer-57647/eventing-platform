package de.innologic.eventing.starter.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.eventing.core.EventEnvelope;
import de.innologic.eventing.core.TopicNaming;
import de.innologic.eventing.outbox.jpa.OutboxEventEntity;
import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import de.innologic.eventing.starter.config.EventingDispatcherProperties;
import de.innologic.eventing.starter.event.EventBus;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxDispatcher {

    static final String STATUS_NEW = "NEW";
    static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final EventingDispatcherProperties properties;
    private final EventBus eventBus;
    private final OutboxEventRepository repository;
    private final Clock clock;
    private final ObjectMapper mapper;
    private final String instanceId;
    private final String serviceName;

    public OutboxDispatcher(EventingDispatcherProperties properties,
                            EventBus eventBus,
                            OutboxEventRepository repository,
                            Environment environment,
                            ObjectMapper mapper) {
        this(properties, eventBus, repository, Clock.systemUTC(), environment, mapper, UUID.randomUUID().toString());
    }

    OutboxDispatcher(EventingDispatcherProperties properties,
                     EventBus eventBus,
                     OutboxEventRepository repository,
                     Clock clock,
                     Environment environment,
                     ObjectMapper mapper,
                     String instanceId) {
        this.properties = properties;
        this.eventBus = eventBus;
        this.repository = repository;
        this.clock = clock;
        this.mapper = mapper;
        this.instanceId = instanceId;
        this.serviceName = determineServiceName(properties, environment);
    }

    @Scheduled(fixedDelayString = "${eventing.dispatcher.poll-interval-ms:1000}")
    @Transactional
    public void dispatch() {
        if (!properties.isEnabled()) {
            log.debug("Eventing dispatcher disabled; skipping run");
            return;
        }

        String claimId = instanceId + ":" + UUID.randomUUID();
        int claimed = repository.claimDueNewEvents(claimId, properties.getBatchSize());
        if (claimed <= 0) {
            return;
        }

        List<OutboxEventEntity> pending = repository.findByStatusAndClaimedByOrderByOccurredAtUtcAsc(
                STATUS_PROCESSING, claimId
        );

        for (OutboxEventEntity event : pending) {
            dispatchEvent(event);
        }
    }

    private void dispatchEvent(OutboxEventEntity event) {
        try {
            EventEnvelope envelope = buildEnvelope(event);
            String topic = TopicNaming.topic(event.getCompanyId(), serviceName, event.getEventType());
            eventBus.publish(topic, envelope);
            event.setStatus(STATUS_SENT);
            event.setNextAttemptAtUtc(null);
            event.setRetryCount(0);
            event.setClaimedAtUtc(null);
            event.setClaimedBy(null);
        } catch (Exception e) {
            handleDispatchError(event, e);
        } finally {
            repository.save(event);
        }
    }

    private EventEnvelope buildEnvelope(OutboxEventEntity event) throws Exception {
        EventEnvelope envelope = new EventEnvelope();
        envelope.setEventId(event.getEventId());
        envelope.setType(event.getEventType());
        envelope.setCompanyId(event.getCompanyId());
        envelope.setTimestamp(event.getOccurredAtUtc() != null ? event.getOccurredAtUtc() : Instant.now(clock));
        envelope.setSchemaVersion(event.getSchemaVersion());
        envelope.setCorrelationId(event.getCorrelationId());
        envelope.setCausationId(event.getCausationId());
        JsonNode payload = mapper.readTree(event.getPayloadJson());
        envelope.setPayload(payload);
        return envelope;
    }

    private void handleDispatchError(OutboxEventEntity event, Exception error) {
        log.warn("Failed to publish outbox event {}", event.getEventId(), error);
        int attempts = event.getRetryCount() + 1;
        event.setRetryCount(attempts);

        if (attempts >= properties.getMaxAttempts()) {
            event.setStatus(STATUS_FAILED);
            event.setNextAttemptAtUtc(null);
            event.setClaimedAtUtc(null);
            event.setClaimedBy(null);
            return;
        }

        event.setStatus(STATUS_NEW);
        event.setClaimedAtUtc(null);
        event.setClaimedBy(null);
        long delay = calculateBackoffMillis(attempts);
        event.setNextAttemptAtUtc(Instant.now(clock).plusMillis(delay));
    }

    private long calculateBackoffMillis(int attempts) {
        double multiplierPow = Math.pow(properties.getBackoffMultiplier(), attempts - 1);
        long delay = Math.round(properties.getBackoffInitialMs() * multiplierPow);
        return Math.min(delay, properties.getBackoffMaxMs());
    }

    private String determineServiceName(EventingDispatcherProperties properties, Environment environment) {
        if (properties.getServiceName() != null && !properties.getServiceName().isBlank()) {
            return properties.getServiceName();
        }
        return environment.getProperty("spring.application.name", "unknown");
    }
}
