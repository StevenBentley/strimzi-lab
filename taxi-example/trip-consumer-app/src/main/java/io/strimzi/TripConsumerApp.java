package io.strimzi;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Set;

public class TripConsumerApp {
    private static final Logger log = LoggerFactory.getLogger(TripConsumerApp.class);

    private static final String STATUS = "status";
    private static Set<TopicPartition> assignedTopicPartitions;


    public static void main(String[] args) {
        TripConsumerConfig config = TripConsumerConfig.fromMap(System.getenv());
        Properties props = TripConsumerConfig.createProperties(config);

        Vertx vertx = Vertx.vertx();

        Router router = Router.router(vertx);

        BridgeOptions options = new BridgeOptions();
        options
                .addOutboundPermitted(new PermittedOptions().setAddress("dashboard"))
                .addOutboundPermitted(new PermittedOptions().setAddress("status"))
                .addInboundPermitted(new PermittedOptions().setAddress("config"));

        router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));
        router.route().handler(StaticHandler.create().setCachingEnabled(false));

        HttpServer httpServer = vertx.createHttpServer();
        httpServer
                .requestHandler(router::accept)
                .listen(8080, done -> {

                    if (done.succeeded()) {
                        log.info("HTTP server started on port {}", done.result().actualPort());
                    } else {
                        log.error("HTTP server not started", done.cause());
                    }
                });

        KafkaConsumer<String, Double> consumer = KafkaConsumer.create(vertx, props, String.class, Double.class);
        consumer.handler(record -> {
            log.info("Received on topic={}, partition={}, offset={}, key={}, value={}",
                    record.topic(), record.partition(), record.offset(), record.key(), record.value());
            JsonObject json = new JsonObject();
            json.put("key", record.key());
            json.put("value", record.value());
            vertx.eventBus().publish("dashboard", json);
        });

        consumer.partitionsAssignedHandler(topicPartitions -> {
            assignedTopicPartitions = topicPartitions;
            TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
            String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", config.getGroupId(), topicPartition.getTopic(), topicPartition.getPartition());
            vertx.eventBus().publish("status", status);
        });

        consumer.subscribe(config.getTopic());

        vertx.eventBus().consumer("config", message -> {

            String body = message.body().toString();

            switch (body) {

                case STATUS:

                    if (assignedTopicPartitions != null) {
                        TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
                        String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", config.getGroupId(), topicPartition.getTopic(), topicPartition.getPartition());
                        vertx.eventBus().publish("status", status);
                    } else {
                        vertx.eventBus().publish("status", String.format("Joining group = [%s] ...", config.getGroupId()));
                    }
                    break;
            }
        });

    }
}
