package io.strimzi.json;

import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.serialization.JsonObjectSerializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonPOJOSerializer<T> implements Serializer<T> {
    private final JsonObjectSerializer SERIALIZER = new JsonObjectSerializer();

    @Override
    public void configure(Map<String, ?> map, boolean b) {}

    @Override
    public byte[] serialize(String s, T t) {
        JsonObject jsonObject = JsonObject.mapFrom(t);
        return SERIALIZER.serialize(s, jsonObject);
    }

    @Override
    public void close() {}
}
