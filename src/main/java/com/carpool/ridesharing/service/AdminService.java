package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.AdminDashboardStatsDto;
import com.carpool.ridesharing.entity.*;
import com.carpool.ridesharing.exception.ResourceNotFoundException;
import com.carpool.ridesharing.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;

    public AdminService(UserRepository userRepository,
                        RideRepository rideRepository,
                        BookingRepository bookingRepository,
                        ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.reportRepository = reportRepository;
    }

    public AdminDashboardStatsDto getDashboardStats() {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();

        long totalUsers = userRepository.count();
        long totalDrivers = userRepository.countByRoleName("ROLE_DRIVER");
        long totalPassengers = userRepository.countByRoleName("ROLE_PASSENGER");
        long activeUsers = userRepository.countByActiveTrue();

        long totalRides = rideRepository.count();
        long completedRides = rideRepository.countByStatus(RideStatus.COMPLETED);

        long totalBookings = bookingRepository.count();
        long activeBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);

        double cancellationRate = 0.0;
        if (totalBookings > 0) {
            cancellationRate = Math.round(((double) cancelledBookings / totalBookings * 100.0) * 10.0) / 10.0;
        }

        // Environmental calculation:
        // Each completed/confirmed seat saves roughly 18 km solo driving
        // ~0.18 kg CO2 per passenger km saved -> ~3.24 kg CO2 per shared trip
        // ~$0.15 per km fuel cost saved
        long sharedTrips = bookingRepository.countCompletedOrConfirmedBookings();
        double co2Saved = Math.round(sharedTrips * 3.24 * 10.0) / 10.0;
        double moneySaved = Math.round(sharedTrips * 18.0 * 0.15 * 10.0) / 10.0;

        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);

        stats.setTotalUsers(totalUsers);
        stats.setTotalDrivers(totalDrivers);
        stats.setTotalPassengers(totalPassengers);
        stats.setActiveUsers(activeUsers);
        stats.setTotalRides(totalRides);
        stats.setCompletedRides(completedRides);
        stats.setTotalBookings(totalBookings);
        stats.setActiveBookings(activeBookings);
        stats.setCancelledBookings(cancelledBookings);
        stats.setCancellationRate(cancellationRate);
        stats.setCo2SavedKg(co2Saved);
        stats.setFuelMoneySavedUsd(moneySaved);
        stats.setPendingReports(pendingReports);

        return stats;
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAllByOrderByDepartureDateDescDepartureTimeDesc();
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingTimeDesc();
    }

    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void resolveReport(Long reportId, String adminNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        report.setStatus(ReportStatus.RESOLVED);
        report.setAdminNotes(adminNotes != null ? adminNotes.trim() : "Resolved by administrator");
        reportRepository.save(report);
    }
}
