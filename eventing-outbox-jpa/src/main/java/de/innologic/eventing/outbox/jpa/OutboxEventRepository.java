package de.innologic.eventing.outbox.jpa;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "update outbox_event set status = 'PROCESSING', claimed_at = :now, claimed_by = :owner " +
            "where status = 'NEW' and (next_attempt_at_utc is null or next_attempt_at_utc <= :now) " +
            "order by occurred_at_utc limit :batchSize", nativeQuery = true)
    int claimPendingEvents(@Param("now") Instant now,
                            @Param("owner") String owner,
                            @Param("batchSize") int batchSize);

    List<OutboxEventEntity> findByStatusAndClaimedByOrderByOccurredAtUtcAsc(String status,
                                                                           String claimedBy,
                                                                           Pageable pageable);
}
