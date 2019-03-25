package io.strimzi.json;

import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.serialization.JsonObjectDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsonPOJODeserializer<T> implements Deserializer<T> {
    private static final Logger log = LoggerFactory.getLogger(JsonPOJODeserializer.class);

    private final JsonObjectDeserializer DESERIALIZER = new JsonObjectDeserializer();

    private Class<T> tClass;

    public JsonPOJODeserializer(Class<T> tClass) {
        this.tClass = tClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> map, boolean b) {}

    @Override
    public T deserialize(String s, byte[] bytes) {
        JsonObject jsonObject = DESERIALIZER.deserialize(s, bytes);
        return jsonObject.mapTo(tClass);
    }

    @Override
    public void close() {}
}