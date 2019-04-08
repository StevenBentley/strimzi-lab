package io.strimzi.trip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class DoublePair implements Serializable {
    Double x;
    Double y;

    @JsonCreator
    public DoublePair(@JsonProperty("x") Double x,
                      @JsonProperty("y") Double y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "DoublePair{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}