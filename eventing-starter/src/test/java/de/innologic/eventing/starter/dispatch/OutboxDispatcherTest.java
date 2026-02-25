package de.innologic.eventing.starter.dispatch;

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

    private EventingDispatcherProperties properties;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new EventingDispatcherProperties();
        properties.setBatchSize(5);
        dispatcher = new OutboxDispatcher(properties, eventBus, repository, FIXED_CLOCK, DISPATCHER_ID);
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

        verify(eventBus, times(1)).publish(any(), eq(event.getPayloadJson()));
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
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), eq(event.getPayloadJson()));
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
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), eq(event.getPayloadJson()));
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
        return event;
    }
}
