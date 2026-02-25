package de.innologic.eventing.outbox.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.innologic.eventing.core.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(repository);
    }

    @Test
    void enqueueWithCompanyIdCallsSaveOnce() {
        EventEnvelope envelope = createEnvelope("company-1");
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        publisher.enqueue(envelope);

        verify(repository, times(1)).save(any(OutboxEventEntity.class));
    }

    @Test
    void enqueueWithoutCompanyIdThrows() {
        EventEnvelope envelope = createEnvelope(null);

        assertThrows(IllegalArgumentException.class, () -> publisher.enqueue(envelope));

        verifyNoInteractions(repository);
    }

    private EventEnvelope createEnvelope(String companyId) {
        ObjectNode payload = mapper.createObjectNode().put("foo", "bar");

        EventEnvelope envelope = new EventEnvelope();
        envelope.setCompanyId(companyId);
        envelope.setType("example.event");
        envelope.setEventId("event-1");
        envelope.setPayload(payload);
        envelope.setTimestamp(Instant.now());
        return envelope;
    }
}
