FROM lakehouse-spark-aws:0.5.0

USER root

ENV KYUUBI_VERSION=1.11.1
ENV KYUUBI_HOME=/opt/kyuubi
RUN wget https://mirror.yandex.ru/mirrors/apache/kyuubi/kyuubi-${KYUUBI_VERSION}/apache-kyuubi-${KYUUBI_VERSION}-bin.tgz && \
    tar -xf apache-kyuubi-${KYUUBI_VERSION}-bin.tgz -C /opt && \
    rm apache-kyuubi-${KYUUBI_VERSION}-bin.tgz && \
    mv /opt/apache-kyuubi-${KYUUBI_VERSION}-bin ${KYUUBI_HOME} && \
    chown -R spark:spark ${KYUUBI_HOME}

USER spark

WORKDIR ${KYUUBI_HOME}

EXPOSE 10099 10009

ENTRYPOINT ["/opt/kyuubi/bin/kyuubi"]
CMD ["run"]


