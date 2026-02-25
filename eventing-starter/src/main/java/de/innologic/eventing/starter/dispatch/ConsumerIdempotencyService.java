package de.innologic.eventing.starter.dispatch;

import de.innologic.eventing.outbox.jpa.ProcessedEventEntity;
import de.innologic.eventing.outbox.jpa.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ConsumerIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerIdempotencyService.class);
    private final ProcessedEventRepository repository;
    private final Clock clock;

    public ConsumerIdempotencyService(ProcessedEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ConsumerIdempotencyService(ProcessedEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public boolean alreadyProcessed(String companyId, String consumerName, String eventId) {
        ProcessedEventEntity probe = new ProcessedEventEntity();
        probe.setCompanyId(companyId);
        probe.setConsumerName(consumerName);
        probe.setEventId(eventId);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnorePaths("id", "processedAtUtc");
        Example<ProcessedEventEntity> example = Example.of(probe, matcher);
        return repository.exists(example);
    }

    public void markProcessed(String companyId, String consumerName, String eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setCompanyId(companyId);
        entity.setConsumerName(consumerName);
        entity.setEventId(eventId);
        entity.setProcessedAtUtc(Instant.now(clock));

        try {
            repository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Processed event already recorded: {}/{}/{}", companyId, consumerName, eventId);
        }
    }
}
