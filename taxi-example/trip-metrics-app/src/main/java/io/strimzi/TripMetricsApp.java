package io.strimzi;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.streams.TracingKafkaClientSupplier;
import io.opentracing.util.GlobalTracer;
import io.strimzi.json.JsonObjectSerde;
import io.strimzi.trip.Cell;
import io.strimzi.trip.DoublePair;
import io.strimzi.trip.Trip;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class TripMetricsApp {
    private static Logger log = LoggerFactory.getLogger(TripMetricsApp.class);

    public static void main(String[] args) {
        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration();
        sampler.withType("const");
        sampler.withParam(1);

        Tracer tracer = Configuration.fromEnv().withSampler(sampler).getTracer();
        GlobalTracer.registerIfAbsent(tracer);

        KafkaClientSupplier supplier = new TracingKafkaClientSupplier(tracer);

        TripMetricsConfig config = TripMetricsConfig.fromMap(System.getenv());
        Properties props = TripMetricsConfig.createConsumerProperties(config);

        final JsonObjectSerde<Cell> cellSerde = new JsonObjectSerde<>(Cell.class);
        final JsonObjectSerde<Trip> tripSerde = new JsonObjectSerde<>(Trip.class);
        final JsonObjectSerde<DoublePair> pairSerde = new JsonObjectSerde<>(DoublePair.class);

        Topology topology = new Topology();

        topology.addSource("SOURCE", cellSerde.deserializer(), tripSerde.deserializer(), config.getSourceTopic())
            .addProcessor("PROCESS-1", () -> new Processor<Cell,Trip>() {
                ProcessorContext context;
                WindowStore<Cell, Trip> windowStore;

                @Override
                @SuppressWarnings("unchecked")
                public void init(ProcessorContext processorContext) {
                    this.context = processorContext;
                    this.windowStore = (WindowStore<Cell, Trip>) processorContext.getStateStore("profit-window");
                }

                @Override
                public void process(Cell cell, Trip trip) {
                    this.windowStore.put(cell, trip, trip.getPickupDatetime().getTime());
                    KeyValueIterator<Windowed<Cell>, Trip> iter = this.windowStore.all();

                    while(iter.hasNext()) {
                        KeyValue<Windowed<Cell>, Trip> entry = iter.next();
                        this.context.forward(entry.key, entry.value);
                    }
                }

                @Override
                public void close() {}
            }, "SOURCE")

            .addStateStore(Stores.windowStoreBuilder(
                    Stores.persistentWindowStore("profit-window", Duration.ofDays(1),Duration.ofMinutes(15),false),
                    cellSerde, tripSerde), "PROCESS-1")

            .addProcessor("PROCESS-2", () -> new Processor<Windowed<Cell>, Trip>() {
                ProcessorContext context;
                KeyValueStore<String, DoublePair> keyValueStore;

                @Override
                @SuppressWarnings("unchecked")
                public void init(ProcessorContext processorContext) {
                    this.context = processorContext;
                    this.keyValueStore = (KeyValueStore<String, DoublePair>) processorContext.getStateStore("profit-aggregate");
                }

                @Override
                public void process(Windowed<Cell> window, Trip trip) {
                    String winName = window.window().toString();
                    if (keyValueStore.get(winName) == null) {
                        keyValueStore.put(winName, new DoublePair((double) 1, (trip.getFareAmount() + trip.getTipAmount())));
                    } else {
                        DoublePair profit = keyValueStore.get(winName);
                        profit.setX(profit.getX() + 1);
                        profit.setY(profit.getY() + (trip.getFareAmount() + trip.getTipAmount()));
                        keyValueStore.put(winName, profit);
                        this.context.forward(window.key(), profit);
                    }
                }

                @Override
                public void close() {

                }
            }, "PROCESS-1")

        .addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("profit-aggregate"),
                Serdes.String(), pairSerde), "PROCESS-2");

        topology.addSink("SINK",config.getSinkTopic(), cellSerde.serializer(), pairSerde.serializer(), "PROCESS-2");

        final KafkaStreams streams = new KafkaStreams(topology, props, supplier);
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
