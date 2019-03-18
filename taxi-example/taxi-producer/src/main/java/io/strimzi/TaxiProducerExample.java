package io.strimzi;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
        log.info("Start..");
        TaxiProducerConfig config = TaxiProducerConfig.fromEnv();
        Properties props = TaxiProducerConfig.createProperties(config);

        try (BufferedReader taxiIn = new BufferedReader(new FileReader(args[0]));
             KafkaProducer producer = new KafkaProducer(props)) {
            log.info("Sending data ...");
            while (taxiIn.ready()) {
                producer.send(new ProducerRecord(config.getTopic(), taxiIn.readLine()));
                Thread.sleep(1000);
            }
            log.info("All data sent ...");
        } catch (FileNotFoundException e) {
            log.error("Invalid / No data file provided.. {}", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            log.error("IO error occurred reading from file.. {}", args[0]);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
