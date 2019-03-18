package io.strimzi;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


public class TaxiConsumerExample {
    private static final Logger log = LoggerFactory.getLogger(TaxiConsumerExample.class);
    private static final Map<TripFields,Integer> fieldsMap = tripFieldsMap();
    private static Set<TopicPartition> assignedTopicPartitions;
    private final Location CELL_1_ORIGIN = new Location(41.474937, -74.913585);

    public static void main(String[] args) {
        log.info("Start..");
        io.strimzi.TaxiConsumerConfig config = io.strimzi.TaxiConsumerConfig.fromEnv();
        Properties props = io.strimzi.TaxiConsumerConfig.createProperties(config);

        Vertx vertx = Vertx.vertx();

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, props, String.class, String.class);

        consumer.handler(record -> {
            log.info("Received on topic={}, partition={}, offset={}, key={}, value={}",
                    record.topic(), record.partition(), record.offset(), record.key(), record.value());
            log.info("Received message:");
            log.info("\tkey: {}", record.key() == null ? "null" : record.key());
            log.info("\tpartition: {}", record.partition());
            log.info("\toffset: {}", record.offset());
            log.info("\tvalue: {}", record.value());
        });

        consumer.partitionsAssignedHandler(topicPartitions -> {
            assignedTopicPartitions = topicPartitions;
            TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
            String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", config.getGroupId(), topicPartition.getTopic(), topicPartition.getPartition());
            log.info(status);
        });

        consumer.subscribe(config.getTopic());
    }

    private static Map<TripFields,Integer> tripFieldsMap() {
    Map<TripFields,Integer> fieldMap = new EnumMap<>(TripFields.class);
        for (int i = 0; i < TripFields.values().length; i++) {
            fieldMap.put(TripFields.values()[i], i);
        }
        return fieldMap;
    }

    public enum TripFields {
        MEDALLION,
        HACK_LICENSE,
        PICKUP_DATETIME,
        DROPOFF_DATETIME,
        TRIP_TIME,          //(seconds)
        TRIP_DISTANCE,      //(miles)
        PICKUP_LONGITUDE,
        PICKUP_LATITUDE,
        DROPOFF_LONGITUDE,
        DROPOFF_LATITUDE,
        PAYMENT_TYPE,       //(CSH/CRD)
        FARE_AMOUNT,        //(dollars)
        SURCHARGE,          //(dollars)
        MTA_TAX,            //(dollars)
        TIP_AMOUNT,         //(dollars)
        TOLLS_AMOUNT,       //(dollars
        TOTAL_AMOUNT        //(dollars)
    }

    public class Location {
        private Double latitude;
        private Double longitude;

        public Location(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }
    }

    public class Cell {
        private int clat;
        private int clong;

        public Cell(int clat, int clong) {
            this.clat = clat;
            this.clong = clong;
        }

        public int getClat() {
            return clat;
        }

        public int getClong() {
            return clong;
        }
    }
}
