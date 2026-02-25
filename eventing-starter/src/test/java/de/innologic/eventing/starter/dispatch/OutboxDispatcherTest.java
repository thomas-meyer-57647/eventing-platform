package de.innologic.eventing.starter.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.eventing.core.EventEnvelope;
import de.innologic.eventing.outbox.jpa.OutboxEventEntity;
import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import de.innologic.eventing.starter.config.EventingDispatcherProperties;
import de.innologic.eventing.starter.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-25T00:00:00Z"), ZoneOffset.UTC);
    private static final String DISPATCHER_ID = "test-dispatcher";

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private EventBus eventBus;

    @Mock
    private Environment environment;

    private EventingDispatcherProperties properties;
    private OutboxDispatcher dispatcher;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new EventingDispatcherProperties();
        properties.setBatchSize(5);
        when(environment.getProperty(eq("spring.application.name"), eq("unknown"))).thenReturn("starter");
        dispatcher = new OutboxDispatcher(properties, eventBus, repository, FIXED_CLOCK, environment, mapper, DISPATCHER_ID);
    }

    @Test
    void disabledDispatcherDoesNotPublish() {
        properties.setEnabled(false);

        dispatcher.dispatch();

        verifyNoInteractions(eventBus);
        verifyNoInteractions(repository);
    }

    @Test
    void claimsZeroEventsSkipsPublish() {
        when(repository.claimDueNewEvents(any(), eq(properties.getBatchSize()))).thenReturn(0);

        dispatcher.dispatch();

        verifyNoInteractions(eventBus);
        verify(repository, times(0)).save(any());
    }

    @Test
    void claimReturnsTwoEventsPublishesTwice() {
        OutboxEventEntity first = newEvent("company-1");
        OutboxEventEntity second = newEvent("company-2");
        when(repository.claimDueNewEvents(any(), eq(properties.getBatchSize()))).thenReturn(2);
        when(repository.findByStatusAndClaimedByOrderByOccurredAtUtcAsc(
                eq(OutboxDispatcher.STATUS_PROCESSING), any()))
                .thenReturn(List.of(first, second));
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatch();

        verify(eventBus, times(2)).publish(any(), any());
        verify(repository, times(2)).save(any(OutboxEventEntity.class));
    }

    @Test
    void successfulPublishMarksSent() {
        OutboxEventEntity event = newEvent("company-3");
        when(repository.claimDueNewEvents(any(), eq(properties.getBatchSize())))
                .thenReturn(1);
        when(repository.findByStatusAndClaimedByOrderByOccurredAtUtcAsc(
                eq(OutboxDispatcher.STATUS_PROCESSING), any()))
                .thenReturn(List.of(event));
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatch();

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventBus, times(1)).publish(any(), envelopeCaptor.capture());
        EventEnvelope envelope = envelopeCaptor.getValue();
        assertEquals(event.getEventType(), envelope.getType());
        assertEquals(event.getCompanyId(), envelope.getCompanyId());
        assertEquals(event.getEventId(), envelope.getEventId());
        assertNotNull(envelope.getPayload());
        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository, times(1)).save(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertEquals("SENT", saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertNull(saved.getNextAttemptAtUtc());
        assertNull(saved.getClaimedBy());
    }

    @Test
    void publishFailureIncrementsRetry() {
        properties.setMaxAttempts(3);
        OutboxEventEntity event = newEvent("company-4");
        when(repository.claimDueNewEvents(any(), eq(properties.getBatchSize())))
                .thenReturn(1);
        when(repository.findByStatusAndClaimedByOrderByOccurredAtUtcAsc(
                eq(OutboxDispatcher.STATUS_PROCESSING), any()))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), any(EventEnvelope.class));
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatch();

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertEquals("NEW", saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextAttemptAtUtc());
        assertNull(saved.getClaimedBy());
    }

    @Test
    void exceededRetriesMarksFailed() {
        properties.setMaxAttempts(1);
        OutboxEventEntity event = newEvent("company-5");
        when(repository.claimDueNewEvents(any(), eq(properties.getBatchSize())))
                .thenReturn(1);
        when(repository.findByStatusAndClaimedByOrderByOccurredAtUtcAsc(
                eq(OutboxDispatcher.STATUS_PROCESSING), any()))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), any(EventEnvelope.class));
        when(repository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcher.dispatch();

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertEquals("FAILED", saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNull(saved.getNextAttemptAtUtc());
        assertNull(saved.getClaimedBy());
    }

    private OutboxEventEntity newEvent(String companyId) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setCompanyId(companyId);
        event.setEventType("order.created");
        event.setPayloadJson("{\"foo\":\"bar\"}");
        event.setStatus(OutboxDispatcher.STATUS_NEW);
        event.setRetryCount(0);
        event.setOccurredAtUtc(Instant.parse("2026-02-25T00:00:00Z"));
        return event;
    }
}
