package com.myfave.api.global.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {

    // roomId → Set<sessionId>
    private final Map<Long, Set<String>> roomSessions = new ConcurrentHashMap<>();

    public void join(Long roomId, String sessionId) {
        roomSessions.computeIfAbsent(roomId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(sessionId);
    }

    public void leave(Long roomId, String sessionId) {
        Set<String> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
    }

    public int getParticipantCount(Long roomId) {
        Set<String> sessions = roomSessions.get(roomId);
        return sessions == null ? 0 : sessions.size();
    }

    public int totalActiveSessions() {
        return roomSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}
