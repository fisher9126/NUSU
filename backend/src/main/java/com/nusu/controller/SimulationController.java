package com.nusu.controller;

import com.nusu.service.AuditService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final AuditService auditService;
    private final Random random = new Random();

    public SimulationController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/telemetry")
    public Map<String, Object> getLiveTelemetry() {
        // Tọa độ quỹ đạo ISS ước tính và chỉ số dao động tự nhiên
        double altitudeKm = 408.0 + (random.nextDouble() * 4.0 - 2.0);
        double velocityKmS = 7.66 + (random.nextDouble() * 0.04 - 0.02);
        double solarFlux = 1361.0 + (random.nextDouble() * 10.0 - 5.0);
        double cabinPressureKpa = 101.3 + (random.nextDouble() * 0.4 - 0.2);

        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("issAltitudeKm", Math.round(altitudeKm * 100.0) / 100.0);
        telemetry.put("issVelocityKmS", Math.round(velocityKmS * 100.0) / 100.0);
        telemetry.put("solarFluxWsqm", Math.round(solarFlux * 10.0) / 10.0);
        telemetry.put("cabinPressureKpa", Math.round(cabinPressureKpa * 10.0) / 10.0);
        telemetry.put("deepSpaceNetworkStatus", "OPTIMAL - GOLDSTONE / CANBERRA / MADRID LINKED");
        telemetry.put("timestamp", System.currentTimeMillis());
        return telemetry;
    }

    @GetMapping("/crisis")
    public List<Map<String, Object>> getCrisisScenarios() {
        List<Map<String, Object>> crises = new ArrayList<>();

        crises.add(Map.of(
                "id", "CRISIS-001",
                "title", "Rò rỉ hệ thống đẩy RCS - Tàu Aurora-1",
                "severity", "HIGH",
                "description", "Cảm biến áp suất khoang động cơ phụ ghi nhận sụt giảm 18% khí nén Nitơ lỏng. Nguy cơ mất kiểm soát tư thế quay.",
                "options", List.of(
                        Map.of("id", "opt1", "text", "Cách ly van phân nhánh 3 và chuyển sang hệ thống dự phòng B", "successRate", 95, "costScore", "+50 ĐIỂM KỸ THUẬT"),
                        Map.of("id", "opt2", "text", "Xả toàn bộ khí nén còn lại và kích hoạt chế độ quán tính trôi dạt", "successRate", 60, "costScore", "+10 ĐIỂM"),
                        Map.of("id", "opt3", "text", "Tiến hành đi bộ ngoài không gian (EVA) hàn ống khẩn cấp", "successRate", 75, "costScore", "+80 ĐIỂM DŨNG CẢM")
                )
        ));

        crises.add(Map.of(
                "id", "CRISIS-002",
                "title", "Cảnh báo Mưa Bão Mặt Trời Cấp X-1.2",
                "severity", "CRITICAL",
                "description", "Trạm quan sát SOHO phát hiện vết đen Mặt Trời phóng khối lượng cực lớn (CME) trực diện quỹ đạo Trái Đất - Mặt Trăng.",
                "options", List.of(
                        Map.of("id", "opt1", "text", "Đưa phi hành đoàn vào hầm trú bão bức xạ nước (Storm Shelter) và tắt thiết bị nhạy cảm", "successRate", 98, "costScore", "+100 ĐIỂM BẢO TOÀN"),
                        Map.of("id", "opt2", "text", "Đổi hướng tấm pin năng lượng và duy trì thông tin liên lạc bình thường", "successRate", 45, "costScore", "-20 ĐIỂM NGUY HIỂM"),
                        Map.of("id", "opt3", "text", "Đốt động cơ khẩn cấp để hạ độ cao quỹ đạo vào từ trường bảo vệ", "successRate", 80, "costScore", "+40 ĐIỂM")
                )
        ));

        crises.add(Map.of(
                "id", "CRISIS-003",
                "title", "Mảnh vỡ quỹ đạo kích thước 12cm áp sát tốc độ 14 km/s",
                "severity", "CRITICAL",
                "description", "Mạng lưới giám sát không gian Space Track cảnh báo mảnh vỡ từ tầng đẩy cũ sẽ đi qua hành lang bay trong vòng 42 phút.",
                "options", List.of(
                        Map.of("id", "opt1", "text", "Thực hiện cú đốt động cơ né tránh quỹ đạo (Debris Avoidance Maneuver - DAM)", "successRate", 96, "costScore", "+90 ĐIỂM CHIẾN THUẬT"),
                        Map.of("id", "opt2", "text", "Khóa các cửa ngăn khoang và cho phi hành đoàn mặc bộ đồ du hành vũ trụ EVA", "successRate", 70, "costScore", "+30 ĐIỂM PHÒNG NGỰ"),
                        Map.of("id", "opt3", "text", "Không can thiệp, xác suất va chạm ước tính chỉ 1/10.000", "successRate", 30, "costScore", "-50 ĐIỂM LIỀU LĨNH")
                )
        ));

        return crises;
    }

    @PostMapping("/crisis/resolve")
    public Map<String, Object> resolveCrisis(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-Actor", required = false) String actor) {
        String crisisId = body.getOrDefault("crisisId", "CRISIS-001");
        String choiceId = body.getOrDefault("choiceId", "opt1");
        String choiceText = body.getOrDefault("choiceText", "Thao tác ứng phó");

        boolean isOpt1 = "opt1".equals(choiceId);
        boolean success = isOpt1 || random.nextInt(100) < 70;

        String resultMsg = success
                ? "Thành công xuất sắc! Mối nguy hiểm đã được xử lý triệt để, hệ thống trở lại trạng thái an toàn."
                : "Cảnh báo: Quyết định gặp trở ngại, tổn hao thêm tài nguyên phụ trợ nhưng không gây thiệt hại sinh mạng.";

        auditService.log("CRISIS_ACTION", actor, "Xử lý sự cố [" + crisisId + "]: " + choiceText + " -> " + (success ? "THÀNH CÔNG" : "CÓ RỦI RO"));

        Map<String, Object> res = new HashMap<>();
        res.put("success", success);
        res.put("message", resultMsg);
        res.put("commanderScore", success ? "+100 EXP" : "+30 EXP");
        return res;
    }

    @GetMapping("/calculator")
    public Map<String, Object> calculateMission(
            @RequestParam(defaultValue = "MOON") String destination,
            @RequestParam(defaultValue = "20") double payloadTons,
            @RequestParam(defaultValue = "4") int crewCount) {

        double deltaV;
        int transitDays;
        double baseCostMillions;

        switch (destination.toUpperCase()) {
            case "MARS" -> {
                deltaV = 9.8;
                transitDays = 210;
                baseCostMillions = 850.0;
            }
            case "EUROPA" -> {
                deltaV = 14.5;
                transitDays = 1050;
                baseCostMillions = 2400.0;
            }
            case "LEO" -> {
                deltaV = 3.2;
                transitDays = 1;
                baseCostMillions = 65.0;
            }
            case "MOON" -> {
                deltaV = 5.9;
                transitDays = 3;
                baseCostMillions = 280.0;
            }
            default -> {
                deltaV = 5.9;
                transitDays = 3;
                baseCostMillions = 280.0;
            }
        }

        double fuelNeededTons = payloadTons * Math.exp(deltaV / 3.1) - payloadTons;
        double totalCost = baseCostMillions + (payloadTons * 1.5) + (crewCount * 12.0);

        Map<String, Object> result = new HashMap<>();
        result.put("destination", destination);
        result.put("deltaVKmS", Math.round(deltaV * 10.0) / 10.0);
        result.put("fuelNeededTons", Math.round(fuelNeededTons * 10.0) / 10.0);
        result.put("transitDays", transitDays);
        result.put("crewCount", crewCount);
        result.put("estimatedCostMillionsUSD", Math.round(totalCost * 10.0) / 10.0);
        result.put("recommendation", "Cần sử dụng động cơ chu trình đốt giai đoạn (Staged Combustion) kèm tên lửa đẩy hạng nặng.");
        return result;
    }

    @GetMapping("/nasa/apod")
    public Map<String, Object> getApod() {
        Map<String, Object> apod = new HashMap<>();
        apod.put("title", "Cực quang trên Đảo Lofoten & Dải Ngân Hà sâu thẳm");
        apod.put("date", LocalDate.now().toString());
        apod.put("explanation", "Một luồng gió Mặt Trời tốc độ cao va chạm với từ quyển Trái Đất tạo nên dải lụa cực quang rực rỡ màu xanh ngọc lục bảo và ánh tím huyền bí. Ở hậu cảnh, tâm dải Ngân Hà lấp lánh với hàng tỷ vì sao, biểu trưng cho khát vọng thám hiểm vũ trụ vô tận của nhân loại.");
        apod.put("copyright", "NASA / ESA Astronomy Archive");
        apod.put("url", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=1200&q=80");
        return apod;
    }
}
