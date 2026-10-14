package com.carpool.ridesharing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RatingDto {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Rating score is required")
    @Min(value = 1, message = "Minimum rating is 1 star")
    @Max(value = 5, message = "Maximum rating is 5 stars")
    private Integer score = 5;

    @Size(max = 1000, message = "Comment must be under 1000 characters")
    private String comment;

    public RatingDto() {}

    public RatingDto(Long bookingId, Integer score, String comment) {
        this.bookingId = bookingId;
        this.score = score;
        this.comment = comment;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
