package com.carpool.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportDto {

    private Long reportedUserId;
    private Long reportedRideId;

    @NotBlank(message = "Please provide a reason for the report")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;

    public ReportDto() {}

    public Long getReportedUserId() {
        return reportedUserId;
    }

    public void setReportedUserId(Long reportedUserId) {
        this.reportedUserId = reportedUserId;
    }

    public Long getReportedRideId() {
        return reportedRideId;
    }

    public void setReportedRideId(Long reportedRideId) {
        this.reportedRideId = reportedRideId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
