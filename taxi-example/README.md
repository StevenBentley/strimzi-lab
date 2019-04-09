# taxi-example

A sample application using the NYC taxi dataset, also used in the DEBS 2015 Grand Challenge.

The intention is for the following application structure:

`DataFile <- KafkaConnect -> Source-Topic -> TripConvertApp -> Trip-Cell-Topic -> TripMetricsApp -> Cell-Profit-Topic -> TripConsumerApp`

