package org.lakehouse.modeller.service;

import org.lakehouse.modeller.dto.SyncLogResponse;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory ring buffer of review/sync events, exposed to admins at
 * {@code /v1_0/admin/sync-logs}. Capacity from {@code logging.sync-log-capacity}.
 */
public class SyncLogService {

    private final int capacity;
    private final Deque<SyncLogResponse> buffer = new ArrayDeque<>();

    public SyncLogService(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void log(String level, String user, String action, String message, String workspaceId) {
        SyncLogResponse entry = new SyncLogResponse(Instant.now(), level, user, action, message, workspaceId);
        buffer.addLast(entry);
        while (buffer.size() > capacity)
            buffer.removeFirst();
    }

    public synchronized List<SyncLogResponse> latest(int limit) {
        int take = Math.min(Math.max(limit, 1), capacity);
        List<SyncLogResponse> all = new ArrayList<>(buffer);
        int start = Math.max(0, all.size() - take);
        return new ArrayList<>(all.subList(start, all.size()));
    }

    public int capacity() {
        return capacity;
    }
}