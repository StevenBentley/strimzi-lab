package io.strimzi;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Map;
import java.util.Properties;

public class TripConvertConfig {

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_SOURCE_TOPIC = "taxi-source-topic";
    private static final String DEFAULT_SINK_TOPIC = "taxi-trip-topic";
    private static final String DEFAULT_GROUP_ID = "trip-convert-app";

    private static final String BOOTSTRAP_SERVERS = "BOOTSTRAP_SERVERS";
    private static final String SOURCE_TOPIC = "SOURCE_TOPIC";
    private static final String SINK_TOPIC = "SINK_TOPIC";
    private static final String GROUP_ID = "GROUP_ID";

    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final String ENABLE_AUTO_COMMIT = "false";
    private static final int CACHE_MAX_BYTES_BUFFERING = 0;

    private final String bootstrapServers;
    private final String sourceTopic;
    private final String sinkTopic;
    private final String groupId;

    public TripConvertConfig(String bootstrapServers, String sourceTopic, String sinkTopic, String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.sourceTopic = sourceTopic;
        this.sinkTopic = sinkTopic;
        this.groupId = groupId;
    }

    public static TripConvertConfig fromMap(Map<String,String> map) {
        String bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS, DEFAULT_BOOTSTRAP_SERVERS);
        String sourceTopic = map.getOrDefault(SOURCE_TOPIC, DEFAULT_SOURCE_TOPIC);
        String sinkTopic = map.getOrDefault(SINK_TOPIC, DEFAULT_SINK_TOPIC);
        String groupId = map.getOrDefault(GROUP_ID, DEFAULT_GROUP_ID);
        return new TripConvertConfig(bootstrapServers, sourceTopic, sinkTopic, groupId);
    }

    public static Properties createProperties(TripConvertConfig config) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getGroupId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, CACHE_MAX_BYTES_BUFFERING);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

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
}
