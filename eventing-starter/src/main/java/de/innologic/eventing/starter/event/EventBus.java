package de.innologic.eventing.starter.event;

public interface EventBus {

    void publish(String topic, Object envelope);
}
