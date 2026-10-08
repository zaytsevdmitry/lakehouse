/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.ui.modeller.service;

import org.lakehouse.ui.modeller.dto.SyncLogResponse;

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