package com.myfave.api.global.chaos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 장애 주입 스위치. application-chaos.yml 의 chaos.pg.* 를 바인딩하고
// ChaosController 로 런타임에 값을 바꾼다. volatile 로 부하 스레드에 즉시 반영.
@Getter
@Setter
@Component
@Profile("chaos")
@ConfigurationProperties(prefix = "chaos.pg")
public class ChaosProperties {

    private volatile boolean enabled = false;
    private volatile long latencyMs = 8000;
    private volatile Mode mode = Mode.SLOW200;

    public enum Mode { SLOW200, ERROR }
}
