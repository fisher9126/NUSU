package com.nusu.dto;

import com.nusu.model.AstronautStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AstronautRequest(
        @NotBlank(message = "không được để trống") String fullName,
        @NotBlank(message = "không được để trống") String rankName,
        String nationality,
        @NotNull(message = "bắt buộc phải chọn") AstronautStatus status,
        Long spaceshipId,
        Long missionId,
        Integer spaceHours,
        Integer heartRate,
        Integer oxygenSaturation,
        Double radiationLevel,
        String bloodType,
        String bio,
        String badges) {
}
