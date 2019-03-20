package io.strimzi.trip;

public enum TripFields {
    MEDALLION,
    HACK_LICENSE,
    PICKUP_DATETIME,
    DROPOFF_DATETIME,
    TRIP_TIME,          //(seconds)
    TRIP_DISTANCE,      //(miles)
    PICKUP_LONGITUDE,
    PICKUP_LATITUDE,
    DROPOFF_LONGITUDE,
    DROPOFF_LATITUDE,
    PAYMENT_TYPE,       //(CSH/CRD)
    FARE_AMOUNT,        //(dollars)
    SURCHARGE,          //(dollars)
    MTA_TAX,            //(dollars)
    TIP_AMOUNT,         //(dollars)
    TOLLS_AMOUNT,       //(dollars)
    TOTAL_AMOUNT        //(dollars)
}
