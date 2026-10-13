package com.nusu.controller;

import com.nusu.dto.AstronautRequest;
import com.nusu.model.Astronaut;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.SpaceshipRepository;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/astronauts")
public class AstronautController {

    private final AstronautRepository astronauts;
    private final SpaceshipRepository ships;

    public AstronautController(AstronautRepository astronauts, SpaceshipRepository ships) {
        this.astronauts = astronauts;
        this.ships = ships;
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
    public Astronaut create(@Valid @RequestBody AstronautRequest request) {
        return astronauts.save(apply(new Astronaut(), request));
    }

    @PutMapping("/{id}")
    public Astronaut update(@PathVariable Long id, @Valid @RequestBody AstronautRequest request) {
        return astronauts.save(apply(find(id), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        astronauts.delete(find(id));
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
        astronaut.setFullName(request.fullName().trim());
        astronaut.setRankName(request.rankName().trim());
        astronaut.setNationality(request.nationality() == null ? null : request.nationality().trim());
        astronaut.setStatus(request.status());
        astronaut.setSpaceship(ship);
        return astronaut;
    }
}
