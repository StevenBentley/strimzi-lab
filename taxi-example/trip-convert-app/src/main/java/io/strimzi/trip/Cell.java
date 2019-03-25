package io.strimzi.trip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class Cell implements Serializable {
    private int clat;
    private int clong;

    @JsonCreator
    public Cell(@JsonProperty("clat") int clat,
                @JsonProperty("clong") int clong) {
        this.clat = clat;
        this.clong = clong;
    }

    public Cell(Location origin, Location sideLength, Location l) {
        this.clat  = (int) Math.floor(((origin.getLatitude() - l.getLatitude()) / sideLength.getLatitude()) + 1);
        this.clong = (int) Math.floor(((l.getLongitude() - origin.getLongitude()) / sideLength.getLongitude()) + 1);
    }

    public int getClat() {
        return clat;
    }

    public int getClong() {
        return clong;
    }

    public boolean inBounds(int maxClat, int maxClong) {
        return this.clat <= maxClat && this.clong <= maxClong;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return clat == cell.clat &&
                clong == cell.clong;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clat, clong);
    }

    @Override
    public String toString() {
        return String.format("Cell(%d,%d)", clat, clong);
    }
}
