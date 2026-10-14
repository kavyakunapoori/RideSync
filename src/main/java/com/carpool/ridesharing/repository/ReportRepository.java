package com.carpool.ridesharing.repository;

import com.carpool.ridesharing.entity.Report;
import com.carpool.ridesharing.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    long countByStatus(ReportStatus status);
}
