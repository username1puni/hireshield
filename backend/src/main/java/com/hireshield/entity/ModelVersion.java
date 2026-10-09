package com.hireshield.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "model_versions")
@Getter
@Setter
@NoArgsConstructor
public class ModelVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(nullable = false, length = 100)
    private String algorithm;

    @Column(name = "trained_at", nullable = false)
    private Instant trainedAt;

    @Column(name = "precision_score")
    private BigDecimal precision;

    @Column(name = "recall_score")
    private BigDecimal recall;

    @Column(name = "f1_score")
    private BigDecimal f1;

    @Column(name = "pr_auc")
    private BigDecimal prAuc;

    private BigDecimal threshold;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    private boolean active;
}
