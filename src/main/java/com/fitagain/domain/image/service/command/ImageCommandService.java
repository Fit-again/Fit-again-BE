package com.fitagain.domain.image.service.command;

import com.fitagain.domain.image.dto.response.ImageAnalysisResDTO.ImageAnalysisDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;

public interface ImageCommandService {
    ImageAnalysisDTO analyzeAndUploadImages(MultipartFile frontImage, List<MultipartFile> detailImages) throws IOException;
}
