package com.hireshield.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.transaction.annotation.Transactional;

import com.hireshield.entity.ModelVersion;

public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {

    Optional<ModelVersion> findFirstByActiveTrue();

    Optional<ModelVersion> findByVersion(String version);

    @Transactional
    @Modifying
    @Query("update ModelVersion m set m.active = false where m.active = true")
    void deactivateAll();
}
