package com.hireshield.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hireshield.dto.RuleResponse;
import com.hireshield.dto.RuleUpdateRequest;
import com.hireshield.entity.RuleConfig;
import com.hireshield.exception.NotFoundException;
import com.hireshield.repository.RuleConfigRepository;
import com.hireshield.rules.RuleEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleAdminService {

    private final RuleConfigRepository repository;
    private final RuleEngine ruleEngine;

    @Transactional(readOnly = true)
    public List<RuleResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(RuleConfig::getPoints).reversed())
                .map(RuleResponse::from)
                .toList();
    }

    @Transactional
    public List<RuleResponse> update(List<RuleUpdateRequest> updates) {
        for (RuleUpdateRequest update : updates) {
            RuleConfig rule = repository.findById(update.code())
                    .orElseThrow(() -> new NotFoundException("Unknown rule " + update.code()));
            rule.setPoints(update.points());
            rule.setEnabled(update.enabled());
        }
        repository.flush();
        ruleEngine.reloadConfig();
        return list();
    }
}
