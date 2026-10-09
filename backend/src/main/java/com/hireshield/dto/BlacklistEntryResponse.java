package com.hireshield.dto;

import java.time.Instant;

import com.hireshield.entity.BlacklistEntry;
import com.hireshield.entity.BlacklistType;

public record BlacklistEntryResponse(BlacklistType type, String value, int reportCount, Instant firstSeen,
                                     Instant lastSeen) {

    public static BlacklistEntryResponse from(BlacklistEntry e) {
        return new BlacklistEntryResponse(e.getType(), e.getValue(), e.getReportCount(), e.getFirstSeen(), e.getLastSeen());
    }
}
