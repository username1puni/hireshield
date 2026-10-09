package com.hireshield.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null when a guest used the public text check.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private InputType inputType;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "recruiter_email")
    private String recruiterEmail;

    private String domain;

    @Column(name = "final_score", nullable = false)
    private int finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "rule_score", nullable = false)
    private int ruleScore;

    @Column(name = "ml_probability", nullable = false)
    private BigDecimal mlProbability;

    @Column(name = "trust_score", nullable = false)
    private int trustScore;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "explanation_source", length = 20)
    private String explanationSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_version_id")
    private ModelVersion modelVersion;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("points DESC")
    private List<AnalysisFlag> flags = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public void addFlag(AnalysisFlag flag) {
        flag.setAnalysis(this);
        flags.add(flag);
    }
}
