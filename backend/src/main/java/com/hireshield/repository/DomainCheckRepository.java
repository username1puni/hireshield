package com.hireshield.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.DomainCheck;

public interface DomainCheckRepository extends JpaRepository<DomainCheck, String> {
}
