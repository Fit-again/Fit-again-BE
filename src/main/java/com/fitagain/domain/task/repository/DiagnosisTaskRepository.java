package com.fitagain.domain.task.repository;

import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisTaskRepository extends JpaRepository<DiagnosisTask, Long> {
    List<DiagnosisTask> findByStatus(TaskStatus status);
}