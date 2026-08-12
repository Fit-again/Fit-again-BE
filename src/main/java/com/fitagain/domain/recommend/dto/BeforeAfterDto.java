package com.fitagain.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BeforeAfterDto {
    private ImagePointsDto before;
    private ImagePointsDto after;
}