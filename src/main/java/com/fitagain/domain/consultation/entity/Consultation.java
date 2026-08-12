package com.fitagain.domain.consultation.entity;

import com.fitagain.domain.task.entity.DiagnosisTask;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_task_id", nullable = false)
    private DiagnosisTask diagnosisTask;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "additional_request", columnDefinition = "text")
    private String additionalRequest;

    @Column(name = "privacy_agreed", nullable = false)
    private Boolean privacyAgreed;
}
