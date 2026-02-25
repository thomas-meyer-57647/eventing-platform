package de.innologic.eventing.outbox.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.innologic.eventing.core.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-25T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OutboxEventRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger savedCount = new AtomicInteger();

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(repository, FIXED_CLOCK);
        savedCount.set(0);
        when(repository.count()).thenAnswer(invocation -> (long) savedCount.get());
    }

    @Test
    void enqueueWithCompanyIdPersistsEvent() {
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> {
            savedCount.incrementAndGet();
            return invocation.getArgument(0);
        });

        EventEnvelope envelope = createEnvelope("company-42");

        assertEquals(0L, repository.count());
        OutboxEventEntity saved = publisher.enqueue(envelope);
        assertEquals(1L, repository.count());
        assertEquals("company-42", saved.getCompanyId());
        assertEquals("test.event", saved.getEventType());
        assertEquals("NEW", saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertEquals(FIXED_CLOCK.instant(), saved.getOccurredAtUtc());
    }

    @Test
    void enqueueWithoutCompanyIdFails() {
        EventEnvelope envelope = createEnvelope(null);

        assertThrows(IllegalArgumentException.class, () -> publisher.enqueue(envelope));
        assertEquals(0L, repository.count());
    }

    private EventEnvelope createEnvelope(String companyId) {
        ObjectNode payload = mapper.createObjectNode().put("payload", "value");

        EventEnvelope envelope = new EventEnvelope();
        envelope.setCompanyId(companyId);
        envelope.setType("test.event");
        envelope.setEventId("event-123");
        envelope.setPayload(payload);
        envelope.setTimestamp(Instant.now());
        return envelope;
    }
}
