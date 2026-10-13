package com.nusu.dto;

import com.nusu.model.ShipStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpaceshipRequest(
        @NotBlank(message = "không được để trống") String name,
        @NotBlank(message = "không được để trống") String model,
        @Min(value = 1, message = "phải từ 1 trở lên") int capacity,
        @NotNull(message = "bắt buộc phải chọn") ShipStatus status) {
}
