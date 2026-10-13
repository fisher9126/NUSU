package com.nusu.controller;

import com.nusu.dto.MissionLogRequest;
import com.nusu.dto.MissionRequest;
import com.nusu.model.Astronaut;
import com.nusu.model.LogLevel;
import com.nusu.model.Mission;
import com.nusu.model.MissionLog;
import com.nusu.model.MissionPriority;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.MissionLogRepository;
import com.nusu.repository.MissionRepository;
import com.nusu.repository.SpaceshipRepository;
import com.nusu.service.AuditService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionRepository missions;
    private final MissionLogRepository logs;
    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;
    private final AuditService auditService;

    public MissionController(MissionRepository missions,
                             MissionLogRepository logs,
                             SpaceshipRepository ships,
                             AstronautRepository astronauts,
                             AuditService auditService) {
        this.missions = missions;
        this.logs = logs;
        this.ships = ships;
        this.astronauts = astronauts;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Mission> list() {
        return missions.findAll(Sort.by("id"));
    }

    @GetMapping("/{id}")
    public Mission get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mission create(@Valid @RequestBody MissionRequest request,
                          @RequestHeader(value = "X-Actor", required = false) String actor) {
        Mission saved = missions.save(apply(new Mission(), request));
        auditService.log("CREATE_MISSION", actor, "Khởi tạo chiến dịch vũ trụ: " + saved.getCode() + " - " + saved.getName());

        MissionLog initLog = new MissionLog();
        initLog.setMission(saved);
        initLog.setLevel(LogLevel.INFO);
        initLog.setMessage("Chiến dịch " + saved.getName() + " chính thức được phê duyệt tại NUSU Command.");
        logs.save(initLog);

        return saved;
    }

    @PutMapping("/{id}")
    public Mission update(@PathVariable Long id, @Valid @RequestBody MissionRequest request,
                          @RequestHeader(value = "X-Actor", required = false) String actor) {
        Mission mission = find(id);
        Mission saved = missions.save(apply(mission, request));
        auditService.log("UPDATE_MISSION", actor, "Cập nhật chiến dịch: " + saved.getCode() + " [Trạng thái: " + saved.getStatus() + "]");
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "X-Actor", required = false) String actor) {
        Mission mission = find(id);
        for (Astronaut a : astronauts.findByMissionId(id)) {
            a.setMission(null);
        }
        missions.delete(mission);
        auditService.log("DELETE_MISSION", actor, "Hủy / Xóa chiến dịch: " + mission.getCode() + " - " + mission.getName());
    }

    @GetMapping("/{id}/logs")
    public List<MissionLog> getMissionLogs(@PathVariable Long id) {
        return logs.findByMissionIdOrderByTimestampDesc(id);
    }

    @PostMapping("/{id}/logs")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionLog addMissionLog(@PathVariable Long id, @Valid @RequestBody MissionLogRequest request,
                                    @RequestHeader(value = "X-Actor", required = false) String actor) {
        Mission mission = find(id);
        MissionLog entry = new MissionLog();
        entry.setMission(mission);
        entry.setLevel(request.level() != null ? request.level() : LogLevel.INFO);
        entry.setMessage(request.message().trim());
        entry.setTimestamp(LocalDateTime.now());
        MissionLog saved = logs.save(entry);
        auditService.log("ADD_MISSION_LOG", actor, "Ghi nhật ký nhiệm vụ " + mission.getCode() + ": " + saved.getMessage());
        return saved;
    }

    @GetMapping("/logs/recent")
    public List<MissionLog> getRecentLogs() {
        return logs.findTop20ByOrderByTimestampDesc();
    }

    private Mission find(Long id) {
        return missions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhiệm vụ"));
    }

    private Mission apply(Mission mission, MissionRequest request) {
        Spaceship ship = null;
        if (request.spaceshipId() != null) {
            ship = ships.findById(request.spaceshipId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tàu được chỉ định không tồn tại"));
        }

        mission.setCode(request.code().trim().toUpperCase());
        mission.setName(request.name().trim());
        mission.setDestination(request.destination().trim());
        mission.setStatus(request.status());
        mission.setPriority(request.priority() != null ? request.priority() : MissionPriority.MEDIUM);
        mission.setLaunchDate(request.launchDate());
        mission.setReturnDate(request.returnDate());
        if (request.budgetMillions() != null) {
            mission.setBudgetMillions(Math.max(0, request.budgetMillions()));
        }
        mission.setPrimaryObjective(request.primaryObjective() != null ? request.primaryObjective().trim() : "");
        mission.setSpaceship(ship);
        return mission;
    }
}
