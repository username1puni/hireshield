package com.hireshield.dto;

import java.util.List;

public record LinkReport(String url, String domain, LinkVerdict verdict, List<String> reasons) {

    public enum LinkVerdict {
        SAFE, SUSPICIOUS, DANGEROUS
    }
}
