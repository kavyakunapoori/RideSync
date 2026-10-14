package com.carpool.ridesharing;

import com.carpool.ridesharing.service.FareSharingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class FareSharingServiceTest {

    private FareSharingService fareSharingService;

    @BeforeEach
    void setUp() {
        fareSharingService = new FareSharingService();
    }

    @Test
    @DisplayName("Should divide fare equally when driver contribution is 0%")
    void testEqualSplitZeroDriverContribution() {
        BigDecimal tripCost = BigDecimal.valueOf(20.00);
        int seats = 4;
        Double driverContribution = 0.0;

        BigDecimal seatFare = fareSharingService.calculatePassengerCost(tripCost, driverContribution, seats);

        // $20.00 / 4 seats = $5.00
        assertEquals(new BigDecimal("5.00"), seatFare);
    }

    @Test
    @DisplayName("Should proportionally reduce passenger fare when driver contributes 20%")
    void testDriverContributionReducesPassengerFare() {
        BigDecimal tripCost = BigDecimal.valueOf(20.00);
        int seats = 4;
        Double driverContribution = 20.0; // Driver covers 20% ($4.00), remaining $16.00 / 4 = $4.00

        BigDecimal seatFare = fareSharingService.calculatePassengerCost(tripCost, driverContribution, seats);

        assertEquals(new BigDecimal("4.00"), seatFare);
    }

    @Test
    @DisplayName("Total passenger contributions should never exceed configured trip cost")
    void testTotalPassengerShareDoesNotExceedTripCost() {
        BigDecimal tripCost = BigDecimal.valueOf(35.50);
        int seats = 3;
        Double driverContribution = 25.0;

        BigDecimal seatFare = fareSharingService.calculatePassengerCost(tripCost, driverContribution, seats);
        BigDecimal totalPassengerCollected = seatFare.multiply(BigDecimal.valueOf(seats));

        assertTrue(totalPassengerCollected.compareTo(tripCost) <= 0);
    }

    @Test
    @DisplayName("Should correctly compute estimated solo cab fare and savings")
    void testSoloFareAndSavings() {
        BigDecimal sharedFare = BigDecimal.valueOf(10.00);

        BigDecimal soloFare = fareSharingService.calculateEstimatedSoloFare(sharedFare);
        BigDecimal savings = fareSharingService.calculateEstimatedPassengerSavings(sharedFare);

        // Solo fare = 10 * 2.5 = 25.00
        assertEquals(new BigDecimal("25.00"), soloFare);
        // Savings = 25.00 - 10.00 = 15.00
        assertEquals(new BigDecimal("15.00"), savings);
    }
}
