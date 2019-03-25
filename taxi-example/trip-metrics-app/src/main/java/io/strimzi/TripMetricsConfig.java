package io.strimzi;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Properties;

public class TripMetricsConfig {

    private final String bootstrapServers;
    private final String sourceTopic;
    private final String sinkTopic;
    private final String groupId;
    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final String ENABLE_AUTO_COMMIT = "false";

    public TripMetricsConfig(String bootstrapServers, String sourceTopic, String sinkTopic, String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.sourceTopic = sourceTopic;
        this.sinkTopic = sinkTopic;
        this.groupId = groupId;
    }

    public static TripMetricsConfig fromEnv() {
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");
        String sourceTopic = System.getenv("SOURCE_TOPIC");
        String sinkTopic = System.getenv("SINK_TOPIC");
        String groupId = System.getenv("GROUP_ID");

        return new TripMetricsConfig(bootstrapServers, sourceTopic, sinkTopic, groupId);
    }

    public static Properties createConsumerProperties(TripMetricsConfig config) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getGroupId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

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
