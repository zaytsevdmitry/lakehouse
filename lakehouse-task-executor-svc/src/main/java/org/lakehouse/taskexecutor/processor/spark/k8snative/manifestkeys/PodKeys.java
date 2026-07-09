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
package org.lakehouse.taskexecutor.processor.spark.k8snative.manifestkeys;

public class PodKeys {
    public static final String API_VERSION = "apiVersion";
    public static final String KIND = "kind";
    public static final String METADATA_NAME = "metadata.name";
    public static final String METADATA_NAMESPACE = "metadata.namespace";
    public static final String METADATA_LABELS = "metadata.labels.";
    public static final String METADATA_ANNOTATIONS = "metadata.annotations.";
    public static final String METADATA_ANNOTATIONS_TASK_NAME_KEY = "lakehouse-management-task";
    public static final String SPEC_RESTART_POLICY = "spec.restartPolicy";
    public static final String SPEC_SERVICE_ACCOUNT_NAME = "spec.serviceAccountName";
    public static final String SPEC_NODE_NAME = "spec.nodeName";
    public static final String SPEC_SUBDOMAIN = "spec.subdomain";
    public static final String SPEC_HOSTNAME = "spec.hostname";
    public static final String SPEC_SCHEDULER_NAME = "spec.schedulerName";
    public static final String SPEC_PRIORITY_CLASS_NAME = "spec.priorityClassName";
    public static final String SPEC_RUNTIME_CLASS_NAME = "spec.runtimeClassName";
    public static final String SPEC_TERMINATION_GRACE_PERIOD_SECONDS = "spec.terminationGracePeriodSeconds";
    public static final String SPEC_ACTIVE_DEADLINE_SECONDS = "spec.activeDeadlineSeconds";
    public static final String SPEC_PRIORITY = "spec.priority";
    public static final String SPEC_HOST_NETWORK = "spec.hostNetwork";
    public static final String SPEC_HOST_PID = "spec.hostPID";
    public static final String SPEC_HOST_IPC = "spec.hostIPC";
    public static final String SPEC_SHARE_PROCESS_NAMESPACE = "spec.shareProcessNamespace";
    public static final String SPEC_AUTOMOUNT_SERVICE_ACCOUNT_TOKEN = "spec.automountServiceAccountToken";
    public static final String SPEC_NODE_SELECTOR = "spec.nodeSelector.";
    public static final String SPEC_IMAGE_PULL_SECRETS = "spec.imagePullSecrets.";
    public static final String SPEC_TOLERATIONS_KEY = "spec.tolerations.%d.key";
    public static final String SPEC_TOLERATIONS_OPERATOR = "spec.tolerations.%d.operator";
    public static final String SPEC_TOLERATIONS_VALUE = "spec.tolerations.%d.value";
    public static final String SPEC_TOLERATIONS_EFFECT = "spec.tolerations.%d.effect";
    public static final String SPEC_HOST_ALIASES = "spec.hostAliases.";

}
