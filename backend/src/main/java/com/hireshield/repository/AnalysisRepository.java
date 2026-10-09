package com.hireshield.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hireshield.entity.Analysis;
import com.hireshield.entity.RiskLevel;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Page<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Analysis> findByUserIdAndRiskLevelOrderByCreatedAtDesc(Long userId, RiskLevel riskLevel, Pageable pageable);

    @EntityGraph(attributePaths = "flags")
    Optional<Analysis> findWithFlagsByIdAndUserId(Long id, Long userId);

    long countByRiskLevel(RiskLevel riskLevel);

    long countByCreatedAtAfter(Instant since);

    @Query("select a.riskLevel, count(a) from Analysis a where a.user.id = :userId group by a.riskLevel")
    List<Object[]> countByRiskForUser(@Param("userId") Long userId);

    @Query("select f.ruleCode, count(f) from AnalysisFlag f group by f.ruleCode order by count(f) desc")
    List<Object[]> countFlagsByRule();
}
