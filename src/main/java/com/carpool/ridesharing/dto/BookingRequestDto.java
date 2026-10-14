package com.carpool.ridesharing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BookingRequestDto {

    @NotNull(message = "Ride ID is required")
    private Long rideId;

    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "Must book at least 1 seat")
    @Max(value = 8, message = "Cannot book more than 8 seats")
    private Integer seatsBooked = 1;

    private String pickupLocation;
    private String dropoffLocation;

    public BookingRequestDto() {}

    public BookingRequestDto(Long rideId, Integer seatsBooked) {
        this.rideId = rideId;
        this.seatsBooked = seatsBooked;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public Integer getSeatsBooked() {
        return seatsBooked != null ? seatsBooked : 1;
    }

    public void setSeatsBooked(Integer seatsBooked) {
        this.seatsBooked = seatsBooked;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(String dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }
}
