package com.carpool.ridesharing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service to handle transparent and proportional fare-sharing calculations.
 * Ensures total allocated passenger costs do not exceed the remaining trip cost.
 */
@Service
public class FareSharingService {

    /**
     * Calculate the contribution per passenger seat.
     *
     * @param totalTripCost              Estimated total fuel/toll/operating cost set by driver
     * @param driverContributionPercent  Percentage of the cost paid by the driver (0% to 90%)
     * @param passengerCapacity          Total passenger seats offered
     * @return Per-passenger seat fare rounded to 2 decimal places
     */
    public BigDecimal calculatePassengerCost(BigDecimal totalTripCost,
                                             Double driverContributionPercent,
                                             int passengerCapacity) {
        if (totalTripCost == null || totalTripCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (passengerCapacity <= 0) {
            return totalTripCost;
        }

        double driverPercent = driverContributionPercent != null ? driverContributionPercent : 0.0;
        if (driverPercent < 0.0) driverPercent = 0.0;
        if (driverPercent > 90.0) driverPercent = 90.0;

        // Passenger share percentage = 100% - driverPercent
        double passengerShareRatio = (100.0 - driverPercent) / 100.0;
        BigDecimal passengerTotalCost = totalTripCost.multiply(BigDecimal.valueOf(passengerShareRatio));

        // Cost per seat = passengerTotalCost / passengerCapacity
        return passengerTotalCost.divide(BigDecimal.valueOf(passengerCapacity), 2, RoundingMode.HALF_UP);
    }

    /**
     * Recalculate passenger cost based on actual confirmed passengers.
     */
    public BigDecimal calculateDynamicSeatCost(BigDecimal totalTripCost,
                                               Double driverContributionPercent,
                                               int confirmedPassengers) {
        if (confirmedPassengers <= 0) {
            return calculatePassengerCost(totalTripCost, driverContributionPercent, 1);
        }
        return calculatePassengerCost(totalTripCost, driverContributionPercent, confirmedPassengers);
    }

    /**
     * Calculate passenger total savings compared to taking a solo taxi/cab.
     * Assumes average commercial cab cost is approximately 2.5x the carpool share.
     */
    public BigDecimal calculateEstimatedSoloFare(BigDecimal carpoolFare) {
        if (carpoolFare == null) return BigDecimal.ZERO;
        return carpoolFare.multiply(BigDecimal.valueOf(2.5)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateEstimatedPassengerSavings(BigDecimal carpoolFare) {
        if (carpoolFare == null) return BigDecimal.ZERO;
        BigDecimal soloFare = calculateEstimatedSoloFare(carpoolFare);
        return soloFare.subtract(carpoolFare).setScale(2, RoundingMode.HALF_UP);
    }
}
