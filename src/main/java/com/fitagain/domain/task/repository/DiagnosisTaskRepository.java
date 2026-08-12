package com.fitagain.domain.task.repository;

import com.fitagain.domain.task.entity.DiagnosisTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisTaskRepository extends JpaRepository<DiagnosisTask, Long> {
}