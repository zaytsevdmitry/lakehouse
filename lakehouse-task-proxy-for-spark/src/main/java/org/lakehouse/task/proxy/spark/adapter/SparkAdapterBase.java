package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public abstract class SparkAdapterBase implements SparkAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final String masterUrl;

    protected SparkAdapterBase(String masterUrl) {
        this.masterUrl = masterUrl;
    }

    protected String defaultCreateSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        try {
            SparkLauncher launcher = buildSparkLauncher(request);
            log.info("Launching spark-submit via SparkLauncher");
            Process process = launcher.launch();

            // Using StringBuffer because it is thread-safe for concurrent appends
            StringBuffer combinedOutput = new StringBuffer();

            // Reading stdout and stderr concurrently to prevent OS buffer blocking
            Thread stdoutReader = new Thread(() -> readStream(process.getInputStream(), combinedOutput, "stdout"));
            Thread stderrReader = new Thread(() -> readStream(process.getErrorStream(), combinedOutput, "stderr"));

            stdoutReader.start();
            stderrReader.start();

            // Wait for the process to exit
            int exitCode = process.waitFor();

            // Wait for the threads to finish consuming any remaining lines in the streams
            stdoutReader.join(10000);
            stderrReader.join(10000);

            String output = combinedOutput.toString();

            if (exitCode == 0) {
                String submissionId = extractSubmissionId(output);
                log.info("spark-submit completed successfully. submissionId={}", submissionId);
                return submissionId;
            } else {
                log.error("spark-submit failed with exit code {}. Output:\n{}", exitCode, output);
                throw new CreateErrorException("spark-submit failed (exit " + exitCode + "): " + output);
            }

        } catch (CreateErrorException e) {
            // Preserving our domain-specific exception
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CreateErrorException("Spark submission process was interrupted", e);
        } catch (Exception e) {
            // Wrapping generic standard/system exceptions into our domain exception
            throw new CreateErrorException("spark-submit error: " + e.getMessage(), e);
        }
    }

    private void readStream(java.io.InputStream is, StringBuffer sb, String label) {
        // try-with-resources guarantees the stream and reader are closed properly
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("Failed to read {}", label, e);
        }
    }


    protected abstract String extractSubmissionId(String output) throws CreateErrorException;

    protected SparkLauncher buildSparkLauncher(CreateSubmissionRequest request) {
        Map<String, String> env = new HashMap<>();
        if (request.environmentVariables() != null) {
            env.putAll(request.environmentVariables());
        }

        SparkLauncher launcher = new SparkLauncher(env);
        launcher.setMaster(masterUrl);
        launcher.setDeployMode("cluster");

        if (request.sparkProperties() != null) {
            for (Map.Entry<String, String> entry : request.sparkProperties().entrySet()) {
                launcher.setConf(entry.getKey(), entry.getValue());
            }
        }

        if (request.mainClass() != null) {
            launcher.setMainClass(request.mainClass());
        }

        if (request.appResource() != null) {
            launcher.setAppResource(request.appResource());
        }

        if (request.appArgs() != null && !request.appArgs().isEmpty()) {
            launcher.addAppArgs(request.appArgs().toArray(new String[0]));
        }

        return launcher;
    }

}
