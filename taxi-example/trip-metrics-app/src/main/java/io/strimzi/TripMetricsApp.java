package io.strimzi;

import io.strimzi.trip.Cell;
import io.strimzi.trip.DoublePair;
import io.strimzi.trip.Trip;
import io.strimzi.json.JsonObjectSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TripMetricsApp {
    private static Logger log = LoggerFactory.getLogger(TripMetricsApp.class);

    public static void main(String[] args) {
        log.info("Start..");
        TripMetricsConfig config = TripMetricsConfig.fromEnv();
        Properties props = TripMetricsConfig.createConsumerProperties(config);

        final JsonObjectSerde<Cell> cellSerde = new JsonObjectSerde<>(Cell.class);
        final JsonObjectSerde<Trip> tripSerde = new JsonObjectSerde<>(Trip.class);
        final JsonObjectSerde<DoublePair> pairSerde = new JsonObjectSerde<>(DoublePair.class);

        StreamsBuilder builder = new StreamsBuilder();

        KStream<Cell, Trip> source = builder.stream(config.getSourceTopic(), Consumed.with(cellSerde, tripSerde));
        KStream<Windowed<Cell>, DoublePair> windowed = source
                .groupByKey(Serialized.with(cellSerde, tripSerde))
                .windowedBy(TimeWindows.of(TimeUnit.MINUTES.toMillis(15)))
                .aggregate(
                        () -> new DoublePair((double) 0, (double)0),
                        (key, record, profit) -> {
                            profit.setX(profit.getX() + 1);
                            profit.setY(profit.getY() + (record.getFareAmount() + record.getTipAmount()));
                            return profit;
                        },
                        Materialized.<Cell, DoublePair, WindowStore<Bytes, byte[]>>as("profit-store") /* state store name */
                                .withValueSerde(pairSerde))
                .toStream();

        windowed.foreach((key, value) -> log.info("key: {}, val:{}", key.key(), value));

        KStream<Cell, Double> average = windowed
                .map((cell, pair) -> new KeyValue<>(cell.key(), (double) Math.round((pair.getY())*100)/100));

        average.foreach((key, value) -> log.info("key: {}, val:{}", key, value));

        average.to(config.getSinkTopic(), Produced.with(cellSerde, Serdes.Double()));

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("trip-metrics-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);

    }



    }
