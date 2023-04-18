package de.nubisoft.backend.api;

import de.nubisoft.backend.domain.Stats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stats")
@RestController
public class StatsController {

    @GetMapping
    Stats getStats() {
        return new Stats(
                Runtime.getRuntime().totalMemory() / 1000000,
                Runtime.getRuntime().maxMemory() / 1000000,
                Runtime.getRuntime().freeMemory() / 1000000
        );
    }
}
