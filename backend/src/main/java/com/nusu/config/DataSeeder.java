package com.nusu.config;

import com.nusu.model.Astronaut;
import com.nusu.model.AstronautStatus;
import com.nusu.model.ShipStatus;
import com.nusu.model.Spaceship;
import com.nusu.repository.AstronautRepository;
import com.nusu.repository.SpaceshipRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SpaceshipRepository ships;
    private final AstronautRepository astronauts;

    public DataSeeder(SpaceshipRepository ships, AstronautRepository astronauts) {
        this.ships = ships;
        this.astronauts = astronauts;
    }

    @Override
    public void run(String... args) {
        if (ships.count() > 0) {
            return;
        }
        Spaceship aurora = ship("Aurora-1", "Falcon-X", 4, ShipStatus.ACTIVE);
        Spaceship nebula = ship("Nebula", "Orion-M", 6, ShipStatus.MAINTENANCE);
        Spaceship vega = ship("Vega", "Starlink-S", 3, ShipStatus.ACTIVE);

        astronaut("Nguyễn Minh An", "Chỉ huy", "Việt Nam", AstronautStatus.ON_MISSION, aurora);
        astronaut("Trần Thu Hà", "Phi công", "Việt Nam", AstronautStatus.ON_MISSION, aurora);
        astronaut("Lê Quốc Bảo", "Kỹ sư", "Việt Nam", AstronautStatus.READY, vega);
        astronaut("Sarah Johnson", "Chuyên gia nhiệm vụ", "Mỹ", AstronautStatus.READY, null);
    }

    private Spaceship ship(String name, String model, int capacity, ShipStatus status) {
        Spaceship ship = new Spaceship();
        ship.setName(name);
        ship.setModel(model);
        ship.setCapacity(capacity);
        ship.setStatus(status);
        return ships.save(ship);
    }

    private void astronaut(String fullName, String rankName, String nationality, AstronautStatus status, Spaceship ship) {
        Astronaut astronaut = new Astronaut();
        astronaut.setFullName(fullName);
        astronaut.setRankName(rankName);
        astronaut.setNationality(nationality);
        astronaut.setStatus(status);
        astronaut.setSpaceship(ship);
        astronauts.save(astronaut);
    }
}
