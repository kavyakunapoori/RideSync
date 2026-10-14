package com.carpool.ridesharing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RideCreateDto {

    @NotNull(message = "Please select a vehicle")
    private Long vehicleId;

    @NotBlank(message = "Origin / Source address is required")
    private String sourceAddress;

    private Double sourceLatitude;
    private Double sourceLongitude;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    private Double destinationLatitude;
    private Double destinationLongitude;

    @NotNull(message = "Departure date is required")
    @FutureOrPresent(message = "Departure date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    @NotNull(message = "Departure time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime departureTime;

    @NotNull(message = "Total available passenger seats is required")
    @Min(value = 1, message = "Must offer at least 1 seat")
    @Max(value = 8, message = "Maximum 8 seats allowed")
    private Integer totalSeats;

    @NotNull(message = "Total estimated trip cost is required")
    @DecimalMin(value = "1.00", message = "Trip cost must be at least $1.00")
    private BigDecimal totalTripCost;

    @Min(value = 0, message = "Driver contribution cannot be negative")
    @Max(value = 90, message = "Driver contribution cannot exceed 90%")
    private Double driverContributionPercent = 0.0;

    private String notes;

    private List<RideStopDto> stops = new ArrayList<>();

    public RideCreateDto() {}

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public Double getSourceLatitude() {
        return sourceLatitude;
    }

    public void setSourceLatitude(Double sourceLatitude) {
        this.sourceLatitude = sourceLatitude;
    }

    public Double getSourceLongitude() {
        return sourceLongitude;
    }

    public void setSourceLongitude(Double sourceLongitude) {
        this.sourceLongitude = sourceLongitude;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public BigDecimal getTotalTripCost() {
        return totalTripCost;
    }

    public void setTotalTripCost(BigDecimal totalTripCost) {
        this.totalTripCost = totalTripCost;
    }

    public Double getDriverContributionPercent() {
        return driverContributionPercent;
    }

    public void setDriverContributionPercent(Double driverContributionPercent) {
        this.driverContributionPercent = driverContributionPercent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<RideStopDto> getStops() {
        return stops;
    }

    public void setStops(List<RideStopDto> stops) {
        this.stops = stops;
    }
}
