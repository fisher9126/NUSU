package com.nusu.controller;

import com.nusu.dto.SpaceshipRequest;
import com.nusu.model.Astronaut;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.SpaceshipRepository;
import com.nusu.service.AuditService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/spaceships")
public class SpaceshipController {

    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;
    private final AuditService auditService;

    public SpaceshipController(SpaceshipRepository ships, AstronautRepository astronauts, AuditService auditService) {
        this.ships = ships;
        this.astronauts = astronauts;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Spaceship> list() {
        return ships.findAll(Sort.by("id"));
    }

    @GetMapping("/{id}")
    public Spaceship get(@PathVariable Long id) {
        return find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Spaceship create(@Valid @RequestBody SpaceshipRequest request,
                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        Spaceship saved = ships.save(apply(new Spaceship(), request));
        auditService.log("CREATE_SPACESHIP", actor, "Thêm tàu vũ trụ mới: " + saved.getName() + " (" + saved.getModel() + ")");
        return saved;
    }

    @PutMapping("/{id}")
    public Spaceship update(@PathVariable Long id, @Valid @RequestBody SpaceshipRequest request,
                            @RequestHeader(value = "X-Actor", required = false) String actor) {
        Spaceship ship = find(id);
        if (request.capacity() < astronauts.countBySpaceshipId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sức chứa nhỏ hơn số phi hành gia đang ở trên tàu");
        }
        Spaceship saved = ships.save(apply(ship, request));
        auditService.log("UPDATE_SPACESHIP", actor, "Cập nhật tàu vũ trụ: " + saved.getName() + " [Trạng thái: " + saved.getStatus() + "]");
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "X-Actor", required = false) String actor) {
        Spaceship ship = find(id);
        for (Astronaut astronaut : astronauts.findBySpaceshipId(id)) {
            astronaut.setSpaceship(null);
        }
        ships.delete(ship);
        auditService.log("DELETE_SPACESHIP", actor, "Xóa tàu vũ trụ: " + ship.getName());
    }

    private Spaceship find(Long id) {
        return ships.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tàu vũ trụ"));
    }

    private Spaceship apply(Spaceship ship, SpaceshipRequest request) {
        ship.setName(request.name().trim());
        ship.setModel(request.model().trim());
        ship.setCapacity(request.capacity());
        ship.setStatus(request.status());
        if (request.fuelLevel() != null) ship.setFuelLevel(Math.max(0, Math.min(100, request.fuelLevel())));
        if (request.oxygenLevel() != null) ship.setOxygenLevel(Math.max(0, Math.min(100, request.oxygenLevel())));
        if (request.speedKmS() != null) ship.setSpeedKmS(Math.max(0, request.speedKmS()));
        if (request.cargoCapacityTons() != null) ship.setCargoCapacityTons(Math.max(0, request.cargoCapacityTons()));
        if (request.manufacturer() != null && !request.manufacturer().isBlank()) ship.setManufacturer(request.manufacturer().trim());
        if (request.launchYear() != null) ship.setLaunchYear(request.launchYear());
        return ship;
    }
}
