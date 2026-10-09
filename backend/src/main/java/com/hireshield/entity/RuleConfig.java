package com.hireshield.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rule_config")
@Getter
@Setter
@NoArgsConstructor
public class RuleConfig {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private boolean enabled = true;

    public RuleConfig(String code, String title, String description, int points) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.points = points;
    }
}
