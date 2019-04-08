package io.strimzi.trip;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

public class TripTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> consumerRecord, long l) {
        Trip trip = (Trip) consumerRecord.value();
        if (trip != null) {
            return trip.getPickupDatetime().getTime();
        } else {
            return System.currentTimeMillis();
        }
    }
}
