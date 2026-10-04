FROM alpine:latest AS downloader
RUN apk add --no-cache wget ca-certificates
RUN wget --no-check-certificate -O /postgresql-42.7.3.jar \
    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar

FROM apache/hive:3.1.3

USER root

RUN rm -f /opt/hive/lib/postgresql-*.jar

COPY --from=downloader /postgresql-42.7.3.jar /opt/hive/lib/postgresql-42.7.3.jar
RUN chmod 644 /opt/hive/lib/postgresql-42.7.3.jar

ADD service.sh /

RUN chown hive:hive /service.sh && chmod +x /service.sh

USER hive
