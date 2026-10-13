package com.nusu.dto;

import com.nusu.model.LogLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MissionLogRequest(
        @NotNull(message = "Cần chọn nhiệm vụ") Long missionId,
        LogLevel level,
        @NotBlank(message = "Nội dung nhật ký không được để trống") String message) {
}
