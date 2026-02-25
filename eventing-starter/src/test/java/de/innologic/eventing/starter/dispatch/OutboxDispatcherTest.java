package de.innologic.eventing.starter.dispatch;

import de.innologic.eventing.outbox.jpa.OutboxEventEntity;
import de.innologic.eventing.starter.config.EventingDispatcherProperties;
import de.innologic.eventing.starter.event.EventBus;
import de.innologic.eventing.starter.outbox.PublisherOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private PublisherOutboxEventRepository repository;

    @Mock
    private EventBus eventBus;

    private EventingDispatcherProperties properties;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new EventingDispatcherProperties();
        properties.setBatchSize(5);
        dispatcher = new OutboxDispatcher(properties, eventBus, repository, FIXED_CLOCK);
    }

    @Test
    void disabledDispatcherDoesNotPublish() {
        properties.setEnabled(false);

        dispatcher.dispatch();

        verifyNoInteractions(eventBus);
        verifyNoInteractions(repository);
    }

    @Test
    void successfulPublishMarksSent() {
        OutboxEventEntity event = newEvent("company-1");
        when(repository.findPendingEvents(eq(OutboxDispatcher.STATUS_NEW), eq(FIXED_CLOCK.instant()), any(Pageable.class)))
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
    }

    @Test
    void publishFailureIncrementsRetry() {
        properties.setMaxAttempts(3);
        OutboxEventEntity event = newEvent("company-2");
        when(repository.findPendingEvents(eq(OutboxDispatcher.STATUS_NEW), eq(FIXED_CLOCK.instant()), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), eq(event.getPayloadJson()));

        dispatcher.dispatch();

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertEquals("NEW", saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextAttemptAtUtc());
    }

    @Test
    void exceededRetriesMarksFailed() {
        properties.setMaxAttempts(1);
        OutboxEventEntity event = newEvent("company-3");
        when(repository.findPendingEvents(eq(OutboxDispatcher.STATUS_NEW), eq(FIXED_CLOCK.instant()), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("boom")).when(eventBus).publish(any(), eq(event.getPayloadJson()));

        dispatcher.dispatch();

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertEquals("FAILED", saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNull(saved.getNextAttemptAtUtc());
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
