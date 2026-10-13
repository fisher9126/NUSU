package com.nusu.dto;

import com.nusu.model.MissionPriority;
import com.nusu.model.MissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MissionRequest(
        @NotBlank(message = "Mã nhiệm vụ không được để trống") String code,
        @NotBlank(message = "Tên nhiệm vụ không được để trống") String name,
        @NotBlank(message = "Điểm đến không được để trống") String destination,
        @NotNull(message = "Trạng thái bắt buộc phải chọn") MissionStatus status,
        MissionPriority priority,
        String launchDate,
        String returnDate,
        Double budgetMillions,
        String primaryObjective,
        Long spaceshipId) {
}
