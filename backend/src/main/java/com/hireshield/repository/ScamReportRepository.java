package com.hireshield.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.ReportStatus;
import com.hireshield.entity.ScamReport;

public interface ScamReportRepository extends JpaRepository<ScamReport, Long> {

    Page<ScamReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
