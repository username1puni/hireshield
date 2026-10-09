package com.hireshield.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByAnalysisIdAndUserId(Long analysisId, Long userId);

    long countByCorrect(boolean correct);
}
