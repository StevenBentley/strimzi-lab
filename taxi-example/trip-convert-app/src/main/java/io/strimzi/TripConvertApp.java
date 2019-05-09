package io.strimzi;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.streams.TracingKafkaClientSupplier;
import io.opentracing.util.GlobalTracer;
import io.strimzi.json.JsonObjectSerde;
import io.strimzi.trip.Cell;
import io.strimzi.trip.Location;
import io.strimzi.trip.Trip;
import io.strimzi.trip.TripFields;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class TripConvertApp {
    private static final Logger log = LoggerFactory.getLogger(TripConvertApp.class);
    private static final Map<TripFields,Integer> fieldsMap = TaxiFieldsMap();

    private static final Location START_CELL_CENTRE = new Location(40.831164,-74.192491);
    private static final int CELL_SIZE_METRES = 500;
    private static final Double EARTH_RADIUS_METRES = 6371000.0;
    private static final Double CELL_LAT_LENGTH = (START_CELL_CENTRE.getLatitude() +
            (CELL_SIZE_METRES / EARTH_RADIUS_METRES) * (180 / Math.PI)) - START_CELL_CENTRE.getLatitude();
    private static final Double CELL_LONG_LENGTH = (START_CELL_CENTRE.getLongitude() +
            (CELL_SIZE_METRES / EARTH_RADIUS_METRES) * (180 / Math.PI) /
                    Math.cos(START_CELL_CENTRE.getLatitude() * Math.PI/180)) - START_CELL_CENTRE.getLongitude();
    private static final Location START_CELL_ORIGIN = startCellOrigin(); // Coordinates of top-left corner of cell 1.1
    private static final Location CELL_LENGTH = new Location(CELL_LAT_LENGTH, CELL_LONG_LENGTH);

    private static final int MAX_CLAT = 45;    // max latitude grid size
    private static final int MAX_CLONG = 72;   // max longitude grid size

    public static void main(String[] args) {
        SamplerConfiguration sampler = new SamplerConfiguration();
        sampler.withType("const");
        sampler.withParam(1);

        Tracer tracer = Configuration.fromEnv().withSampler(sampler).getTracer();
        GlobalTracer.registerIfAbsent(tracer);

        KafkaClientSupplier supplier = new TracingKafkaClientSupplier(tracer);

        TripConvertConfig config = TripConvertConfig.fromMap(System.getenv());
        Properties props = TripConvertConfig.createProperties(config);

        final JsonObjectSerde<Cell> cellSerde = new JsonObjectSerde<>(Cell.class);
        final JsonObjectSerde<Trip> tripSerde = new JsonObjectSerde<>(Trip.class);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> source = builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), Serdes.String()));
        KStream<Cell, Trip> mapped = source
                .map((key, value) -> {
                    log.info("Received message:");
                    log.info("\tvalue: {}", value);
                    Trip trip = constructTripFromString(value);
                    return new KeyValue<>(new Cell(START_CELL_ORIGIN, CELL_LENGTH, trip.getPickupLoc()),trip);
                })
                .filter((cell, trip) -> cell.inBounds(MAX_CLAT, MAX_CLONG));

        mapped.to(config.getSinkTopic(), Produced.with(cellSerde, tripSerde));

        final KafkaStreams streams = new KafkaStreams(builder.build(), props, supplier);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("trip-convert-shutdown-hook") {
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

    public static Trip constructTripFromString(String csv) {
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
