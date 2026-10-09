package com.hireshield.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.LinkCheck;

public interface LinkCheckRepository extends JpaRepository<LinkCheck, String> {
}
