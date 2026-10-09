package com.hireshield.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "link_checks")
@Getter
@Setter
@NoArgsConstructor
public class LinkCheck {

    @Id
    @Column(name = "url_hash", length = 64)
    private String urlHash;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 20)
    private String verdict;

    @Column(length = 1000)
    private String reasons;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
