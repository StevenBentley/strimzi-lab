package io.strimzi;

import io.strimzi.trip.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class TripProducerExample {
    private static final Logger log = LoggerFactory.getLogger(TripProducerExample.class);
    private static final Map<TripFields,Integer> fieldsMap = TaxiFieldsMap();
    private static Set<TopicPartition> assignedTopicPartitions;

    private static final Location START_CELL_CENTRE = new Location(41.474937, -74.913585);
    private static final Double CELL_SIZE_MILES = 0.310685596118667; // 500 metres in miles
    private static final Double CELL_LAT_LENGTH = 2 * (CELL_SIZE_MILES / 69);
    private static final Double CELL_LONG_LENGTH = 2 * (CELL_LAT_LENGTH / Math.cos(START_CELL_CENTRE.getLatitude()));
    private static final Location START_CELL_ORIGIN = startCellOrigin(); // Coordinates of top-left corner of cell 1.1
    private static final Location CELL_LENGTH = new Location(CELL_LAT_LENGTH, CELL_LONG_LENGTH);

    private static final int MAX_CLAT = 300;    // max latitude grid size
    private static final int MAX_CLONG = 300;   // max longitude grid size

    public static void main(String[] args) {
        TripProducerConfig config = TripProducerConfig.fromEnv();
        Properties consumerProp = TripProducerConfig.createConsumerProperties(config);
        Properties producerProp = TripProducerConfig.createProducerProperties(config);

        Vertx vertx = Vertx.vertx();

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, consumerProp, String.class, String.class);
        KafkaProducer<JsonObject, JsonObject> producer = KafkaProducer.create(vertx, producerProp, JsonObject.class, JsonObject.class);

        consumer.handler(record -> {
            log.info("Received on topic={}, partition={}, offset={}, key={}, value={}",
                    record.topic(), record.partition(), record.offset(), record.key(), record.value());
            log.info("Received message:");
            log.info("\tkey: {}", record.key() == null ? "null" : record.key());
            log.info("\tpartition: {}", record.partition());
            log.info("\toffset: {}", record.offset());
            log.info("\tvalue: {}", record.value());
            Cell pickupCell = getPickupCellFromTrip(record.value());
            if (pickupCell.inBounds(MAX_CLAT, MAX_CLONG)) {
                log.info("\tpickupCell: {}", pickupCell);
                Trip trip = getTripFromString(record.value());
                log.info("\ttrip: {}", trip);
            producer.write(KafkaProducerRecord.create(config.getSinkTopic(),
                    JsonObject.mapFrom(pickupCell),
                    JsonObject.mapFrom(trip)));
            }
        });

        consumer.partitionsAssignedHandler(topicPartitions -> {
            assignedTopicPartitions = topicPartitions;
            TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
            String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", config.getGroupId(), topicPartition.getTopic(), topicPartition.getPartition());
            log.info(status);
        });

        consumer.subscribe(config.getSourceTopic());
    }

    private static Map<TripFields,Integer> TaxiFieldsMap() {
    Map<TripFields,Integer> fieldMap = new EnumMap<>(TripFields.class);
        for (int i = 0; i < TripFields.values().length; i++) {
            fieldMap.put(TripFields.values()[i], i);
        }
        return fieldMap;
    }

    private static Location startCellOrigin() {
        return new Location(START_CELL_CENTRE.getLatitude() + (CELL_LAT_LENGTH / 2),
                START_CELL_CENTRE.getLongitude() - (CELL_LONG_LENGTH / 2));
    }

    public static Cell getPickupCellFromTrip(String csv) {
        String[] elements = csv.split(",");
        Location pickupLoc = new Location(Double.parseDouble(elements[fieldsMap.get(TripFields.PICKUP_LATITUDE)]),
                                          Double.parseDouble(elements[fieldsMap.get(TripFields.PICKUP_LONGITUDE)]));
        return new Cell(START_CELL_ORIGIN, CELL_LENGTH, pickupLoc);
    }

    public static Trip getTripFromString(String csv) {
        String[] elements = csv.split(",");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd H:m:s");
        Date pickupTime = null;
        Date dropoffTime = null;
        try {
            pickupTime = format.parse(elements[fieldsMap.get(TripFields.PICKUP_DATETIME)]);
            dropoffTime = format.parse(elements[fieldsMap.get(TripFields.DROPOFF_DATETIME)]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Trip(elements[fieldsMap.get(TripFields.MEDALLION)],
                elements[fieldsMap.get(TripFields.HACK_LICENSE)],
                pickupTime,
                dropoffTime,
                Double.parseDouble(elements[fieldsMap.get(TripFields.TRIP_TIME)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.TRIP_DISTANCE)]),
                new Location(Double.parseDouble(elements[fieldsMap.get(TripFields.PICKUP_LATITUDE)]),
                        Double.parseDouble(elements[fieldsMap.get(TripFields.PICKUP_LONGITUDE)])),
                new Location(Double.parseDouble(elements[fieldsMap.get(TripFields.DROPOFF_LATITUDE)]),
                        Double.parseDouble(elements[fieldsMap.get(TripFields.DROPOFF_LONGITUDE)])),
                elements[fieldsMap.get(TripFields.PAYMENT_TYPE)].equals("CSH")
                        ? Trip.PaymentType.CSH : Trip.PaymentType.CRD,
                Double.parseDouble(elements[fieldsMap.get(TripFields.FARE_AMOUNT)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.SURCHARGE)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.MTA_TAX)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.TIP_AMOUNT)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.TOLLS_AMOUNT)]),
                Double.parseDouble(elements[fieldsMap.get(TripFields.TOTAL_AMOUNT)]));
    }



}
