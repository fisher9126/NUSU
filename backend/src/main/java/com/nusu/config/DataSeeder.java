package com.nusu.config;

import com.nusu.model.Astronaut;
import com.nusu.model.AstronautStatus;
import com.nusu.model.AuditLog;
import com.nusu.model.LogLevel;
import com.nusu.model.Mission;
import com.nusu.model.MissionLog;
import com.nusu.model.MissionPriority;
import com.nusu.model.MissionStatus;
import com.nusu.model.ShipStatus;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.AuditLogRepository;
import com.nusu.repository.MissionLogRepository;
import com.nusu.repository.MissionRepository;
import com.nusu.repository.SpaceshipRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;
    private final MissionRepository missions;
    private final MissionLogRepository logs;
    private final AuditLogRepository auditLogs;

    public DataSeeder(JdbcTemplate jdbcTemplate,
                      SpaceshipRepository ships,
                      AstronautRepository astronauts,
                      MissionRepository missions,
                      MissionLogRepository logs,
                      AuditLogRepository auditLogs) {
        this.jdbcTemplate = jdbcTemplate;
        this.ships = ships;
        this.astronauts = astronauts;
        this.missions = missions;
        this.logs = logs;
        this.auditLogs = auditLogs;
    }

    @Override
    public void run(String... args) {
        try {
            migrateSchemaSafely();
            seedDataSafely();
        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu DataSeeder: {}", e.getMessage(), e);
        }
    }

    private void migrateSchemaSafely() {
        try {
            // Tự động thêm các cột mới vào PostgreSQL nếu chưa tồn tại (tránh lỗi 500 do schema cũ)
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS fuel_level integer DEFAULT 100");
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS oxygen_level integer DEFAULT 100");
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS speed_kms float8 DEFAULT 7.8");
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS cargo_capacity_tons float8 DEFAULT 10.0");
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS manufacturer varchar(255) DEFAULT 'NUSU Aerospace'");
            jdbcTemplate.execute("ALTER TABLE spaceships ADD COLUMN IF NOT EXISTS launch_year integer DEFAULT 2024");

            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS mission_id bigint");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS space_hours integer DEFAULT 100");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS heart_rate integer DEFAULT 72");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS oxygen_saturation integer DEFAULT 98");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS radiation_level float8 DEFAULT 1.2");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS blood_type varchar(10) DEFAULT 'O+'");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS bio text");
            jdbcTemplate.execute("ALTER TABLE astronauts ADD COLUMN IF NOT EXISTS badges varchar(255) DEFAULT 'CREW_CERTIFIED'");
            log.info("Schema migration kiểm tra thành công.");
        } catch (Exception ex) {
            log.warn("Thông báo schema migration (bỏ qua nếu bảng chưa tạo): {}", ex.getMessage());
        }
    }

    private void seedDataSafely() {
        // 1. Cập nhật hoặc tạo Tàu vũ trụ
        Spaceship aurora, nebula, vega, daedalus;

        if (ships.count() == 0) {
            aurora = createShip("Aurora-1", "Falcon-Heavy X", 4, ShipStatus.ACTIVE, 92, 98, 11.2, 25.0, "NUSU Aerospace", 2024);
            nebula = createShip("Nebula Orbital", "Orion-M DeepSpace", 6, ShipStatus.MAINTENANCE, 45, 60, 7.8, 18.5, "Orbital Sciences", 2023);
            vega = createShip("Vega Starliner", "Starship Cargo v3", 6, ShipStatus.ACTIVE, 85, 95, 12.4, 50.0, "SpaceX Industry", 2025);
            daedalus = createShip("Daedalus-IX", "Prometheus Fusion", 8, ShipStatus.ACTIVE, 99, 100, 24.5, 80.0, "NUSU Deep Exploration", 2026);
            createShip("Phoenix Lunar", "Eagle-Lander Mk2", 3, ShipStatus.RETIRED, 0, 0, 0.0, 8.0, "Lunar Dynamics", 2020);
        } else {
            List<Spaceship> existingShips = ships.findAll();
            for (Spaceship s : existingShips) {
                if (s.getFuelLevel() <= 0 && s.getStatus() == ShipStatus.ACTIVE) s.setFuelLevel(90);
                if (s.getOxygenLevel() <= 0 && s.getStatus() == ShipStatus.ACTIVE) s.setOxygenLevel(98);
                if (s.getSpeedKmS() <= 0 && s.getStatus() == ShipStatus.ACTIVE) s.setSpeedKmS(7.8);
                if (s.getCargoCapacityTons() <= 0) s.setCargoCapacityTons(15.0);
                if (s.getManufacturer() == null || s.getManufacturer().isBlank()) s.setManufacturer("NUSU Aerospace");
                if (s.getLaunchYear() == null) s.setLaunchYear(2024);
                ships.save(s);
            }
            aurora = existingShips.get(0);
            nebula = existingShips.size() > 1 ? existingShips.get(1) : aurora;
            vega = existingShips.size() > 2 ? existingShips.get(2) : aurora;
            daedalus = existingShips.size() > 3 ? existingShips.get(3) : aurora;
        }

        // 2. Tạo Chiến dịch vũ trụ nếu chưa có
        if (missions.count() == 0) {
            Mission artemis = createMission(
                    "ARTEMIS-VII",
                    "Khảo sát Cực Nam Mặt Trăng",
                    "Mặt Trăng (Miệng núi Shackleton)",
                    MissionStatus.IN_TRANSIT,
                    MissionPriority.CRITICAL,
                    "2026-09-15",
                    "2026-11-20",
                    480.0,
                    "Tìm kiếm trầm tích băng nước vĩnh cửu tại cực nam và lắp ráp mô-đun năng lượng pin mặt trời.",
                    aurora
            );

            Mission ares = createMission(
                    "ARES-ONE",
                    "Thám hiểm Tiền trạm Sao Hỏa",
                    "Sao Hỏa (Đồng bằng Utopia)",
                    MissionStatus.COUNTDOWN,
                    MissionPriority.HIGH,
                    "2026-12-01",
                    "2028-05-15",
                    1250.0,
                    "Triển khai robot tự hành địa chất và kiểm tra khả năng sản xuất oxy tại chỗ (MOXIE-2).",
                    daedalus
            );

            Mission issExp = createMission(
                    "EXPEDITION-74",
                    "Bảo trì Trạm Vũ trụ Quốc tế",
                    "Quỹ đạo Trái Đất (LEO 408 km)",
                    MissionStatus.ORBITING,
                    MissionPriority.MEDIUM,
                    "2026-08-01",
                    "2027-02-15",
                    120.0,
                    "Thay thế mảng pin mặt trời iROSA và nghiên cứu tinh thể sinh học trong môi trường vi trọng lực.",
                    vega
            );

            // Tạo Nhật ký cho các nhiệm vụ
            createLog(artemis, LogLevel.SUCCESS, "Tàu Aurora-1 hoàn tất cú đốt TLI (Trans-Lunar Injection) thành công rực rỡ.");
            createLog(artemis, LogLevel.INFO, "Phi hành đoàn đã vào trạng thái quỹ đạo chuyển tiếp an toàn. Chỉ số oxy ổn định 98%.");
            createLog(artemis, LogLevel.WARNING, "Cảm biến báo nhiệt độ tấm pin mạn phải tăng nhẹ 3.2°C do góc chiếu Mặt Trời.");

            createLog(ares, LogLevel.INFO, "Hệ thống tự động đã hoàn thành việc nạp nhiên liệu Methane-Oxy lỏng giai đoạn 1.");
            createLog(ares, LogLevel.INFO, "Đếm ngược T-minus 48 ngày cho chuyến hành trình lịch sử đến Sao Hỏa.");

            createLog(issExp, LogLevel.SUCCESS, "Ghép nối thành công vào mô-đun Harmony ở độ cao 412 km.");

            // Gán nhiệm vụ cho phi hành gia nếu có sẵn
            List<Astronaut> existingAstronauts = astronauts.findAll();
            if (!existingAstronauts.isEmpty()) {
                for (int i = 0; i < existingAstronauts.size(); i++) {
                    Astronaut a = existingAstronauts.get(i);
                    if (a.getMission() == null) {
                        a.setMission(i % 2 == 0 ? artemis : issExp);
                    }
                    if (a.getHeartRate() <= 0) a.setHeartRate(72);
                    if (a.getOxygenSaturation() <= 0) a.setOxygenSaturation(98);
                    if (a.getBadges() == null || a.getBadges().isBlank()) a.setBadges("CREW_CERTIFIED,EVA_SPECIALIST");
                    astronauts.save(a);
                }
            }
        }

        // 3. Tạo Phi hành gia nếu danh sách trống
        if (astronauts.count() == 0) {
            Mission artemis = missions.findAll().isEmpty() ? null : missions.findAll().get(0);
            createAstronaut("Nguyễn Minh An", "Chỉ huy Trưởng", "Việt Nam", AstronautStatus.ON_MISSION, aurora, artemis,
                    1450, 68, 99, 1.15, "O+",
                    "Cựu phi công thử nghiệm quân sự, đã có 3 chuyến bay vào không gian và 12 giờ đi bộ EVA ngoài không gian.",
                    "MOONWALKER,COMMAND_CREW,EVA_SPECIALIST");

            createAstronaut("Trần Thu Hà", "Phi công Chuyến bay", "Việt Nam", AstronautStatus.ON_MISSION, aurora, artemis,
                    920, 72, 98, 0.95, "A+",
                    "Chuyên gia điều hướng quỹ đạo và hạ cánh bề mặt thiên thể. Kỹ sư hàng không vũ trụ ĐHBK.",
                    "PILOT_ACE,COMMAND_CREW");

            createAstronaut("Lê Quốc Bảo", "Kỹ sư Hệ thống", "Việt Nam", AstronautStatus.READY, vega, artemis,
                    640, 75, 97, 0.82, "B+",
                    "Chuyên gia robot tự hành và sửa chữa khoang nén chân không. Trưởng đội kỹ thuật NUSU.",
                    "MASTER_ENGINEER,CREW_CERTIFIED");
        }

        // 4. Tạo Audit logs khởi đầu nếu chưa có
        if (auditLogs.count() == 0) {
            auditLogs.save(new AuditLog("SYSTEM_BOOT", "Kernel", "Hệ thống NUSU Mission Control khởi động toàn diện."));
            auditLogs.save(new AuditLog("DISPATCH_MISSION", "Giám đốc Nhiệm vụ", "Phê duyệt chiến dịch ARTEMIS-VII và ARES-ONE."));
        }
    }

    private Spaceship createShip(String name, String model, int capacity, ShipStatus status,
                                 int fuel, int oxygen, double speed, double cargo, String manufacturer, int launchYear) {
        Spaceship s = new Spaceship();
        s.setName(name);
        s.setModel(model);
        s.setCapacity(capacity);
        s.setStatus(status);
        s.setFuelLevel(fuel);
        s.setOxygenLevel(oxygen);
        s.setSpeedKmS(speed);
        s.setCargoCapacityTons(cargo);
        s.setManufacturer(manufacturer);
        s.setLaunchYear(launchYear);
        return ships.save(s);
    }

    private Mission createMission(String code, String name, String destination, MissionStatus status,
                                  MissionPriority priority, String launch, String ret, double budget,
                                  String objective, Spaceship ship) {
        Mission m = new Mission();
        m.setCode(code);
        m.setName(name);
        m.setDestination(destination);
        m.setStatus(status);
        m.setPriority(priority);
        m.setLaunchDate(launch);
        m.setReturnDate(ret);
        m.setBudgetMillions(budget);
        m.setPrimaryObjective(objective);
        m.setSpaceship(ship);
        m.setCreatedAt(LocalDateTime.now());
        return missions.save(m);
    }

    private void createLog(Mission mission, LogLevel level, String message) {
        MissionLog l = new MissionLog();
        l.setMission(mission);
        l.setLevel(level);
        l.setMessage(message);
        l.setTimestamp(LocalDateTime.now());
        logs.save(l);
    }

    private void createAstronaut(String name, String rank, String nationality, AstronautStatus status,
                                 Spaceship ship, Mission mission, int hours, int bpm, int o2, double rad,
                                 String blood, String bio, String badges) {
        Astronaut a = new Astronaut();
        a.setFullName(name);
        a.setRankName(rank);
        a.setNationality(nationality);
        a.setStatus(status);
        a.setSpaceship(ship);
        a.setMission(mission);
        a.setSpaceHours(hours);
        a.setHeartRate(bpm);
        a.setOxygenSaturation(o2);
        a.setRadiationLevel(rad);
        a.setBloodType(blood);
        a.setBio(bio);
        a.setBadges(badges);
        astronauts.save(a);
    }
}
