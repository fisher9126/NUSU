package com.nusu.controller;

import com.nusu.dto.AstronautRequest;
import com.nusu.model.Astronaut;
import com.nusu.model.Mission;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.MissionRepository;
import com.nusu.repository.SpaceshipRepository;
import com.nusu.service.AuditService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/astronauts")
public class AstronautController {

    private final AstronautRepository astronauts;
    private final SpaceshipRepository ships;
    private final MissionRepository missions;
    private final AuditService auditService;

    public AstronautController(AstronautRepository astronauts,
                               SpaceshipRepository ships,
                               MissionRepository missions,
                               AuditService auditService) {
        this.astronauts = astronauts;
        this.ships = ships;
        this.missions = missions;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Astronaut> list() {
        return astronauts.findAll(Sort.by("id"));
    }

    @GetMapping("/{id}")
    public Astronaut get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Astronaut create(@Valid @RequestBody AstronautRequest request,
                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        Astronaut saved = astronauts.save(apply(new Astronaut(), request));
        auditService.log("CREATE_ASTRONAUT", actor, "Thêm phi hành gia: " + saved.getFullName() + " (" + saved.getRankName() + ")");
        return saved;
    }

    @PutMapping("/{id}")
    public Astronaut update(@PathVariable Long id,
                            @Valid @RequestBody AstronautRequest request,
                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        Astronaut saved = astronauts.save(apply(find(id), request));
        auditService.log("UPDATE_ASTRONAUT", actor, "Cập nhật phi hành gia: " + saved.getFullName() + " [Trạng thái: " + saved.getStatus() + "]");
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "X-Actor", required = false) String actor) {
        Astronaut astronaut = find(id);
        astronauts.delete(astronaut);
        auditService.log("DELETE_ASTRONAUT", actor, "Xóa phi hành gia: " + astronaut.getFullName());
    }

    private Astronaut find(Long id) {
        return astronauts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phi hành gia"));
    }

    private Astronaut apply(Astronaut astronaut, AstronautRequest request) {
        Spaceship ship = null;
        if (request.spaceshipId() != null) {
            ship = ships.findById(request.spaceshipId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tàu được chọn không tồn tại"));
            boolean alreadyOnShip = astronaut.getSpaceship() != null
                    && astronaut.getSpaceship().getId().equals(ship.getId());
            if (!alreadyOnShip && astronauts.countBySpaceshipId(ship.getId()) >= ship.getCapacity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tàu " + ship.getName() + " đã đủ chỗ");
            }
        }

        Mission mission = null;
        if (request.missionId() != null) {
            mission = missions.findById(request.missionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhiệm vụ được chọn không tồn tại"));
        }

        astronaut.setFullName(request.fullName().trim());
        astronaut.setRankName(request.rankName().trim());
        astronaut.setNationality(request.nationality() == null ? null : request.nationality().trim());
        astronaut.setStatus(request.status());
        astronaut.setSpaceship(ship);
        astronaut.setMission(mission);

        if (request.spaceHours() != null) astronaut.setSpaceHours(Math.max(0, request.spaceHours()));
        if (request.heartRate() != null) astronaut.setHeartRate(Math.max(30, Math.min(220, request.heartRate())));
        if (request.oxygenSaturation() != null) astronaut.setOxygenSaturation(Math.max(70, Math.min(100, request.oxygenSaturation())));
        if (request.radiationLevel() != null) astronaut.setRadiationLevel(Math.max(0.0, request.radiationLevel()));
        if (request.bloodType() != null && !request.bloodType().isBlank()) astronaut.setBloodType(request.bloodType().trim());
        if (request.bio() != null) astronaut.setBio(request.bio().trim());
        if (request.badges() != null) astronaut.setBadges(request.badges().trim());

        return astronaut;
    }
}
