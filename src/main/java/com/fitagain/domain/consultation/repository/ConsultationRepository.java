package com.fitagain.domain.consultation.repository;

import com.fitagain.domain.consultation.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    boolean existsByDiagnosisTaskId(Long diagnosisTaskId);
}
