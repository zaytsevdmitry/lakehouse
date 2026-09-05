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

package org.lakehouse.config.vcs.service;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.DriverDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.config.vcs.entity.VcsObjectLog;
import org.lakehouse.config.vcs.entity.VcsSyncLog;
import org.lakehouse.config.vcs.entity.VcsSyncStatus;
import org.lakehouse.config.vcs.repository.VcsObjectLogRepository;
import org.lakehouse.config.vcs.repository.VcsSyncLogRepository;
import org.lakehouse.config.vcs.yaml.ConfigKind;
import org.lakehouse.config.vcs.yaml.GitOpsYamlParser;
import org.lakehouse.config.vcs.yaml.ParsedConfig;
import org.lakehouse.config.vcs.yaml.ScriptContent;
import org.lakehouse.config.service.NameSpaceService;
import org.lakehouse.config.service.ScenarioActTemplateService;
import org.lakehouse.config.service.ScriptService;
import org.lakehouse.config.service.TaskExecutionServiceGroupService;
import org.lakehouse.config.service.TaskService;
import org.lakehouse.config.service.dataset.DataSetService;
import org.lakehouse.config.service.datasource.DataSourceService;
import org.lakehouse.config.service.datasource.DriverService;
import org.lakehouse.config.service.dq.QualityMetricsConfService;
import org.lakehouse.config.service.ScheduleService;
import org.lakehouse.validator.config.ScenarioActTemplateConfValidator;
import org.lakehouse.validator.config.ScheduleConfValidator;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.exception.DTOValidationException;
import org.lakehouse.validator.task.TaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies a {@link GitSyncChangeSet} to the configuration database.
 * <p>
 * All constructs of a commit are written inside one transaction: created and updated
 * constructs first (in dependency order), deleted constructs last (in reverse dependency
 * order), and only then the SUCCESS marker is stored. Any failure rolls the whole commit
 * back so the database always reflects either the full commit or none of it.
 */
