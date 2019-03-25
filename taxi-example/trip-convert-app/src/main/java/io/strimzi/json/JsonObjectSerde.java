package io.strimzi.json;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonObjectSerde<T> implements Serde<T> {
    private final JsonPOJOSerializer<T> SERIALIZER;
    private final JsonPOJODeserializer<T> DESERIALIZER;

    public JsonObjectSerde(Class<T> tClass) {
        SERIALIZER = new JsonPOJOSerializer<>();
        DESERIALIZER = new JsonPOJODeserializer<>(tClass);
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        SERIALIZER.configure(map, b);
        DESERIALIZER.configure(map, b);
    }

    @Override
    public void close() {
        SERIALIZER.close();
        DESERIALIZER.close();
    }

    @Override
    public Serializer<T> serializer() {
        return SERIALIZER;
    }

    @Override
    public Deserializer<T> deserializer() {
        return DESERIALIZER;
    }
}