package de.innologic.eventing.core;

public final class TopicNaming {

    private TopicNaming() {
        // prevent instantiation
    }

    public static String topic(String companyId, String service, String eventType) {
        return companyId + "." + service + "." + eventType;
    }
}
