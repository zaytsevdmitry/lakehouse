package org.lakehouse.client.api.dto.configs.dq;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetSourceDTO;

import java.util.*;

public class QualityMetricsConfDTO {
    private String dataSetKeyName;
    private String keyName;
    private String description;
    private boolean enabled;
    private Types.DQThresholdViolationLevel dqThresholdViolationLevel;
    private Map<String, DataSetSourceDTO> sources = new HashMap<>();
    private Map<String, QualityMetricsConfTestSetDTO> testSets = new HashMap<>();
    private Map<String, QualityMetricsConfTestSetDTO> thresholds = new HashMap<>();


    public QualityMetricsConfDTO() {
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, DataSetSourceDTO> getSources() {
        return sources;
    }

    public void setSources(Map<String, DataSetSourceDTO> sources) {
        this.sources = sources;
    }

    public Map<String, QualityMetricsConfTestSetDTO> getTestSets() {
        return testSets;
    }

    public void setTestSets(Map<String, QualityMetricsConfTestSetDTO> testSets) {
        this.testSets = testSets;
    }

    public Types.DQThresholdViolationLevel getDqThresholdViolationLevel() {
        return dqThresholdViolationLevel;
    }

    public void setDqThresholdViolationLevel(Types.DQThresholdViolationLevel dqThresholdViolationLevel) {
        this.dqThresholdViolationLevel = dqThresholdViolationLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfDTO that = (QualityMetricsConfDTO) o;
        return isEnabled() == that.isEnabled() && Objects.equals(getDataSetKeyName(), that.getDataSetKeyName()) && Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getTestSets(), that.getTestSets()) && Objects.equals(getThresholds(), that.getThresholds()) && getDqThresholdViolationLevel() == that.getDqThresholdViolationLevel();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getKeyName(), getDescription(), isEnabled(), getSources(), getTestSets(), getThresholds(), getDqThresholdViolationLevel());
    }

    @Override
    public String toString() {
        return "QualityMetricsConfDTO{" +
                "dataSetKeyName='" + dataSetKeyName + '\'' +
                ", keyName='" + keyName + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", sources=" + sources +
                ", qualityMetricsConfTestSets=" + testSets +
                ", thresholds=" + thresholds +
                ", dqThresholdViolationLevel=" + dqThresholdViolationLevel +
                '}';
    }

    public Map<String, QualityMetricsConfTestSetDTO> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, QualityMetricsConfTestSetDTO> thresholds) {
        this.thresholds = thresholds;
    }
}

/*qualities:
  quality1:
    name: example_ds_example_dq
    dataset: example
    sources:
      - stage
      - target
    test-sets:
    thresholds:
      - name: ${dataset}_pk
        type: jdbc_primary_constraint
  quality:
    name: example_ds_example_dq
    dataset: example
    sources:
      - example4
    test-sets:
      - name: mysqltestset
        push down:
          query: true
          filter: true
        query: ""
        save: true
        type: jdbc
    thresholds:
      - name: ${dataset}_logic_test
        type: jdbc
        query: ""*/