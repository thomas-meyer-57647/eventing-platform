package de.innologic.eventing.outbox.jpa;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE outbox_event " +
            "SET status='PROCESSING', claimed_at_utc=UTC_TIMESTAMP(), claimed_by=:claimedBy " +
            "WHERE status='NEW' " +
            "  AND (next_attempt_at_utc IS NULL OR next_attempt_at_utc <= UTC_TIMESTAMP()) " +
            "ORDER BY occurred_at_utc " +
            "LIMIT :batchSize", nativeQuery = true)
    int claimDueNewEvents(@Param("claimedBy") String claimedBy,
                          @Param("batchSize") int batchSize);

    List<OutboxEventEntity> findByStatusAndClaimedByOrderByOccurredAtUtcAsc(String status, String claimedBy);
}
