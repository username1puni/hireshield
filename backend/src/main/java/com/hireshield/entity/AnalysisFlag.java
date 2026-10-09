package com.hireshield.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analysis_flags")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(name = "rule_code", nullable = false, length = 40)
    private String ruleCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int points;

    @Column(length = 500)
    private String evidence;

    public AnalysisFlag(String ruleCode, String title, int points, String evidence) {
        this.ruleCode = ruleCode;
        this.title = title;
        this.points = points;
        this.evidence = evidence;
    }
}
