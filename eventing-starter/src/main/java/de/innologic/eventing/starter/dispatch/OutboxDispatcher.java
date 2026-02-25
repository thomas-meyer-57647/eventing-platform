package de.innologic.eventing.starter.dispatch;

import de.innologic.eventing.core.TopicNaming;
import de.innologic.eventing.outbox.jpa.OutboxEventEntity;
import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import de.innologic.eventing.starter.config.EventingDispatcherProperties;
import de.innologic.eventing.starter.event.EventBus;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final String instanceId;

    public OutboxDispatcher(EventingDispatcherProperties properties,
                            EventBus eventBus,
                            OutboxEventRepository repository) {
        this(properties, eventBus, repository, Clock.systemUTC(), UUID.randomUUID().toString());
    }

    OutboxDispatcher(EventingDispatcherProperties properties,
                     EventBus eventBus,
                     OutboxEventRepository repository,
                     Clock clock,
                     String instanceId) {
        this.properties = properties;
        this.eventBus = eventBus;
        this.repository = repository;
        this.clock = clock;
        this.instanceId = instanceId;
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
            String topic = TopicNaming.topic(event.getCompanyId(), "dispatcher", event.getEventType());
            eventBus.publish(topic, event.getPayloadJson());
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
}
