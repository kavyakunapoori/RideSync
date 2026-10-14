package com.carpool.ridesharing.dto;

public class RideStopDto {
    private String stopName;
    private Double latitude;
    private Double longitude;
    private int stopOrder;

    public RideStopDto() {}

    public RideStopDto(String stopName, Double latitude, Double longitude, int stopOrder) {
        this.stopName = stopName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.stopOrder = stopOrder;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }
}
