package com.hireshield.dto;

import java.time.LocalDate;

/**
 * What we found out about one domain. Null fields mean "could not check", not "no".
 */
public record DomainReport(
        String domain,
        LocalDate registeredOn,
        Integer ageDays,
        String registrar,
        Boolean hasMx,
        boolean freeEmailProvider,
        String lookalikeOf,
        boolean governmentDomain) {

    public boolean isNew(int days) {
        return ageDays != null && ageDays < days;
    }
}
