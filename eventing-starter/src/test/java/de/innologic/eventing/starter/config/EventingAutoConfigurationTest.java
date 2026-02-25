package de.innologic.eventing.starter.config;

import de.innologic.eventing.outbox.jpa.OutboxEventRepository;
import de.innologic.eventing.starter.dispatch.OutboxDispatcher;
import de.innologic.eventing.starter.event.EventBus;
import de.innologic.eventing.starter.event.NoopEventBus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OutboxEventRepository.class, () -> mock(OutboxEventRepository.class))
            .withConfiguration(AutoConfigurations.of(EventingAutoConfiguration.class));

    @Test
    void autoConfigurationRegistersBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(EventingDispatcherProperties.class);
            assertThat(ctx).hasSingleBean(EventBus.class);
            assertThat(ctx.getBean(EventBus.class)).isInstanceOf(NoopEventBus.class);
            assertThat(ctx).hasSingleBean(OutboxDispatcher.class);
        });
    }

    @Test
    void dispatcherDisabledWhenPropertyFalse() {
        contextRunner.withPropertyValues("eventing.dispatcher.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OutboxDispatcher.class));
    }
}
