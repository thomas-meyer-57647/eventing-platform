package de.innologic.eventing.starter.dispatch;

import de.innologic.eventing.outbox.jpa.ProcessedEventEntity;
import de.innologic.eventing.outbox.jpa.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerIdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository repository;

    private ConsumerIdempotencyService service;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-25T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new ConsumerIdempotencyService(repository, FIXED_CLOCK);
    }

    @Test
    void alreadyProcessedDelegatesToRepository() {
        when(repository.exists(any(Example.class))).thenReturn(true);

        assertTrue(service.alreadyProcessed("company-1", "consumer-a", "event-1"));

        ArgumentCaptor<Example<ProcessedEventEntity>> captor = ArgumentCaptor.forClass(Example.class);
        verify(repository).exists(captor.capture());
        ProcessedEventEntity captured = captor.getValue().getProbe();
        assertNotNull(captured);
        assertNotNull(captured.getCompanyId());
    }

    @Test
    void markProcessedSavesEntity() {
        service.markProcessed("company-1", "consumer-a", "event-2");

        ArgumentCaptor<ProcessedEventEntity> captor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(repository).save(captor.capture());
        ProcessedEventEntity saved = captor.getValue();
        assertEquals("company-1", saved.getCompanyId());
        assertEquals("consumer-a", saved.getConsumerName());
        assertEquals("event-2", saved.getEventId());
        assertNotNull(saved.getProcessedAtUtc());
    }

    @Test
    void markProcessedSwallowsDuplicate() {
        doThrow(new DataIntegrityViolationException("dup")).when(repository).save(any(ProcessedEventEntity.class));

        service.markProcessed("company-1", "consumer-a", "event-3");

        verify(repository).save(any(ProcessedEventEntity.class));
    }
}
