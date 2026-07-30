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
package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class SparkAdapterBase implements SparkAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final String masterUrl;
    private final long submissionTimeoutSeconds;

    protected SparkAdapterBase(String masterUrl, long submissionTimeoutSeconds) {
        this.masterUrl = masterUrl;
        this.submissionTimeoutSeconds = submissionTimeoutSeconds;
    }

    protected String defaultCreateSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        try {
            SparkLauncher launcher = buildSparkLauncher(request);
            log.info("Launching spark-submit via SparkLauncher");
            Process process = launcher.launch();

            StringBuffer combinedOutput = new StringBuffer();

            Thread stdoutReader = new Thread(() -> readStream(process.getInputStream(), combinedOutput, "stdout"));
            Thread stderrReader = new Thread(() -> readStream(process.getErrorStream(), combinedOutput, "stderr"));

            stdoutReader.start();
            stderrReader.start();

            boolean finished = process.waitFor(submissionTimeoutSeconds, TimeUnit.SECONDS);

            stdoutReader.join(5000);
            stderrReader.join(5000);

            String output = combinedOutput.toString();

            if (!finished) {
                process.destroyForcibly();
                log.error("spark-submit timed out after {}s. Output:\n{}", submissionTimeoutSeconds, output);
                throw new CreateErrorException(
                        "spark-submit timed out after " + submissionTimeoutSeconds + "s: " + output);
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                String submissionId = extractSubmissionId(output);
                log.info("spark-submit completed successfully. submissionId={}", submissionId);
                return submissionId;
            } else {
                log.error("spark-submit failed with exit code {}. Output:\n{}", exitCode, output);
                throw new CreateErrorException("spark-submit failed (exit " + exitCode + "): " + output);
            }

        } catch (CreateErrorException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CreateErrorException("Spark submission process was interrupted", e);
        } catch (Exception e) {
            throw new CreateErrorException("spark-submit error: " + e.getMessage(), e);
        }
    }

    protected abstract String extractSubmissionId(String output) throws CreateErrorException;

    private void readStream(java.io.InputStream is, StringBuffer sb, String label) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("Failed to read {}", label, e);
        }
    }

    protected SparkLauncher buildSparkLauncher(CreateSubmissionRequest request) {
        Map<String, String> env = new HashMap<>();
        if (request.environmentVariables() != null) {
            env.putAll(request.environmentVariables());
        }

        SparkLauncher launcher = new SparkLauncher(env);
        launcher.setMaster(masterUrl);
        launcher.setDeployMode("cluster");

        if (request.mainClass() != null) {
            launcher.setMainClass(request.mainClass());
        }

        if (request.sparkProperties() != null) {
            for (Map.Entry<String, String> entry : request.sparkProperties().entrySet()) {
                launcher.setConf(entry.getKey(), entry.getValue());
            }
        }

        if (request.appResource() != null) {
            launcher.setAppResource(request.appResource());
        }

        if (request.appArgs() != null && !request.appArgs().isEmpty()) {
            launcher.addAppArgs(request.appArgs().toArray(new String[0]));
        }

        return launcher;
    }

    @Override
    public SubmissionResponse clearCompleted(String submissionId) {
        return new SubmissionResponse("ClearResponse", null, null, submissionId, true);
    }
}
