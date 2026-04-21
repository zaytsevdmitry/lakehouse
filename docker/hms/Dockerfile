# --- Этап 1: Распаковка и очистка ---
FROM eclipse-temurin:17-jdk AS builder

RUN apt-get update && apt-get install -y wget tar

ENV HADOOP_VERSION=3.3.4 \
    METASTORE_VERSION=3.1.3 \
    LOG4J_WEB_VERSION=2.17.1 \
    HIVE_HOME=/opt/hive

WORKDIR /tmp

# Скачиваем и чистим Hadoop
RUN wget -nv  https://archive.apache.org/dist/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz
RUN mkdir -p /opt/hadoop && \
    tar -xzf hadoop-$HADOOP_VERSION.tar.gz -C /opt/hadoop --strip-components=1 && \
    # УДАЛЯЕМ ЛИШНЕЕ (экономим ~500-600МБ)
    rm -rf /opt/hadoop/share/doc \
           /opt/hadoop/share/hadoop/yarn \
           /opt/hadoop/share/hadoop/mapreduce \
           /opt/hadoop/share/hadoop/kms \
           /opt/hadoop/share/hadoop/httpfs \
           /opt/hadoop/bin/*.cmd \
           /opt/hadoop/sbin/*.cmd
RUN rm /tmp/hadoop-$HADOOP_VERSION.tar.gz
# Скачиваем и чистим Hive Metastore
RUN wget -nv https://repo1.maven.org/maven2/org/apache/hive/hive-standalone-metastore/${METASTORE_VERSION}/hive-standalone-metastore-${METASTORE_VERSION}-bin.tar.gz && \
    mkdir -p /opt/hive && \
    tar -xzf hive-standalone-metastore-$METASTORE_VERSION-bin.tar.gz -C /opt/hive --strip-components=1 && \
     rm -rf /opt/hive/hcatalog

RUN wget -nv -P ${HIVE_HOME}/lib/ https://repo1.maven.org/maven2/org/apache/hive/hive-exec/${METASTORE_VERSION}/hive-exec-${METASTORE_VERSION}.jar
RUN wget -nv -P ${HIVE_HOME}/lib/ https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-web/${LOG4J_WEB_VERSION}/log4j-web-${LOG4J_WEB_VERSION}.jar
# --- Этап 2: Финальный образ ---
FROM eclipse-temurin:17-jre

# Переменные окружения
ENV HADOOP_HOME=/opt/hadoop \
    HIVE_HOME=/opt/hive \
    JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/hadoop/bin:/opt/hive/bin:${PATH}"

WORKDIR /opt

# Копируем только очищенные файлы из builder
COPY --from=builder /opt/hadoop /opt/hadoop
COPY --from=builder /opt/hive /opt/hive
ENV HADOOP_HOME=/opt/hadoop \
    HADOOP_YARN_HOME=$HADOOP_HOME \
    HADOOP_MAPRED_HOME=$HADOOP_HOME \
    HIVE_HOME=/opt/hive \
    JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/hadoop/bin:/opt/hive/bin:${PATH}" \
    JAVA_OPTS="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED" \
    HADOOP_OPTS=$JAVA_OPTS \
    PSQL_CONN_VERSION=42.7.8 \
    DATABASE_TYPE=postgres

# Добавляем драйверы (Postgres и S3)
RUN apt-get update && apt-get install -y wget --no-install-recommends && \
    # Postgres JDBC
    wget -nv -P $HIVE_HOME/lib/ https://repo1.maven.org/maven2/org/postgresql/postgresql/${PSQL_CONN_VERSION}/postgresql-${PSQL_CONN_VERSION}.jar && \
    # AWS S3A (копируем из уже скачанного Hadoop Tools в lib метастора)
    cp $HADOOP_HOME/share/hadoop/tools/lib/hadoop-aws* $HIVE_HOME/lib/ && \
    cp $HADOOP_HOME/share/hadoop/tools/lib/aws-java-sdk-bundle* $HIVE_HOME/lib/ && \
    # РЕШЕНИЕ КОНФЛИКТА GUAVA (Hive 19.0 -> Hadoop 27.0)
    rm $HIVE_HOME/lib/guava-19.0.jar && \
    cp $HADOOP_HOME/share/hadoop/common/lib/guava-*.jar $HIVE_HOME/lib/ && \
    apt-get purge -y wget && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

# Создаем пользователя для безопасности
RUN userdel -r ubuntu  && \
    groupadd -r hive --gid=1000 && \
    useradd -r -g hive --uid=1000 -d /opt/hive hive && \
    chown -R hive:hive /opt/hadoop /opt/hive


ADD ./entrypoint.sh /
ENV ENTRYPOINT_FILE=entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT /entrypoint.sh

USER hive
EXPOSE 9083

