package com.carpool.ridesharing;

import com.carpool.ridesharing.dto.RideMatchResultDto;
import com.carpool.ridesharing.dto.RideSearchDto;
import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.repository.RatingRepository;
import com.carpool.ridesharing.repository.RideRepository;
import com.carpool.ridesharing.service.SmartRideMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SmartRideMatchingServiceTest {

    private RideRepository rideRepository;
    private RatingRepository ratingRepository;
    private SmartRideMatchingService matchingService;

    @BeforeEach
    void setUp() {
        rideRepository = Mockito.mock(RideRepository.class);
        ratingRepository = Mockito.mock(RatingRepository.class);
        matchingService = new SmartRideMatchingService(rideRepository, ratingRepository);

        // Inject default weights
        ReflectionTestUtils.setField(matchingService, "routeWeight", 0.50);
        ReflectionTestUtils.setField(matchingService, "timeWeight", 0.20);
        ReflectionTestUtils.setField(matchingService, "costWeight", 0.20);
        ReflectionTestUtils.setField(matchingService, "seatsWeight", 0.10);
    }

    @Test
    @DisplayName("Haversine formula should calculate accurate geodesic distances in kilometers")
    void testHaversineDistance() {
        // Distance between Times Square (40.7580, -73.9855) and Wall Street (40.7061, -74.0092) is approx 6.1 km
        double dist = matchingService.calculateDistanceKm(40.7580, -73.9855, 40.7061, -74.0092);
        assertTrue(dist > 5.5 && dist < 6.5, "Distance should be ~6.1 km, got: " + dist);
    }

    @Test
    @DisplayName("Smart matching should rank higher compatibility rides first")
    void testMatchingRankOrder() {
        User driver = new User("driver@carpool.com", "pass", "John", "123");
        driver.setId(1L);
        Vehicle car = new Vehicle(driver, "Toyota", "Camry", 2023, "ABC-123", "Silver", 4);

        // Ride A: Exact match with search
        Ride rideA = new Ride(
                driver, car,
                "Downtown", 40.7128, -74.0060,
                "Tech Park", 40.7589, -73.9851,
                LocalDate.now(), LocalTime.of(8, 30),
                3, BigDecimal.valueOf(15.00), 20.0, BigDecimal.valueOf(4.00)
        );
        rideA.setId(10L);

        // Ride B: Far away from search
        Ride rideB = new Ride(
                driver, car,
                "Airport", 40.6413, -73.7781,
                "Newark", 40.6895, -74.1745,
                LocalDate.now(), LocalTime.of(18, 00),
                2, BigDecimal.valueOf(45.00), 0.0, BigDecimal.valueOf(22.50)
        );
        rideB.setId(20L);

        when(rideRepository.findByStatusAndAvailableSeatsGreaterThanAndDepartureDateGreaterThanEqual(
                eq(RideStatus.PLANNED), eq(0), any(LocalDate.class)))
                .thenReturn(List.of(rideB, rideA));

        when(ratingRepository.calculateAverageRatingForDriver(1L)).thenReturn(4.8);
        when(ratingRepository.countRatingsForDriver(1L)).thenReturn(5L);

        RideSearchDto search = new RideSearchDto();
        search.setSourceAddress("Downtown");
        search.setSourceLatitude(40.7128);
        search.setSourceLongitude(-74.0060);
        search.setDestinationAddress("Tech Park");
        search.setDestinationLatitude(40.7589);
        search.setDestinationLongitude(-73.9851);
        search.setDepartureDate(LocalDate.now());
        search.setDepartureTime(LocalTime.of(8, 30));
        search.setSeatsRequested(1);

        List<RideMatchResultDto> results = matchingService.findMatchingRides(search);

        assertFalse(results.isEmpty());
        assertEquals(2, results.size());

        // Ride A should be ranked #1 with higher match score
        assertEquals(10L, results.get(0).getRide().getId());
        assertTrue(results.get(0).getMatchScore() > results.get(1).getMatchScore(),
                "Ride A match score should be higher than Ride B");
        assertTrue(results.get(0).getMatchScore() >= 80.0,
                "Ride A should score >= 80%");
    }
}
