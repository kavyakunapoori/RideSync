package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.RatingDto;
import com.carpool.ridesharing.entity.Booking;
import com.carpool.ridesharing.entity.BookingStatus;
import com.carpool.ridesharing.entity.Rating;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.exception.ResourceNotFoundException;
import com.carpool.ridesharing.repository.BookingRepository;
import com.carpool.ridesharing.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;

    public RatingService(RatingRepository ratingRepository, BookingRepository bookingRepository) {
        this.ratingRepository = ratingRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Rating submitRating(User passenger, RatingDto dto) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));

        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new InvalidOperationException("Only the passenger who took this trip can submit a rating.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidOperationException("You can only rate completed trips.");
        }

        if (booking.isRated() || ratingRepository.existsByBookingId(booking.getId())) {
            throw new InvalidOperationException("You have already submitted a rating for this trip.");
        }

        if (dto.getScore() < 1 || dto.getScore() > 5) {
            throw new InvalidOperationException("Rating score must be between 1 and 5.");
        }

        Rating rating = new Rating(
                booking,
                booking.getRide(),
                passenger,
                booking.getRide().getDriver(),
                dto.getScore(),
                dto.getComment()
        );

        booking.setRated(true);
        bookingRepository.save(booking);

        return ratingRepository.save(rating);
    }

    public Double getAverageRatingForDriver(Long driverId) {
        Double avg = ratingRepository.calculateAverageRatingForDriver(driverId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : null;
    }

    public List<Rating> getDriverReviews(Long driverId) {
        return ratingRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
    }
}
