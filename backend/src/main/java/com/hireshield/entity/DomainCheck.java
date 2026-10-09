package com.hireshield.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domain_checks")
@Getter
@Setter
@NoArgsConstructor
public class DomainCheck {

    @Id
    private String domain;

    @Column(name = "registered_on")
    private LocalDate registeredOn;

    @Column(name = "age_days")
    private Integer ageDays;

    private String registrar;

    @Column(name = "has_mx")
    private Boolean hasMx;

    @Column(name = "free_provider", nullable = false)
    private boolean freeProvider;

    @Column(name = "lookalike_of", length = 100)
    private String lookalikeOf;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
