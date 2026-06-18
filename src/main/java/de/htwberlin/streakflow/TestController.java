package de.htwberlin.streakflow;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "status", "ok",
                "service", "StreakFlow Backend",
                "hint", "Use /exercises, /progress, /executions or /shop/items"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/test")
    public String test() {
        return "Streakflow läuft!";
    }
}
