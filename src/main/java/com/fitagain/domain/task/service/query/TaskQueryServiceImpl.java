package com.fitagain.domain.task.service.query;

import com.fitagain.domain.task.dto.response.DiagnosisResDTO;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryServiceImpl implements TaskQueryService {

    private final DiagnosisTaskRepository taskRepository;

    @Override
    public DiagnosisResDTO.DiagnosisResultDTO getDiagnosisStatus(Long taskId) {

        DiagnosisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("해당 작업을 찾을 수 없습니다."));

        Map<String, Object> mergedResult = null;
        if (task.getDiagnosisResult() != null) {
            mergedResult = new HashMap<>(task.getDiagnosisResult());
            
            // 이미지 전체 리스트화
            List<String> allImages = new ArrayList<>();
            if (task.getFrontImageUrl() != null) allImages.add(task.getFrontImageUrl());
            if (task.getDetailImageUrls() != null) allImages.addAll(task.getDetailImageUrls());
            if (task.getDamageImageUrls() != null) allImages.addAll(task.getDamageImageUrls());
            
            mergedResult.put("allImages", allImages);
            mergedResult.put("productType", task.getProductType());
        }

        return new DiagnosisResDTO.DiagnosisResultDTO(
                task.getStatus().name(),
                mergedResult,
                task.getErrorMessage()
        );
    }
}
