package com.nusu.controller;

import com.nusu.model.AstronautStatus;
import com.nusu.model.ShipStatus;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.SpaceshipRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    public record Stats(long ships, long activeShips, long astronauts, long onMission) {
    }

    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;

    public StatsController(SpaceshipRepository ships, AstronautRepository astronauts) {
        this.ships = ships;
        this.astronauts = astronauts;
    }

    @GetMapping
    public Stats stats() {
        return new Stats(
                ships.count(),
                ships.countByStatus(ShipStatus.ACTIVE),
                astronauts.count(),
                astronauts.countByStatus(AstronautStatus.ON_MISSION));
    }
}
