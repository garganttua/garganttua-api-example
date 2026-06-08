package com.garganttua.api.example;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.MimeType;
import com.garganttua.api.commons.serialization.ISerializer;
import com.garganttua.core.reflection.IClass;

/**
 * Minimal JSON {@link ISerializer} for the example's server mode.
 *
 * <p>The framework ships the {@code ISerializer} SPI but no concrete production
 * serializer yet (the {@code garganttua-api-binding-jackson} module is an empty
 * WIP), so a consumer provides its own and registers it with
 * {@code builder.serializer(...)}. This one uses Jackson (already on the
 * classpath via the Javalin binding) with a tiny module that renders
 * {@link Instant} as an ISO-8601 string — avoiding the extra
 * {@code jackson-datatype-jsr310} dependency.
 */
public class ExampleJsonSerializer implements ISerializer {

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        SimpleModule instants = new SimpleModule();
        instants.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider sp) throws IOException {
                gen.writeString(value.toString());
            }
        });
        instants.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return Instant.parse(p.getValueAsString());
            }
        });
        return new ObjectMapper()
                .registerModule(instants)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public MimeType mimeType() {
        return MimeType.APPLICATION_JSON;
    }

    @Override
    public byte[] serialize(Object object) throws ApiException {
        try {
            return MAPPER.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new ApiException("JSON serialize failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, IClass<T> type) throws ApiException {
        try {
            return (T) MAPPER.readValue(data, Class.forName(type.getName()));
        } catch (Exception e) {
            throw new ApiException("JSON deserialize failed: " + e.getMessage(), e);
        }
    }
}
