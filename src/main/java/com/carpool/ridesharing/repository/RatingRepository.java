package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    Optional<Rating> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.driver.id = :driverId")
    Double calculateAverageRatingForDriver(@Param("driverId") Long driverId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.driver.id = :driverId")
    long countRatingsForDriver(@Param("driverId") Long driverId);
}
