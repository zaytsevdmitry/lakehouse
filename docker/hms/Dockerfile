FROM apache/hive:3.1.3

USER root

RUN rm -f /opt/hive/lib/postgresql-*.jar

RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN wget --no-check-certificate -O  /opt/hive/lib/postgresql-42.7.3.jar \
    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar && \
    chmod 644 /opt/hive/lib/postgresql-42.7.3.jar

ADD service.sh /

RUN chown hive:hive /service.sh && chmod +x /service.sh

USER hive
