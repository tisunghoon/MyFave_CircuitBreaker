package com.myfave.api.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "trace_id";

    // k6 부하테스트 라운드/시나리오 라벨 — Loki에서 Round별 로그 분리용.
    // k6 lib/context.js에서 모든 요청에 자동 부착됨.
    private static final String LOADTEST_RUN_ID_HEADER = "X-Loadtest-Run-Id";
    private static final String LOADTEST_SCENARIO_HEADER = "X-Loadtest-Scenario";
    private static final String LOADTEST_ROUND_HEADER = "X-Loadtest-Round";
    private static final String MDC_LOADTEST_RUN_ID = "loadtest_run_id";
    private static final String MDC_LOADTEST_SCENARIO = "loadtest_scenario";
    private static final String MDC_LOADTEST_ROUND = "loadtest_round";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        // 부하테스트 헤더가 있을 때만 MDC에 주입 — 운영 트래픽에는 무영향.
        putIfPresent(request, LOADTEST_RUN_ID_HEADER, MDC_LOADTEST_RUN_ID);
        putIfPresent(request, LOADTEST_SCENARIO_HEADER, MDC_LOADTEST_SCENARIO);
        putIfPresent(request, LOADTEST_ROUND_HEADER, MDC_LOADTEST_ROUND);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove(MDC_LOADTEST_RUN_ID);
            MDC.remove(MDC_LOADTEST_SCENARIO);
            MDC.remove(MDC_LOADTEST_ROUND);
        }
    }

    private void putIfPresent(HttpServletRequest request, String headerName, String mdcKey) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            MDC.put(mdcKey, value);
        }
    }
}
