package io.strimzi.trip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class Trip implements Serializable {
    private String medallion;
    private String hackLicense;
    private Date pickupDatetime;
    private Date dropoffDatetime;
    private Double tripTime;
    private Double tripDistance;
    private Location pickupLoc;
    private Location dropoffLoc;
    private PaymentType paymentType;
    private Double fareAmount;
    private Double surcharge;
    private Double mtaTax;
    private Double tipAmount;
    private Double tollsAmount;
    private Double totalAmount;

    @JsonCreator
    public Trip(@JsonProperty("medallion") String medallion,
                @JsonProperty("hackLicense") String hackLicense,
                @JsonProperty("pickupDatetime") Date pickupDatetime,
                @JsonProperty("dropoffDatetime") Date dropoffDatetime,
                @JsonProperty("tripTime") Double tripTime,
                @JsonProperty("tripDistance") Double tripDistance,
                @JsonProperty("pickupLoc") Location pickupLoc,
                @JsonProperty("dropoffLoc") Location dropoffLoc,
                @JsonProperty("paymentType") PaymentType paymentType,
                @JsonProperty("fareAmount") Double fareAmount,
                @JsonProperty("surcharge") Double surcharge,
                @JsonProperty("mtaTax") Double mtaTax,
                @JsonProperty("tipAmount") Double tipAmount,
                @JsonProperty("tollsAmount") Double tollsAmount,
                @JsonProperty("totalAmount") Double totalAmount) {
        this.medallion = medallion;
        this.hackLicense = hackLicense;
        this.pickupDatetime = pickupDatetime;
        this.dropoffDatetime = dropoffDatetime;
        this.tripTime = tripTime;
        this.tripDistance = tripDistance;
        this.pickupLoc = pickupLoc;
        this.dropoffLoc = dropoffLoc;
        this.paymentType = paymentType;
        this.fareAmount = fareAmount;
        this.surcharge = surcharge;
        this.mtaTax = mtaTax;
        this.tipAmount = tipAmount;
        this.tollsAmount = tollsAmount;
        this.totalAmount = totalAmount;
    }

    public String getMedallion() {
        return medallion;
    }

    public String getHackLicense() {
        return hackLicense;
    }

    public Date getPickupDatetime() {
        return pickupDatetime;
    }

    public Date getDropoffDatetime() {
        return dropoffDatetime;
    }

    public Double getTripTime() {
        return tripTime;
    }

    public Double getTripDistance() {
        return tripDistance;
    }

    public Location getPickupLoc() {
        return pickupLoc;
    }

    public Location getDropoffLoc() {
        return dropoffLoc;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public Double getFareAmount() {
        return fareAmount;
    }

    public Double getSurcharge() {
        return surcharge;
    }

    public Double getMtaTax() {
        return mtaTax;
    }

    public Double getTipAmount() {
        return tipAmount;
    }

    public Double getTollsAmount() {
        return tollsAmount;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public enum PaymentType {
        CSH,
        CRD
    }

    @Override
    public String toString() {
        return "Trip{" +
                "medallion='" + medallion + '\'' +
                ", hackLicense='" + hackLicense + '\'' +
                ", pickupDatetime=" + pickupDatetime +
                ", dropoffDatetime=" + dropoffDatetime +
                ", tripTime=" + tripTime +
                ", tripDistance=" + tripDistance +
                ", pickupLoc=" + pickupLoc +
                ", dropoffLoc=" + dropoffLoc +
                ", paymentType=" + paymentType +
                ", fareAmount=" + fareAmount +
                ", surcharge=" + surcharge +
                ", mtaTax=" + mtaTax +
                ", tipAmount=" + tipAmount +
                ", tollsAmount=" + tollsAmount +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
