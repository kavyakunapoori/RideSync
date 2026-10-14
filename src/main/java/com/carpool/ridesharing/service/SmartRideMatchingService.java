package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.RideMatchResultDto;
import com.carpool.ridesharing.dto.RideSearchDto;
import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.RideStatus;
import com.carpool.ridesharing.entity.RideStop;
import com.carpool.ridesharing.repository.RatingRepository;
import com.carpool.ridesharing.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Intelligent Smart Ride-Matching Algorithm
 * Evaluates candidate rides based on multi-criteria normalized scoring:
 * - Route compatibility (50%)
 * - Departure time compatibility (20%)
 * - Cost compatibility (20%)
 * - Available seat compatibility (10%)
 */
@Service
public class SmartRideMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(SmartRideMatchingService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Configurable weights
    @Value("${ridesharing.matching.route-weight:0.50}")
    private double routeWeight;

    @Value("${ridesharing.matching.time-weight:0.20}")
    private double timeWeight;

    @Value("${ridesharing.matching.cost-weight:0.20}")
    private double costWeight;

    @Value("${ridesharing.matching.seats-weight:0.10}")
    private double seatsWeight;

    private final RideRepository rideRepository;
    private final RatingRepository ratingRepository;

    // Pre-registered coordinates fallback for common locations / demonstration cities
    private static final Map<String, double[]> KNOWN_LOCATIONS = new HashMap<>();

    static {
        // Sample city hubs (New York & suburbs demo corridor)
        KNOWN_LOCATIONS.put("times square", new double[]{40.7580, -73.9855});
        KNOWN_LOCATIONS.put("wall street", new double[]{40.7061, -74.0092});
        KNOWN_LOCATIONS.put("brooklyn bridge", new double[]{40.7061, -73.9969});
        KNOWN_LOCATIONS.put("jfk airport", new double[]{40.6413, -73.7781});
        KNOWN_LOCATIONS.put("newark airport", new double[]{40.6895, -74.1745});
        KNOWN_LOCATIONS.put("central park", new double[]{40.785091, -73.968285});
        KNOWN_LOCATIONS.put("jersey city", new double[]{40.7178, -74.0431});
        KNOWN_LOCATIONS.put("columbia university", new double[]{40.8075, -73.9626});
        KNOWN_LOCATIONS.put("nyu", new double[]{40.7295, -73.9965});
        KNOWN_LOCATIONS.put("grand central", new double[]{40.7527, -73.9772});
        KNOWN_LOCATIONS.put("penn station", new double[]{40.7505, -73.9934});

        // Common generic college / tech park corridors
        KNOWN_LOCATIONS.put("downtown", new double[]{40.7128, -74.0060});
        KNOWN_LOCATIONS.put("tech park", new double[]{40.7589, -73.9851});
        KNOWN_LOCATIONS.put("university campus", new double[]{40.8075, -73.9626});
        KNOWN_LOCATIONS.put("airport", new double[]{40.6413, -73.7781});
        KNOWN_LOCATIONS.put("suburb station", new double[]{40.7300, -73.9950});
    }

    public SmartRideMatchingService(RideRepository rideRepository, RatingRepository ratingRepository) {
        this.rideRepository = rideRepository;
        this.ratingRepository = ratingRepository;
    }

    /**
     * Resolves approximate coordinates for an address query using known hubs or fallback.
     */
    public double[] resolveCoordinates(String address, Double userLat, Double userLon) {
        if (userLat != null && userLon != null && userLat != 0.0 && userLon != 0.0) {
            return new double[]{userLat, userLon};
        }
        if (address == null || address.trim().isEmpty()) {
            return new double[]{40.7128, -74.0060}; // Default city center fallback
        }

        String query = address.toLowerCase().trim();
        for (Map.Entry<String, double[]> entry : KNOWN_LOCATIONS.entrySet()) {
            if (query.contains(entry.getKey()) || entry.getKey().contains(query)) {
                return entry.getValue();
            }
        }

        // Deterministic synthetic coordinate generation based on string hash for testing
        // so consistent input yields consistent spatial coordinates in a realistic ~25km bounding area
        int hash = Math.abs(query.hashCode());
        double deltaLat = ((hash % 1000) / 1000.0 - 0.5) * 0.25;
        double deltaLon = (((hash / 1000) % 1000) / 1000.0 - 0.5) * 0.25;
        return new double[]{40.7128 + deltaLat, -74.0060 + deltaLon};
    }

    /**
     * Main smart matching algorithm.
     */
    public List<RideMatchResultDto> findMatchingRides(RideSearchDto search) {
        LocalDate searchDate = search.getDepartureDate() != null ? search.getDepartureDate() : LocalDate.now();
        int requestedSeats = search.getSeatsRequested() != null ? search.getSeatsRequested() : 1;

        // Step 1: Candidate retrieval from database (only active, future/present, seats >= requested)
        List<Ride> candidates = rideRepository.findByStatusAndAvailableSeatsGreaterThanAndDepartureDateGreaterThanEqual(
                RideStatus.PLANNED, 0, searchDate);

        double[] passengerOrigin = resolveCoordinates(search.getSourceAddress(), search.getSourceLatitude(), search.getSourceLongitude());
        double[] passengerDest = resolveCoordinates(search.getDestinationAddress(), search.getDestinationLatitude(), search.getDestinationLongitude());

        LocalTime searchTime = search.getDepartureTime() != null ? search.getDepartureTime() : LocalTime.of(8, 0);

        List<RideMatchResultDto> matchResults = new ArrayList<>();

        for (Ride ride : candidates) {
            // Must have sufficient available seats
            if (ride.getAvailableSeats() < requestedSeats) {
                continue;
            }

            // Step 2: Route Compatibility (50% max)
            double originDist = calculateDistanceKm(passengerOrigin[0], passengerOrigin[1],
                    ride.getSourceLatitude(), ride.getSourceLongitude());

            double destDist = calculateDistanceKm(passengerDest[0], passengerDest[1],
                    ride.getDestinationLatitude(), ride.getDestinationLongitude());

            // Check if passenger's pickup or dropoff is close to any intermediate stops
            if (ride.getStops() != null && !ride.getStops().isEmpty()) {
                for (RideStop stop : ride.getStops()) {
                    double stopToOrigin = calculateDistanceKm(passengerOrigin[0], passengerOrigin[1],
                            stop.getLatitude(), stop.getLongitude());
                    if (stopToOrigin < originDist) {
                        originDist = stopToOrigin;
                    }

                    double stopToDest = calculateDistanceKm(passengerDest[0], passengerDest[1],
                            stop.getLatitude(), stop.getLongitude());
                    if (stopToDest < destDist) {
                        destDist = stopToDest;
                    }
                }
            }

            // Route compatibility score: 50 * max(0, 1 - (originDist / 30.0)*0.5 - (destDist / 30.0)*0.5)
            // If both origin and dest are < 1 km, routeScore -> 50.
            double originNormalized = Math.max(0.0, 1.0 - (originDist / 30.0));
            double destNormalized = Math.max(0.0, 1.0 - (destDist / 30.0));
            double routeScore = (originNormalized * 0.5 + destNormalized * 0.5) * 50.0;

            // Step 3: Departure-Time Compatibility (20% max)
            double timeScore = calculateTimeCompatibilityScore(searchTime, ride.getDepartureTime(), searchDate, ride.getDepartureDate());

            // Step 4: Cost Compatibility (20% max)
            // Normalized based on passenger cost per seat
            double costScore = calculateCostCompatibilityScore(ride.getPassengerCost());

            // Step 5: Seat Availability Compatibility (10% max)
            double seatsScore = calculateSeatsCompatibilityScore(ride.getAvailableSeats(), requestedSeats);

            // Total composite score (0 - 100)
            double totalScore = routeScore + timeScore + costScore + seatsScore;

            // Generate understandable explanation
            String explanation = buildScoreExplanation(routeScore, timeScore, costScore, seatsScore, originDist, destDist);

            // Driver rating info
            Double avgRating = ratingRepository.calculateAverageRatingForDriver(ride.getDriver().getId());
            long ratingCount = ratingRepository.countRatingsForDriver(ride.getDriver().getId());

            RideMatchResultDto resultDto = new RideMatchResultDto(
                    ride,
                    totalScore,
                    routeScore,
                    timeScore,
                    costScore,
                    seatsScore,
                    explanation,
                    originDist,
                    destDist,
                    ride.getPassengerCost(),
                    avgRating,
                    ratingCount
            );

            matchResults.add(resultDto);
        }

        // Step 6: Rank candidates by total composite score descending
        matchResults.sort(Comparator.comparingDouble(RideMatchResultDto::getMatchScore).reversed());

        return matchResults;
    }

    /**
     * Haversine formula to compute geodesic surface distance in kilometers.
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Departure-time score out of 20 points.
     * Within 15 minutes = 20 pts.
     * Linearly decays up to 3 hours (180 mins).
     */
    private double calculateTimeCompatibilityScore(LocalTime targetTime, LocalTime rideTime,
                                                   LocalDate targetDate, LocalDate rideDate) {
        long dateDiffDays = Math.abs(Duration.between(targetDate.atStartOfDay(), rideDate.atStartOfDay()).toDays());
        if (dateDiffDays > 3) {
            return 0.0;
        }

        long diffMinutes = Math.abs(Duration.between(targetTime, rideTime).toMinutes());
        if (diffMinutes > 720) {
            diffMinutes = 1440 - diffMinutes; // handle overnight wrap
        }

        if (diffMinutes <= 15) {
            return 20.0;
        } else if (diffMinutes >= 180) {
            return 2.0; // small baseline for same-day trips
        } else {
            double ratio = 1.0 - ((double)(diffMinutes - 15) / (180 - 15));
            return Math.max(2.0, ratio * 20.0);
        }
    }

    /**
     * Cost score out of 20 points.
     * Lower passenger fares receive higher score.
     */
    private double calculateCostCompatibilityScore(BigDecimal passengerCost) {
        if (passengerCost == null) return 10.0;
        double cost = passengerCost.doubleValue();
        if (cost <= 5.0) {
            return 20.0;
        } else if (cost >= 50.0) {
            return 5.0;
        } else {
            // Linear scale between $5 and $50
            double ratio = 1.0 - ((cost - 5.0) / 45.0);
            return Math.max(5.0, 5.0 + ratio * 15.0);
        }
    }

    /**
     * Available seats score out of 10 points.
     */
    private double calculateSeatsCompatibilityScore(int availableSeats, int requestedSeats) {
        if (availableSeats < requestedSeats) return 0.0;
        if (availableSeats == requestedSeats) return 8.0;
        // Having spare seats allows comfortable boarding
        return 10.0;
    }

    private String buildScoreExplanation(double routeScore, double timeScore, double costScore,
                                         double seatsScore, double originDist, double destDist) {
        StringBuilder sb = new StringBuilder();
        if (routeScore >= 40.0) {
            sb.append("High route overlap (Pickup ").append(String.format("%.1f", originDist))
              .append("km away, Dropoff ").append(String.format("%.1f", destDist)).append("km away). ");
        } else if (routeScore >= 25.0) {
            sb.append("Moderate route alignment (Pickup ").append(String.format("%.1f", originDist))
              .append("km away). ");
        } else {
            sb.append("Approximate route match. ");
        }

        if (timeScore >= 16.0) {
            sb.append("Departure time aligns closely. ");
        } else if (timeScore >= 10.0) {
            sb.append("Reasonable departure window. ");
        }

        if (costScore >= 15.0) {
            sb.append("Cost-effective shared fare. ");
        }

        return sb.toString().trim();
    }
}
