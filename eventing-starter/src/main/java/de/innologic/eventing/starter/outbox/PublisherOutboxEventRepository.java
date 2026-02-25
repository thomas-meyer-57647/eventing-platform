package de.innologic.eventing.starter.outbox;

import de.innologic.eventing.outbox.jpa.OutboxEventEntity;
import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PublisherOutboxEventRepository extends OutboxEventRepository {

    @Query("select e from OutboxEventEntity e where e.status = :status and (e.nextAttemptAtUtc is null or e.nextAttemptAtUtc <= :cutoff) order by e.occurredAtUtc asc")
    List<OutboxEventEntity> findPendingEvents(@Param("status") String status, @Param("cutoff") Instant cutoff, Pageable pageable);
}
