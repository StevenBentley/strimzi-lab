# taxi-example

A sample application using the NYC taxi dataset, also used in the DEBS 2015 Grand Challenge.

The intention is for the following application structure:

`DataFile <- KafkaConnect -> Source-Topic -> CellProducer (N replicas) -> Cell-Topic -> CellConsumer (N' replicas) ->
Sink`

* We can scale processing of the `Source-Topic` by increasing `CellProducer` replicas.
* The key-based distribution of `CellProducer` splits the data source across partitions
based on the Cell, and so we can then use the Kafka Streams API to process cell specific data
* Temporarily using a producer image until the `KafkaConnect` part is set up

### TODO

- [ ] Link up `KafkaConnect` with the first part of the pipeline, providing a method for obtaining the data file
- [ ] CellProducer to accept data from the topic, convert to Cell's and output to second topic
    - [x] Read incoming data from `Source-Topic`
    - [ ] Implement Cell creation logic
    - [ ] Set key as some combination of the particular grid Cell
    - [ ] Stream data to `Cell-Topic`
- [ ] Implement `CellConsumer`
    - [ ] Read data from `Cell-Topic`
    - [ ] Write some custom logic using Kafka Streams API, with some interesting Cell based metrics,
likely Windowing on the key
    - [ ] Decide on an appropriate sink for the data, and implement a solution