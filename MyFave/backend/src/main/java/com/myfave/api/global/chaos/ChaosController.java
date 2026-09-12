package com.myfave.api.global.chaos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// [DRILL] 부하 중 장애를 라이브 토글. @Profile("chaos") 이므로 일반 실행엔 존재하지 않음.
// 실제 경로: /api/v1/internal/chaos/pg  (context-path /api/v1 포함)
@Slf4j
@RestController
@RequestMapping("/internal/chaos")
@Profile("chaos")
@RequiredArgsConstructor
public class ChaosController {

    private final ChaosProperties chaos;

    @PostMapping("/pg")
    public Map<String, Object> togglePg(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Long latencyMs,
            @RequestParam(required = false) ChaosProperties.Mode mode) {
        if (enabled != null) chaos.setEnabled(enabled);
        if (latencyMs != null) chaos.setLatencyMs(latencyMs);
        if (mode != null) chaos.setMode(mode);
        log.warn("[Chaos] PG 주입 상태 변경 → enabled={}, latencyMs={}, mode={}",
                chaos.isEnabled(), chaos.getLatencyMs(), chaos.getMode());
        return Map.of("enabled", chaos.isEnabled(),
                "latencyMs", chaos.getLatencyMs(), "mode", chaos.getMode());
    }
}
