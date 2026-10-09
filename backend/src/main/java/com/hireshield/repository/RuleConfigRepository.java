package com.hireshield.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.RuleConfig;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, String> {
}
