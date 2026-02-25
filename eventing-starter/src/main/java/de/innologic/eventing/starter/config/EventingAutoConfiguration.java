package de.innologic.eventing.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import de.innologic.eventing.outbox.jpa.OutboxPublisher;
import de.innologic.eventing.starter.dispatch.OutboxDispatcher;
import de.innologic.eventing.starter.event.EventBus;
import de.innologic.eventing.starter.event.NoopEventBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(EventingDispatcherProperties.class)
public class EventingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper eventingObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventingEventBus() {
        return new NoopEventBus();
    }

    @Bean
    @ConditionalOnClass(OutboxPublisher.class)
    @ConditionalOnMissingBean
    public OutboxPublisher eventingOutboxPublisher(OutboxEventRepository repository) {
        return new OutboxPublisher(repository);
    }

    @Bean
    @ConditionalOnProperty(prefix = "eventing.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OutboxDispatcher outboxDispatcher(EventingDispatcherProperties properties,
                                             EventBus eventBus,
                                             OutboxEventRepository repository,
                                             Environment environment,
                                             ObjectMapper mapper) {
        return new OutboxDispatcher(properties, eventBus, repository, environment, mapper);
    }
}
