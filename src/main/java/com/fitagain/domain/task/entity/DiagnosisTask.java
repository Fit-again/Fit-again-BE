package com.fitagain.domain.task.entity;

import com.fitagain.domain.task.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "diagnosis_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiagnosisTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "front_image_url", nullable = false, columnDefinition = "text")
    private String frontImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_image_urls", columnDefinition = "json")
    private List<String> detailImageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "damage_image_urls", columnDefinition = "json")
    private List<String> damageImageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "json")
    private List<String> keywords;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diagnosis_result", columnDefinition = "json")
    private Map<String, Object> diagnosisResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_result", columnDefinition = "json")
    private Map<String, Object> recommendationResult;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
