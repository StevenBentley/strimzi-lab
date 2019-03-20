package io.strimzi;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class TripProducerConfig {

    private final String bootstrapServers;
    private final String sourceTopic;
    private final String sinkTopic;
    private final String groupId;
    private String acks = "1";
    private final String autoOffsetReset = "earliest";
    private final String enableAutoCommit = "false";

    public TripProducerConfig(String bootstrapServers, String sourceTopic, String sinkTopic, String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.sourceTopic = sourceTopic;
        this.sinkTopic = sinkTopic;
        this.groupId = groupId;
    }

    public static TripProducerConfig fromEnv() {
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");
        String sourceTopic = System.getenv("SOURCE_TOPIC");
        String sinkTopic = System.getenv("SINK_TOPIC");
        String groupId = System.getenv("GROUP_ID");

        return new TripProducerConfig(bootstrapServers, sourceTopic, sinkTopic, groupId);
    }

    public static Properties createConsumerProperties(TripProducerConfig config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.getEnableAutoCommit());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        return props;
    }

    public static Properties createProducerProperties(TripProducerConfig config) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.vertx.kafka.client.serialization.JsonObjectSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.vertx.kafka.client.serialization.JsonObjectSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, config.getAcks());

        return props;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public String getSinkTopic() {
        return sinkTopic;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getAcks() {
        return acks;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public String getEnableAutoCommit() {
        return enableAutoCommit;
    }
}
