package io.strimzi;

import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Map;
import java.util.Properties;

public class TripConsumerConfig {

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "cell-profit-topic";
    private static final String DEFAULT_GROUP_ID = "trip-consumer-app";

    private static final String BOOTSTRAP_SERVERS = "BOOTSTRAP_SERVERS";
    private static final String TOPIC = "TOPIC";
    private static final String GROUP_ID = "GROUP_ID";

    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final String ENABLE_AUTO_COMMIT = "false";

    private final String bootstrapServers;
    private final String topic;
    private final String groupId;

    public TripConsumerConfig(String bootstrapServers, String topic, String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.groupId = groupId;
    }

    public static TripConsumerConfig fromMap(Map<String,String> map) {
        String bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS, DEFAULT_BOOTSTRAP_SERVERS);
        String topic = map.getOrDefault(TOPIC, DEFAULT_TOPIC);
        String groupId = map.getOrDefault(GROUP_ID, DEFAULT_GROUP_ID);

        return new TripConsumerConfig(bootstrapServers, topic, groupId);
    }

    public static Properties createProperties(TripConsumerConfig config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        return props;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }

    public String getGroupId() {
        return groupId;
    }

}
