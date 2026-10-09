package com.hireshield.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scam_reports")
@Getter
@Setter
@NoArgsConstructor
public class ScamReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(name = "company_name", length = 200)
    private String companyName;

    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    private String domain;

    @Column(length = 50)
    private String platform;

    @Column(name = "amount_lost")
    private BigDecimal amountLost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
