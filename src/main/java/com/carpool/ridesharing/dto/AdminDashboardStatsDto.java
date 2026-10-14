package com.carpool.ridesharing.dto;

public class AdminDashboardStatsDto {

    private long totalUsers;
    private long totalDrivers;
    private long totalPassengers;
    private long activeUsers;
    private long totalRides;
    private long completedRides;
    private long activeBookings;
    private long totalBookings;
    private long cancelledBookings;
    private double cancellationRate;
    private double co2SavedKg;
    private double fuelMoneySavedUsd;
    private long pendingReports;

    public AdminDashboardStatsDto() {}

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDrivers() {
        return totalDrivers;
    }

    public void setTotalDrivers(long totalDrivers) {
        this.totalDrivers = totalDrivers;
    }

    public long getTotalPassengers() {
        return totalPassengers;
    }

    public void setTotalPassengers(long totalPassengers) {
        this.totalPassengers = totalPassengers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(long totalRides) {
        this.totalRides = totalRides;
    }

    public long getCompletedRides() {
        return completedRides;
    }

    public void setCompletedRides(long completedRides) {
        this.completedRides = completedRides;
    }

    public long getActiveBookings() {
        return activeBookings;
    }

    public void setActiveBookings(long activeBookings) {
        this.activeBookings = activeBookings;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public double getCancellationRate() {
        return cancellationRate;
    }

    public void setCancellationRate(double cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public double getCo2SavedKg() {
        return co2SavedKg;
    }

    public void setCo2SavedKg(double co2SavedKg) {
        this.co2SavedKg = co2SavedKg;
    }

    public double getFuelMoneySavedUsd() {
        return fuelMoneySavedUsd;
    }

    public void setFuelMoneySavedUsd(double fuelMoneySavedUsd) {
        this.fuelMoneySavedUsd = fuelMoneySavedUsd;
    }

    public long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(long pendingReports) {
        this.pendingReports = pendingReports;
    }
}
