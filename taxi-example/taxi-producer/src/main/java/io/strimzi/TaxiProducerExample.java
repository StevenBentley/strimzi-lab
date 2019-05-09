package io.strimzi;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class TaxiProducerExample {
    private static final Logger log = LoggerFactory.getLogger(TaxiProducerExample.class);

    public static void main(String[] args) {
        TaxiProducerConfig config = TaxiProducerConfig.fromMap(System.getenv());
        Properties props = TaxiProducerConfig.createProperties(config);

        try (InputStream is = TaxiProducerExample.class.getResourceAsStream(args[0]);
             BufferedReader taxiIn = new BufferedReader(new InputStreamReader(is))) {
            KafkaProducer<String, String> producer = new KafkaProducer<>(props);

            log.info("Sending data ...");
            while (taxiIn.ready()) {
                producer.send(new ProducerRecord<>(config.getTopic(), taxiIn.readLine()));
            }

            producer.close();
            log.info("All data sent ...");
        } catch (FileNotFoundException e) {
            log.error("Invalid / No data file provided.. {}", e.getMessage());
            e.printStackTrace();
        }  catch (IOException e) {
            log.error("IO error occurred reading from file.. {}", args[0]);
            e.printStackTrace();
        }
    }
}
