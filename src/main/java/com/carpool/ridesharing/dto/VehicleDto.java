package com.carpool.ridesharing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VehicleDto {

    private Long id;

    @NotBlank(message = "Vehicle make is required (e.g. Toyota, Honda)")
    @Size(max = 50)
    private String make;

    @NotBlank(message = "Vehicle model is required (e.g. Camry, Civic)")
    @Size(max = 50)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1990, message = "Vehicle year must be 1990 or newer")
    @Max(value = 2030, message = "Invalid vehicle year")
    private Integer year;

    @NotBlank(message = "License plate number is required")
    @Size(min = 2, max = 30)
    private String licensePlate;

    private String color;

    @NotNull(message = "Passenger seating capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 passenger")
    @Max(value = 8, message = "Capacity cannot exceed 8 passengers")
    private Integer capacity;

    public VehicleDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
