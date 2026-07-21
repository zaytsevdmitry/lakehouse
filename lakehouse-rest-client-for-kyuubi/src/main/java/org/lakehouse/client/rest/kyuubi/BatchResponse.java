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
package org.lakehouse.client.rest.kyuubi;

import java.util.Map;

/**
 * POJO mapping the official Apache Kyuubi REST API Swagger specification for Batch sessions.
 */
public class BatchResponse {

    private String id;
    private String user;
    private String batchType;
    private String name;
    private long appStartTime;
    private String appId;
    private String appUrl;
    private String appState;
    private String appDiagnostic;
    private String kyuubiInstance;
    private String state;
    private long createTime;
    private long endTime;
    private Map<String, String> batchInfo;

    // Default constructor
    public BatchResponse() {
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAppStartTime() {
        return appStartTime;
    }

    public void setAppStartTime(long appStartTime) {
        this.appStartTime = appStartTime;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public String getAppState() {
        return appState;
    }

    public void setAppState(String appState) {
        this.appState = appState;
    }

    public String getAppDiagnostic() {
        return appDiagnostic;
    }

    public void setAppDiagnostic(String appDiagnostic) {
        this.appDiagnostic = appDiagnostic;
    }

    public String getKyuubiInstance() {
        return kyuubiInstance;
    }

    public void setKyuubiInstance(String kyuubiInstance) {
        this.kyuubiInstance = kyuubiInstance;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Map<String, String> getBatchInfo() {
        return batchInfo;
    }

    public void setBatchInfo(Map<String, String> batchInfo) {
        this.batchInfo = batchInfo;
    }

    @Override
    public String toString() {
        return "BatchResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", appState='" + appState + '\'' +
                ", appId='" + appId + '\'' +
                '}';
    }
}
