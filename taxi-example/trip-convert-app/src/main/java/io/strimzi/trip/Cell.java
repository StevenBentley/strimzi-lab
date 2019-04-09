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
        this.clong = (int) Math.floor((Math.abs(l.getLongitude() - origin.getLongitude()) / sideLength.getLongitude()) + 1);
    }
    // latlength  = 0.00900537959
    // longlength = 0.01017737174
    // 41.474937,-74.913585
    // 40.738079,-73.990479,40.746323,-73.981880
    // 41.474937 - 40.738079 = 0.736858 / 0.00900537959 = 81.8242021489
    // -73.990479 - -74.913585 = 0.923106 / 0.01017737174 = 90.7018062799
    // -73.965485, 40.762737
    // (41.474937 - 40.738079)/0.00900537959 = 81.8242021489
    public int getClat() {
        return clat;
    }

    public int getClong() {
        return clong;
    }

    public boolean inBounds(int minClat, int minClong, int maxClat, int maxClong) {
        return (this.clat >= minClat && this.clong >= minClong) && (this.clat <= maxClat && this.clong <= maxClong);
    }

    public boolean inBounds(int maxClat, int maxClong) {
        return inBounds(1,1, maxClat, maxClong);
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
