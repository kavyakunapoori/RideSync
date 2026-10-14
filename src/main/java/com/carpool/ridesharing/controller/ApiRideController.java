package com.carpool.ridesharing.controller;

import com.carpool.ridesharing.dto.RideMatchResultDto;
import com.carpool.ridesharing.dto.RideSearchDto;
import com.carpool.ridesharing.entity.Ride;
import com.carpool.ridesharing.entity.RideStop;
import com.carpool.ridesharing.service.RideService;
import com.carpool.ridesharing.service.SmartRideMatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiRideController {

    private final SmartRideMatchingService matchingService;
    private final RideService rideService;

    public ApiRideController(SmartRideMatchingService matchingService, RideService rideService) {
        this.matchingService = matchingService;
        this.rideService = rideService;
    }

    @GetMapping("/rides/search")
    public ResponseEntity<List<RideMatchResultDto>> searchRidesApi(
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam(value = "seats", defaultValue = "1") int seats) {

        RideSearchDto searchDto = new RideSearchDto();
        searchDto.setSourceAddress(origin);
        searchDto.setDestinationAddress(destination);
        searchDto.setSeatsRequested(seats);

        List<RideMatchResultDto> matches = matchingService.findMatchingRides(searchDto);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/rides/{id}/route")
    public ResponseEntity<Map<String, Object>> getRideRoute(@PathVariable("id") Long id) {
        Ride ride = rideService.getRideById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("rideId", ride.getId());
        response.put("source", Map.of(
                "address", ride.getSourceAddress(),
                "lat", ride.getSourceLatitude(),
                "lng", ride.getSourceLongitude()
        ));
        response.put("destination", Map.of(
                "address", ride.getDestinationAddress(),
                "lat", ride.getDestinationLatitude(),
                "lng", ride.getDestinationLongitude()
        ));

        List<Map<String, Object>> stopsList = new ArrayList<>();
        if (ride.getStops() != null) {
            for (RideStop stop : ride.getStops()) {
                stopsList.add(Map.of(
                        "name", stop.getStopName(),
                        "lat", stop.getLatitude(),
                        "lng", stop.getLongitude(),
                        "order", stop.getStopOrder()
                ));
            }
        }
        response.put("stops", stopsList);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocode(@RequestParam("query") String query) {
        double[] coords = matchingService.resolveCoordinates(query, null, null);
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("lat", coords[0]);
        result.put("lng", coords[1]);
        return ResponseEntity.ok(result);
    }
}
