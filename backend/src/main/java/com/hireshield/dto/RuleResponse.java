package com.hireshield.dto;

import com.hireshield.entity.RuleConfig;

public record RuleResponse(String code, String title, String description, int points, boolean enabled) {

    public static RuleResponse from(RuleConfig r) {
        return new RuleResponse(r.getCode(), r.getTitle(), r.getDescription(), r.getPoints(), r.isEnabled());
    }
}
