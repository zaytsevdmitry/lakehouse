package org.lakehouse.config.entities.datasource;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
public class DataSourceService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "data_source_service__data_source_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSource dataSource;

    private String host;
    private String port;
    private String urn;


    public DataSourceService() {
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceService that = (DataSourceService) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getDataSource(), that.getDataSource()) && Objects.equals(getHost(), that.getHost()) && Objects.equals(getPort(), that.getPort()) && Objects.equals(getUrn(), that.getUrn());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDataSource(), getHost(), getPort(), getUrn());
    }
}
