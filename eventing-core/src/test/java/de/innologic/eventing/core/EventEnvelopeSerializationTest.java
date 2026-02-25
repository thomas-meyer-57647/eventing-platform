package de.innologic.eventing.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventEnvelopeSerializationTest {

    @Test
    void jacksonSerializeAndDeserializePreservesFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JsonNode payload = mapper.readTree("{\"foo\":\"bar\"}");

        EventEnvelope before = new EventEnvelope();
        before.setEventId(UUID.randomUUID().toString());
        before.setType("test.event");
        before.setCompanyId("company-123");
        before.setTimestamp(Instant.parse("2026-02-25T12:00:00Z"));
        before.setPayload(payload);
        before.setCorrelationId("corr-1");
        before.setCausationId("cause-1");

        String json = mapper.writeValueAsString(before);
        EventEnvelope after = mapper.readValue(json, EventEnvelope.class);

        assertEquals(before.getEventId(), after.getEventId());
        assertEquals(before.getType(), after.getType());
        assertEquals(before.getCompanyId(), after.getCompanyId());
        assertEquals(before.getTimestamp(), after.getTimestamp());
        assertEquals(before.getPayload(), after.getPayload());
        assertEquals(before.getCorrelationId(), after.getCorrelationId());
        assertEquals(before.getCausationId(), after.getCausationId());
        assertEquals(1, after.getSchemaVersion());
    }
}
