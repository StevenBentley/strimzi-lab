package io.strimzi;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TaxiProducerExample {
    private static final Logger log = LoggerFactory.getLogger(TaxiProducerExample.class);

    public static void main(String[] args) {
        SamplerConfiguration sampler = new SamplerConfiguration();
        sampler.withType("const");
        sampler.withParam(1);

        Tracer tracer = Configuration.fromEnv().withSampler(sampler).getTracer();
        GlobalTracer.registerIfAbsent(tracer);

        TaxiProducerConfig config = TaxiProducerConfig.fromMap(System.getenv());
        Properties props = TaxiProducerConfig.createProperties(config);

        try (InputStream is = TaxiProducerExample.class.getResourceAsStream(args[0]);
             BufferedReader taxiIn = new BufferedReader(new InputStreamReader(is))) {
            KafkaProducer<String, String> producer = new KafkaProducer<>(props);

            log.info("Sending data ...");
            int i = 0;
            while (taxiIn.ready()) {
                Span span = tracer.buildSpan("stream.message").start();
                try (Scope scope = tracer.scopeManager().activate(span)) {
                    log.info("Send message {}", ++i);
                    span.log(String.format("Send message %d", i));
                    producer.send(new ProducerRecord<>(config.getTopic(), taxiIn.readLine()));
                } catch(Exception ex) {
                    Tags.ERROR.set(span, true);
                    Map<String, Object> map = new HashMap<>();
                    map.put(Fields.EVENT, "error");
                    map.put(Fields.ERROR_OBJECT, ex);
                    map.put(Fields.MESSAGE, ex.getMessage());
                    span.log(map);
                } finally {
                    span.finish();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
