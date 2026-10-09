package com.hireshield.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blacklist_entries")
@Getter
@Setter
@NoArgsConstructor
public class BlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private BlacklistType type;

    @Column(name = "entry_value", nullable = false)
    private String value;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 1;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen = Instant.now();

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen = Instant.now();

    public BlacklistEntry(BlacklistType type, String value) {
        this.type = type;
        this.value = value;
    }
}
