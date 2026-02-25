package de.innologic.eventing.starter.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(NoopEventBus.class);

    @Override
    public void publish(String topic, Object envelope) {
        log.info("Noop publish to topic {} with payload {}", topic, envelope);
    }
}