@Service
public class GitOpsSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GitOpsYamlParser yamlParser;
    private final VcsSyncLogRepository vcsSyncLogRepository;
    private final VcsObjectLogRepository vcsObjectLogRepository;
    private final NameSpaceService nameSpaceService;
    private final DriverService driverService;
    private final DataSourceService dataSourceService;
    private final ScriptService scriptService;
    private final TaskExecutionServiceGroupService taskExecutionServiceGroupService;
    private final TaskService taskService;
    private final DataSetService dataSetService;
    private final ScenarioActTemplateService scenarioActTemplateService;
    private final QualityMetricsConfService qualityMetricsConfService;
    private final ScheduleService scheduleService;

    public GitOpsSynchronizer(
            GitOpsYamlParser yamlParser,
            VcsSyncLogRepository vcsSyncLogRepository,
            VcsObjectLogRepository vcsObjectLogRepository,
            NameSpaceService nameSpaceService,
            DriverService driverService,
            DataSourceService dataSourceService,
            ScriptService scriptService,
            TaskExecutionServiceGroupService taskExecutionServiceGroupService,
            TaskService taskService,
            DataSetService dataSetService,
            ScenarioActTemplateService scenarioActTemplateService,
            QualityMetricsConfService qualityMetricsConfService,
            ScheduleService scheduleService) {
        this.yamlParser = yamlParser;
        this.vcsSyncLogRepository = vcsSyncLogRepository;
        this.vcsObjectLogRepository = vcsObjectLogRepository;
        this.nameSpaceService = nameSpaceService;
        this.driverService = driverService;
        this.dataSourceService = dataSourceService;
        this.scriptService = scriptService;
        this.taskExecutionServiceGroupService = taskExecutionServiceGroupService;
        this.taskService = taskService;
        this.dataSetService = dataSetService;
        this.scenarioActTemplateService = scenarioActTemplateService;
        this.qualityMetricsConfService = qualityMetricsConfService;
        this.scheduleService = scheduleService;
    }

    /**
     * Validates, applies and records a configuration commit atomically.
     *
     * @throws RuntimeException when the change set is invalid; the whole change set
     *                          is then rolled back and no log row is written
     */
    @Transactional(rollbackFor = Exception.class)
    public void sync(GitSyncChangeSet changeSet, String commitId) {
        if (changeSet.isEmpty()) {
            logger.info("Commit {} contains no configuration changes", commitId);
            markSuccess(commitId);
            return;
        }
        validateAll(changeSet);
        applyAll(changeSet.toApply());
        unmanageAll(changeSet.toDelete());
        recordObjectLogs(changeSet, commitId);
        markSuccess(commitId);
        logger.info("Configuration commit {} applied successfully", commitId);
    }

    private void markSuccess(String commitId) {
        vcsSyncLogRepository.save(new VcsSyncLog(commitId, OffsetDateTime.now(), VcsSyncStatus.SUCCESS, null));
    }

    private void recordObjectLogs(GitSyncChangeSet changeSet, String commitId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<GitSyncItem> items = new ArrayList<>(changeSet.toApply());
        items.addAll(changeSet.toDelete());
        for (GitSyncItem item : items) {
            vcsObjectLogRepository.save(
                    new VcsObjectLog(
                            now,
                            yamlParser.resolveKey(item.parsedConfig()),
                            item.parsedConfig().kind().name(),
                            item.path(),
                            commitId));
        }
    }

    private void validateAll(GitSyncChangeSet changeSet) {
        for (GitSyncItem item : changeSet.toApply())
            validate(item.parsedConfig());
    }

    private void validate(ParsedConfig parsedConfig) {
        switch (parsedConfig.kind()) {
            case SCHEDULE -> {
                ValidationResult result = ScheduleConfValidator.validate((ScheduleDTO) parsedConfig.dto());
                if (!result.isValid())
                    throw new DTOValidationException(result.getDescriptions());
            }
            case SCENARIO_ACT_TEMPLATE -> {
                ValidationResult result = ScenarioActTemplateConfValidator.validate((ScenarioActTemplateDTO) parsedConfig.dto());
                if (!result.isValid())
                    throw new DTOValidationException(result.getDescriptions());
            }
            case TASK -> {
                ValidationResult result = TaskDTOValidator.validate((TaskDTO) parsedConfig.dto());
                if (!result.isValid())
                    throw new DTOValidationException(result.getDescriptions());
            }
            default -> {
                //no dedicated validator; handled by the corresponding service
            }
        }
    }

    private void applyAll(List<GitSyncItem> toApply) {
        // all constructs are applied in the kind dependency order; datasets are kept at
        // their own position (order 7) but sorted among themselves by their sources so
        // they are created before scenarios, metrics and schedules reference them
        List<GitSyncItem> others = toApply.stream()
                .filter(item -> item.parsedConfig().kind() != ConfigKind.DATA_SET)
                .sorted((a, b) -> {
                    int order = Integer.compare(a.parsedConfig().kind().order(), b.parsedConfig().kind().order());
                    return order != 0 ? order : a.path().compareTo(b.path());
                })
                .toList();

        List<GitSyncItem> dataSets = new ArrayList<>();
        for (GitSyncItem item : toApply) {
            if (item.parsedConfig().kind() == ConfigKind.DATA_SET)
                dataSets.add(item);
        }
        List<GitSyncItem> dataSetsOrdered = orderDataSetsDependencyWise(dataSets);

        List<GitSyncItem> ordered = new ArrayList<>();
        boolean dataSetsInserted = false;
        for (GitSyncItem item : others) {
            if (!dataSetsInserted && item.parsedConfig().kind().order() > ConfigKind.DATA_SET.order()) {
                ordered.addAll(dataSetsOrdered);
                dataSetsInserted = true;
            }
            ordered.add(item);
        }
        if (!dataSetsInserted)
            ordered.addAll(dataSetsOrdered);

        for (GitSyncItem item : ordered)
            apply(item.parsedConfig());
    }

    private void unmanageAll(List<GitSyncItem> toDelete) {
        List<GitSyncItem> ordered = new ArrayList<>(toDelete);
        ordered.sort((a, b) -> {
            int order = Integer.compare(b.parsedConfig().kind().order(), a.parsedConfig().kind().order());
            return order != 0 ? order : a.path().compareTo(b.path());
        });
        for (GitSyncItem item : ordered)
            unmanage(item.parsedConfig());
    }

    private List<GitSyncItem> orderDataSetsDependencyWise(List<GitSyncItem> dataSets) {
        if (dataSets.size() < 2)
            return dataSets;
        Map<String, GitSyncItem> byKey = new LinkedHashMap<>();
        for (GitSyncItem item : dataSets)
            byKey.put(yamlParser.resolveKey(item.parsedConfig()), item);

        Map<String, GitSyncItem> index = new HashMap<>(byKey.size());
        Map<GitSyncItem, Set<GitSyncItem>> dependents = new HashMap<>();
        Map<GitSyncItem, Integer> dependencyCount = new HashMap<>();
        for (GitSyncItem item : dataSets) {
            index.put(yamlParser.resolveKey(item.parsedConfig()), item);
            dependents.put(item, new HashSet<>());
            dependencyCount.put(item, 0);
        }
        for (GitSyncItem item : dataSets) {
            Set<String> sources = ((DataSetDTO) item.parsedConfig().dto()).getSources().keySet();
            for (String sourceKey : sources) {
                GitSyncItem dependency = index.get(sourceKey);
                if (dependency == null)
                    continue;
                dependents.get(dependency).add(item);
                dependencyCount.put(item, dependencyCount.get(item) + 1);
            }
        }

        Deque<GitSyncItem> ready = new ArrayDeque<>();
        List<GitSyncItem> result = new ArrayList<>();
        dataSets.forEach(item -> {
            if (dependencyCount.get(item) == 0)
                ready.add(item);
        });
        while (!ready.isEmpty()) {
            GitSyncItem item = ready.poll();
            result.add(item);
            for (GitSyncItem dependent : dependents.get(item)) {
                dependencyCount.put(dependent, dependencyCount.get(dependent) - 1);
                if (dependencyCount.get(dependent) == 0)
                    ready.add(dependent);
            }
        }
        //cyclic references are not resolvable; fall back to the declared order
        for (GitSyncItem item : dataSets) {
            if (!result.contains(item))
                result.add(item);
        }
        return result;
    }

    private void apply(ParsedConfig parsedConfig) {
        switch (parsedConfig.kind()) {
            case NAME_SPACE -> nameSpaceService.saveVcs((NameSpaceDTO) parsedConfig.dto());
            case DRIVER -> driverService.saveVcs((DriverDTO) parsedConfig.dto());
            case DATA_SOURCE -> dataSourceService.saveVcs((DataSourceDTO) parsedConfig.dto());
            case SCRIPT -> {
                ScriptContent script = (ScriptContent) parsedConfig.dto();
                scriptService.saveVcs(script.key(), script.value());
            }
            case TASK_EXECUTION_SERVICE_GROUP -> taskExecutionServiceGroupService.saveVcs((TaskExecutionServiceGroupDTO) parsedConfig.dto());
            case TASK -> taskService.saveVcs((TaskDTO) parsedConfig.dto(), null, null);
            case DATA_SET -> dataSetService.saveVcs((DataSetDTO) parsedConfig.dto());
            case SCENARIO_ACT_TEMPLATE -> scenarioActTemplateService.saveVcs((ScenarioActTemplateDTO) parsedConfig.dto());
            case QUALITY_METRICS_CONF -> qualityMetricsConfService.saveVcs((QualityMetricsConfDTO) parsedConfig.dto());
            case SCHEDULE -> scheduleService.saveVcs((ScheduleDTO) parsedConfig.dto());
        }
    }

    private void unmanage(ParsedConfig parsedConfig) {
        String key = yamlParser.resolveKey(parsedConfig);
        switch (parsedConfig.kind()) {
            case NAME_SPACE -> nameSpaceService.unmanage(key);
            case DRIVER -> driverService.unmanage(key);
            case DATA_SOURCE -> dataSourceService.unmanage(key);
            case SCRIPT -> scriptService.unmanage(key);
            case TASK_EXECUTION_SERVICE_GROUP -> taskExecutionServiceGroupService.unmanage(key);
            case TASK -> taskService.unmanageByName(key, null, null);
            case DATA_SET -> dataSetService.unmanage(key);
            case SCENARIO_ACT_TEMPLATE -> scenarioActTemplateService.unmanage(key);
            case QUALITY_METRICS_CONF -> qualityMetricsConfService.unmanage(key);
            case SCHEDULE -> scheduleService.unmanage(key);
        }
    }
}