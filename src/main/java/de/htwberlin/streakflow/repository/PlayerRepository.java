package de.htwberlin.streakflow.repository;

import de.htwberlin.streakflow.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
