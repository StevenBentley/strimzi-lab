package io.strimzi;

import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class TaxiProducerConfig {

    private final String bootstrapServers;
    private final String topic;
    private static final String ACKS = "1";

    public TaxiProducerConfig(String bootstrapServers, String topic) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    public static TaxiProducerConfig fromEnv() {
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");
        String topic = System.getenv("TOPIC");

        return new TaxiProducerConfig(bootstrapServers, topic);
    }

    public static Properties createProperties(TaxiProducerConfig config) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, ACKS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return props;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }
}
