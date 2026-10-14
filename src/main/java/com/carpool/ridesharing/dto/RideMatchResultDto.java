package com.carpool.ridesharing.dto;

import com.carpool.ridesharing.entity.Ride;
import java.math.BigDecimal;

public class RideMatchResultDto {

    private Ride ride;
    private double matchScore;       // Total 0-100%
    private double routeScore;       // Max 50
    private double timeScore;        // Max 20
    private double costScore;        // Max 20
    private double seatsScore;       // Max 10
    private String explanation;
    private double passengerPickupDistanceKm;
    private double passengerDropoffDistanceKm;
    private BigDecimal passengerCost;
    private Double driverRating;
    private long driverRatingCount;

    public RideMatchResultDto() {}

    public RideMatchResultDto(Ride ride, double matchScore, double routeScore, double timeScore,
                              double costScore, double seatsScore, String explanation,
                              double passengerPickupDistanceKm, double passengerDropoffDistanceKm,
                              BigDecimal passengerCost, Double driverRating, long driverRatingCount) {
        this.ride = ride;
        this.matchScore = Math.round(matchScore * 10.0) / 10.0;
        this.routeScore = Math.round(routeScore * 10.0) / 10.0;
        this.timeScore = Math.round(timeScore * 10.0) / 10.0;
        this.costScore = Math.round(costScore * 10.0) / 10.0;
        this.seatsScore = Math.round(seatsScore * 10.0) / 10.0;
        this.explanation = explanation;
        this.passengerPickupDistanceKm = Math.round(passengerPickupDistanceKm * 10.0) / 10.0;
        this.passengerDropoffDistanceKm = Math.round(passengerDropoffDistanceKm * 10.0) / 10.0;
        this.passengerCost = passengerCost;
        this.driverRating = driverRating != null ? Math.round(driverRating * 10.0) / 10.0 : null;
        this.driverRatingCount = driverRatingCount;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public double getRouteScore() {
        return routeScore;
    }

    public void setRouteScore(double routeScore) {
        this.routeScore = routeScore;
    }

    public double getTimeScore() {
        return timeScore;
    }

    public void setTimeScore(double timeScore) {
        this.timeScore = timeScore;
    }

    public double getCostScore() {
        return costScore;
    }

    public void setCostScore(double costScore) {
        this.costScore = costScore;
    }

    public double getSeatsScore() {
        return seatsScore;
    }

    public void setSeatsScore(double seatsScore) {
        this.seatsScore = seatsScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public double getPassengerPickupDistanceKm() {
        return passengerPickupDistanceKm;
    }

    public void setPassengerPickupDistanceKm(double passengerPickupDistanceKm) {
        this.passengerPickupDistanceKm = passengerPickupDistanceKm;
    }

    public double getPassengerDropoffDistanceKm() {
        return passengerDropoffDistanceKm;
    }

    public void setPassengerDropoffDistanceKm(double passengerDropoffDistanceKm) {
        this.passengerDropoffDistanceKm = passengerDropoffDistanceKm;
    }

    public BigDecimal getPassengerCost() {
        return passengerCost;
    }

    public void setPassengerCost(BigDecimal passengerCost) {
        this.passengerCost = passengerCost;
    }

    public Double getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(Double driverRating) {
        this.driverRating = driverRating;
    }

    public long getDriverRatingCount() {
        return driverRatingCount;
    }

    public void setDriverRatingCount(long driverRatingCount) {
        this.driverRatingCount = driverRatingCount;
    }
}
