# taxi-example

A sample application using the NYC taxi dataset, also used in the DEBS 2015 Grand Challenge.

The intention is for the following application structure:

`DataFile <- KafkaConnect -> Source-Topic -> TripProducer (N replicas) -> Trip-Topic -> TripConsumer (N' replicas) ->
Sink`

* We can scale processing of the `Source-Topic` by increasing `TripProducer` replicas.
* The key-based distribution of `TripProducer` splits the data source across partitions
based on the `Cell` so that we can then use the Kafka Streams API to process cell specific data
* Temporarily using `taxi-producer` module until the `KafkaConnect` part is set up

### TODO

- [ ] Link up `KafkaConnect` with the first part of the pipeline, providing a method for obtaining the data file
- [x] `TripProducer` to accept data from the `Source-Topic`, convert to `Trip` using `Cell` as key,
and output to `Trip-Topic`
    - [x] Read incoming data from `Source-Topic`
    - [x] Implement `Cell` and `Trip` creation logic
    - [x] Set key as some combination of the particular grid `Cell`
    - [x] Stream data to `Trip-Topic`
- [ ] Implement `TripConsumer`
    - [ ] Read data from `Trip-Topic`
    - [ ] Write some custom logic using Kafka Streams API, with some interesting Cell based metrics,
likely windowing on the key
    - [ ] Decide on an appropriate sink for the data, and implement a solution