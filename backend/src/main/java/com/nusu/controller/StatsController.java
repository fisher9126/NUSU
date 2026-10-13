package com.nusu.controller;

import com.nusu.model.AstronautStatus;
import com.nusu.model.Mission;
import com.nusu.model.MissionStatus;
import com.nusu.model.ShipStatus;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.MissionRepository;
import com.nusu.repository.SpaceshipRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    public record Stats(
            long ships,
            long activeShips,
            long astronauts,
            long onMission,
            long missions,
            long activeMissions,
            int avgFuel,
            int avgOxygen,
            double totalBudgetMillions) {
    }

    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;
    private final MissionRepository missions;

    public StatsController(SpaceshipRepository ships, AstronautRepository astronauts, MissionRepository missions) {
        this.ships = ships;
        this.astronauts = astronauts;
        this.missions = missions;
    }

    @GetMapping
    public Stats stats() {
        List<Spaceship> allShips = ships.findAll();
        List<Mission> allMissions = missions.findAll();

        int avgFuel = allShips.isEmpty() ? 100 : (int) allShips.stream().mapToInt(Spaceship::getFuelLevel).average().orElse(100);
        int avgOxygen = allShips.isEmpty() ? 100 : (int) allShips.stream().mapToInt(Spaceship::getOxygenLevel).average().orElse(100);

        long activeMissions = allMissions.stream().filter(m ->
                m.getStatus() == MissionStatus.IN_TRANSIT ||
                m.getStatus() == MissionStatus.ORBITING ||
                m.getStatus() == MissionStatus.LANDED ||
                m.getStatus() == MissionStatus.COUNTDOWN).count();

        double totalBudget = allMissions.stream().mapToDouble(Mission::getBudgetMillions).sum();

        return new Stats(
                allShips.size(),
                ships.countByStatus(ShipStatus.ACTIVE),
                astronauts.count(),
                astronauts.countByStatus(AstronautStatus.ON_MISSION),
                allMissions.size(),
                activeMissions,
                avgFuel,
                avgOxygen,
                Math.round(totalBudget * 10.0) / 10.0
        );
    }
}
