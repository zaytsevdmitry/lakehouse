package org.lakehouse.client.api.dto.configs;

import java.util.*;

public class QualityMetricsConfDTO {
    private String dataSetKeyName;
    private String keyName;
    private String description;
    private boolean enabled;
    private Set<DataSetSourceDTO> sources = new HashSet<>();
    private Set<QualityMetricsConfTestSetDTO> qualityMetricsConfTestSets = new HashSet<>();
    private Set<QualityMetricsConfTestSetDTO> thresholds = new HashSet<>();

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

    public Set<DataSetSourceDTO> getSources() {
        return sources;
    }

    public void setSources(Set<DataSetSourceDTO> sources) {
        this.sources = sources;
    }

    public Set<QualityMetricsConfTestSetDTO> getQualityMetricsConfTestSets() {
        return qualityMetricsConfTestSets;
    }

    public void setQualityMetricsConfTestSets(Set<QualityMetricsConfTestSetDTO> qualityMetricsConfTestSets) {
        this.qualityMetricsConfTestSets = qualityMetricsConfTestSets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfDTO that = (QualityMetricsConfDTO) o;
        return isEnabled() == that.isEnabled() && Objects.equals(getDataSetKeyName(), that.getDataSetKeyName()) && Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getQualityMetricsConfTestSets(), that.getQualityMetricsConfTestSets()) && Objects.equals(getThresholds(), that.getThresholds());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataSetKeyName(), getKeyName(), getDescription(), isEnabled(), getSources(), getQualityMetricsConfTestSets(), getThresholds());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"{" +
                "dataSetKeyName='" + dataSetKeyName + '\'' +
                ", keyName='" + keyName + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", sources=" + sources +
                ", qualityMetricsConfTestSets=" + qualityMetricsConfTestSets +
                ", thresholds=" + thresholds +
                '}';
    }

    public Set<QualityMetricsConfTestSetDTO> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Set<QualityMetricsConfTestSetDTO> thresholds) {
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