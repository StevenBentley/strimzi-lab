---
title: "Building a Kafka Streams Application"
date: 08-04-2019
author: Adam Cattermole
---

# Building a Kafka Streams Application

Within this article we describe the steps required to get started with building your own Kafka Streams application on top of Strimzi. We start with a simple example and build upon it to create a multi-stage pipeline, performing some operations on the data stream, and visualising at the consumer.

## Dataset/Problem

The data we have chosen for this example is New York City taxi journey information from 2013, which was the dataset for the [Distributed Event Based Systems (DEBS) Grand Challenge in 2015](http://www.debs2015.org/call-grand-challenge.html). For a description of the source of the data, see [the article here](https://chriswhong.com/open-data/foil_nyc_taxi/).

The dataset is provided as a CSV file, with the columns detailed below:

| Column              | Description                                             |
| ------------------- | ------------------------------------------------------- |
| `medallion`         | an md5sum of the identifier of the taxi - vehicle bound |
| `hack_license`      | an md5sum of the identifier for the taxi license        |
| `pickup_datetime`   | time when the passenger(s) were picked up               |
| `dropoff_datetime`  | time when the passenger(s) were dropped off             |
| `trip_time_in_secs` | duration of the trip                                    |
| `trip_distance`     | trip distance in miles                                  |
| `pickup_longitude`  | longitude coordinate of the pickup location             |
| `pickup_latitude`   | latitude coordinate of the pickup location              |
| `dropoff_longitude` | longitude coordinate of the drop-off location           |
| `dropoff_latitude`  | latitude coordinate of the drop-off location            |
| `payment_type`      | the payment method - credit card or cash                |
| `fare_amount`       | fare amount in dollars                                  |
| `surcharge`         | surcharge in dollars                                    |
| `mta_tax`           | tax in dollars                                          |
| `tip_amount`        | tip in dollars                                          |
| `tolls_amount`      | bridge and tunnel tolls in dollars                      |
| `total_amount`      | total paid amount in dollars                            |

*source: DEBS 2015 Grand Challenge*

There are several different interesting avenues that could be explored within this dataset, for example:

- We could follow specific taxis to calculate the takings from one taxi throughout the course of a day, or calculate the distance from their last drop off to the next pickup to find out whether they are travelling far without a passenger
- By using the distance and time of the taxi trip we could calculate the average speed, and use the coordinates of the pickup and drop off to try to guess the amount of traffic encountered

We have picked a relatively simple example, where we can calculate the total amount of money (`fare_amount + tip_amount`) taken, based off of the pickup location of the journey. This involves splitting the input data into a grid of different cells, and summing the total amount of money taken for every journey that originates from any cell. To do this we have to consider splitting up processing in a way that ensures the correctness of our output.

We will build this example up step-by-step, starting with KafkaConnect and the Strimzi built-in `FileStreamSourceConnector`.

## Strimzi Setup

Follow the current [Strimzi quickstart documentation](https://strimzi.io/quickstarts/) to get the Kafka cluster deployed.

## Getting Data into the System

First things first, we need to make our dataset accessible from the cluster. The example connector that has been built relies on hosting the file on an FTP server. For convenience, we use a python library `pyftpdlib` to host the file with the username and password set to `strimzi`. For instructions on running the FTP server see [Running the Python FTP server](https://github.com/adam-cattermole/strimzi-lab/tree/add-taxi-example/taxi-example#running-the-python-ftp-server)

We have built a connector, which consists of both the connector itself and tasks. The tasks are invoked based on the configuration of `tasks.max`, and the `poll()` function is called to retrieve data. We use the [FTPConnection.java](../taxi-connect/src/main/java/io/strimzi/util/FTPConnection.java) class, which provides a wrapper to the existing Apache Commons [FTPClient](https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ftp/FTPClient.html). It should be mentioned however that invoking several tasks to stream data from the same file could result in undesirable behaviour and out of order messages, so we do not use the `maxTasks` parameter in `taskConfigs(..)`.

To deploy the connector, we simply have to build a new docker image based on the existing [strimzi/kafka-connect](https://hub.docker.com/r/strimzi/kafka-connect) image, copying the JAR from our build into the plugins folder - see the docs [here](https://strimzi.io/docs/master/#creating-new-image-from-base-str). We can then deploy the new image with the same configuration as the [existing KafkaConnect example](https://github.com/strimzi/strimzi-kafka-operator/blob/master/examples/kafka-connect/kafka-connect.yaml), adding the `spec.image` field to point to the correct image.

To check that the connector is present you can run the following to list all connector plugins:

```bash
oc exec -i my-cluster-kafka-0 -- curl -s -X GET \
    http://my-connect-cluster-connect-api:8083/connector-plugins
```

KafkaConnect is exposed as a RESTful resource, and so to create a new Connector we can `POST` the following. This creates a new `TaxiSourceConnector`, pointing to the FTP server, and providing the path to the file, exporting it to our new KafkaTopic `taxi-source-topic`. For this to work correctly, the following configuration needs to be set correctly

`connect.ftp.address` should be set to the IP address of the FTP server.

`connect.ftp.filepath` should be set to the path of the file to be read from the root of the FTP server.

```bash
oc exec -i my-cluster-kafka-0 -- curl -s -X POST \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://my-connect-cluster-connect-api:8083/connectors -d @- <<'EOF'

{
    "name": "taxi-connector",
    "config": {
        "connector.class": "io.strimzi.TaxiSourceConnector",
        "connect.ftp.address": "<ip-address>",
        "connect.ftp.user": "strimzi",
        "connect.ftp.password": "strimzi",
        "connect.ftp.filepath": "sorteddata.csv",
        "connect.ftp.topic": "taxi-source-topic",
        "tasks.max": "1",
        "value.converter": "org.apache.kafka.connect.storage.StringConverter"
    }
}
EOF
```

To check that the connector has successfully been created, check that it is listed here:

```bash
oc exec -i my-cluster-kafka-0 -- curl -s -X GET \
    http://my-connect-cluster-connect-api:8083/connectors
```

We can check that the data is streaming correctly by consuming events from the topic:

```bash
oc run kafka-consumer -ti --image=strimzi/kafka:0.11.1-kafka-2.1.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic taxi-source-topic --from-beginning
```

For debugging information, see the logs of `my-connect-cluster-connect`.

Lets delete the connector for now and start to look at the rest of the pipeline:

```bash
oc exec -i my-cluster-kafka-0 -- curl -s -X DELETE \
    http://my-connect-cluster-connect-api:8083/connectors/taxi-connector
```

## Kafka Streams Operations on the Data

Now that we have confirmed that the `String` data is present in the system, we can start to perform operations on the data. To start with, we need to configure the Kafka client with several different options - these are provided through use of the [TripConvertConfig.java](../trip-convert-app/src/main/java/io/strimzi/TripConvertConfig.java). Some configuration is passed in from the deployment YAML using environment variables, so that this is abstracted away from the application logic. For each new application that we develop we use the same method for providing configuration. Lets create the config options using this first:

```java
TripConvertConfig config = TripConvertConfig.fromEnv();
Properties props = TripConvertConfig.createProperties(config);
```

The KafkaStreams we have defined follow the same pattern, where we read from one KafkaTopic, perform some kind of operation on the data, and sink to another output KafkaTopic. Lets start by creating the source KafkaStream object, and define the SerDes (Serialisation/Deserialisation) for consuming data from this topic:

```java
StreamsBuilder builder = new StreamsBuilder();
KStream<String, String> source = builder.stream(config.getSourceTopic(), Consumed.with(Serdes.String(), Serdes.String()));
```

To perform any operations on the data, we need to convert the `<String, String>` (`<key, value>`) events into the type we require. We have created a Plain Old Java Object (POJO) representing the `Trip` data type, along with the `enum TripFields` representing the column of each data element. We provide a function `constructTripFromString` to take these CSV lines and convert them into this data type. The Kafka Streams DSL makes it easy to map over the events to convert to the type we require:

```java
KStream<String, Trip> mapped = source
                .map((key, value) -> {
                    new KeyValue<>(key, constructTripFromString(value))
                });
```

We could then write the output rom this operation to the sink topic, however the SerDes we use has changed for the value field. We require a method of performing this sort of operation on the custom data type we have created. This is where the [JsonObjectSerde.java](../trip-convert-app/src/main/java/io/strimzi/json/JsonObjectSerde.java) class comes into play. We are using the [Vertx JsonObject](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonObject.html) implementation, and including our class type in the constructor to save us doing the hard work, although a different implementation may be better suited to your application. The original `Trip` type only needs adjusting with appropriate `@JsonCreator` and `@JsonProperty` annotations. We are now ready to output to our KafkaTopic with the following:

```java
final JsonObjectSerde<Trip> tripSerde = new JsonObjectSerde<>(Trip.class);
mapped.to(config.getSinkTopic(), Produced.with(Serdes.String(), tripSerde));
```

### Application Specific Information

We wanted to calculate profitability of a particular cell by calculating which cell a particular journey originates from. This requires us to perform some calculations given the latitude and longitude of a `Trip`. We use the logic laid out in the DEBS Grand Challenge for defining the specifics of the grid. See the figure below for an example.

We use an origin point (blue point in the figure) representing the centre of the grid cell (1,1), and a size in metres of each cell in the grid. The cell size is converted into a latitude and longitude distance, `dy` and `dx` respectively, and the position of the top left of the grid is calculated (red point), which allows us to easily count how many of this distance away the incoming coordinates are, and therefore that it originates from cell (3,4).

<p align="center">
  <img src ="assets/taxi-grid.png" alt="Taxi Grid Example"/>
</p>

The additional application logic in the [Cell.java](../trip-convert-app/src/main/java/io/strimzi/trip/Cell.java) class and [TripConvertApp.java](../trip-convert-app/src/main/java/io/strimzi/TripConvertApp.java) perform this calculation, and we set the key of the new records as the `Cell` type, using a new SerDes created in an identical fashion to that of the `tripSerde`. This is important to ensure that every `Trip` corresponding to a particular pickup `Cell` are distributed to the same partition, and in turn the same processing node will receive this event, ensuring correctness and reproducibility of the operations.

## Aggregation

Now that we have data arriving as type `<Cell, Trip>` we would like to perform an aggregation operation. To keep things simple, we intend to calculate the sum of the `fare_amount + tip_amount` for every journey originating from one pickup cell within a set time period.

The first thing we need is to create a class that implements `TimestampExtractor`, which we can set in our configuration, as we want our time windows to be based on the time included in the events, instead of the time they entered the system. See the implementation in [TripTimestampExtractor.java](../trip-metrics-app/src/main/java/io/strimzi/trip/TripTimestampExtractor.java) for details.

We use `groupByKey` to ensure our records are partitioned by cell before we perform the subsequent window operation. The window size is easily changeable, for the time being though we have chosen a window of 15 minutes. Finally we provide an aggregate operation, where we provide the base accumulator value, and the function that we apply to each record. The output of this function is a type of `KTable`, where the keys correspond to a particular window, and the value is the profit value sum.

```java
KStream<Cell, Trip> source = builder.stream(config.getSourceTopic(), Consumed.with(cellSerde, tripSerde));
KStream<Windowed<Cell>, Double> windowed = source
        .groupByKey(Serialized.with(cellSerde, tripSerde))
        .windowedBy(TimeWindows.of(TimeUnit.MINUTES.toMillis(15)))
        .aggregate(
                () -> (double) 0,
                (key, record, profit) -> {
                    profit + record.getFareAmount() + record.getTipAmount()
                },
                Materialized.<Cell, Double, WindowStore<Bytes, byte[]>>as("profit-store")
                        .withValueSerde(Serdes.Double()))
        .toStream();
```

We use `toStream()` to convert it back to a `KStream`, and re-map the cell as the key, as well as rounding the profit to two decimal places.

```java
KStream<Cell, Double> rounded = windowed
                .map((cell, profit) -> new KeyValue<>(cell.key(), (double) Math.round(profit*100)/100));
```

The data can now be written to the output topic using the same method as in the previous application.

```java
rounded.to(config.getSinkTopic(), Produced.with(cellSerde, Serdes.Double()));
```

## Consume and Visualise

Now that we have the cell based profit metric being output to the final topic, we can write some code to consume and visualise the data. We are using the Vertx Kafka Consumer to consume the data from our topic, and we stream it to our JavaScript dashboard using Vertx EventBus and sockjs (WebSockets). See [TripConsumerApp.java](../trip-consumer-app/src/main/java/io/strimzi/TripConsumerApp.java) for the implementation.

We setup a `KafkaConsumer` instance and configure the handler to publish messages to a particular outbound EventBus channel. In JavaScript we can configure the EventBus connection and register a handler for messages arriving at the same address and perform an action to visualise the data. We publish the data as JSON to allow us to easily read the data in JavaScript later on. As the `Cell` key has already been serialised to a JSON string we can save some work by reading it as a `String` instead of parsing to a cell.

```java
KafkaConsumer<String, Double> consumer = KafkaConsumer.create(vertx, props, String.class, Double.class);
        consumer.handler(record -> {
            JsonObject json = new JsonObject();
            json.put("key", record.key());
            json.put("value", record.value());
            vertx.eventBus().publish("dashboard", json);
        });
```

We log the information in a window so that we can see the raw data, and use a geographical mapping library ([Leaflet](https://leafletjs.com/)) to draw the original cells, modifying the opacity based on the value of the metric.

<p align="center">
  <img src ="assets/dashboard.png" alt="Screenshot of Dashboard"/>
</p>

By modifying the starting latitude and longitude, or the cell size (in both [index.html](../trip-consumer-app/src/main/resources/webroot/index.html) and [TripConvertApp.java](../trip-convert-app/src/main/java/io/strimzi/TripConvertApp.java)) you can change the grid that is being worked with. You can also adjust the logic in the aggregate function to calculate some alternative metric from the data.

## System Design

Below you can find a figure showing the overall architecture of the pipeline

<p align=center>
  <img src="assets/taxi-implementation.png" alt="Application Structure">
</p>
