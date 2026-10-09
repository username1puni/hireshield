package com.hireshield.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hireshield.entity.UploadedFile;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
}
