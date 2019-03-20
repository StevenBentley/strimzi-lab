package io.strimzi;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class TaxiProducerExample {
    private static final Logger log = LoggerFactory.getLogger(TaxiProducerExample.class);

    public static void main(String[] args) {
        TaxiProducerConfig config = TaxiProducerConfig.fromEnv();
        Properties props = TaxiProducerConfig.createProperties(config);

        Vertx vertx = Vertx.vertx();

        try (BufferedReader taxiIn = new BufferedReader(new FileReader(args[0]))) {
            KafkaProducer<String,String> producer = KafkaProducer.create(vertx, props);

            log.info("Sending data ...");
            while (taxiIn.ready()) {
                producer.write(KafkaProducerRecord.create(config.getTopic(), taxiIn.readLine()));
                Thread.sleep(1000);
            }

            producer.close();
            log.info("All data sent ...");
        } catch (FileNotFoundException e) {
            log.error("Invalid / No data file provided.. {}", e.getMessage());
            e.printStackTrace();
        }  catch (IOException e) {
            log.error("IO error occurred reading from file.. {}", args[0]);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
