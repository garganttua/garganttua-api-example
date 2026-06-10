package com.garganttua.api.example;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.MimeType;
import com.garganttua.api.commons.serialization.ISerializer;
import com.garganttua.core.reflection.IClass;

/**
 * XML {@link ISerializer} for the example's server mode, registered for both
 * {@code application/xml} and {@code text/xml} (one instance per media type).
 *
 * <p>Why not the binding's {@code JacksonXmlSerializer}? That one uses a bare
 * {@link XmlMapper} with no Java-time module, so it throws a 500 on any non-null
 * {@link Instant} field (e.g. {@code Key.expiration}) — it only appears to work
 * when every temporal field happens to be null. This one adds the same tiny
 * ISO-8601 {@code Instant} module as {@link ExampleJsonSerializer}, avoiding the
 * {@code jackson-datatype-jsr310} dependency, and additionally covers the
 * {@code text/xml} alias the binding does not advertise.
 */
public class ExampleXmlSerializer implements ISerializer {

    private static final XmlMapper MAPPER = buildMapper();

    private final MimeType mimeType;

    public ExampleXmlSerializer(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    private static XmlMapper buildMapper() {
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
        XmlMapper mapper = new XmlMapper();
        mapper.registerModule(instants);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    @Override
    public MimeType mimeType() {
        return this.mimeType;
    }

    @Override
    public byte[] serialize(Object object) throws ApiException {
        try {
            return MAPPER.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new ApiException("XML serialize failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, IClass<T> type) throws ApiException {
        try {
            return (T) MAPPER.readValue(data, Class.forName(type.getName()));
        } catch (Exception e) {
            throw new ApiException("XML deserialize failed: " + e.getMessage(), e);
        }
    }
}
