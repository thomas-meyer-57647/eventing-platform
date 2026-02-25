package de.innologic.eventing.outbox.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
}
