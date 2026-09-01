FROM lakehouse-spark-aws:0.10.0
USER root
RUN curl -Z -L -O https://download.oracle.com/java/23/archive/jdk-23.0.2_linux-x64_bin.tar.gz && \
    tar xzvf jdk-23.0.2_linux-x64_bin.tar.gz -C /opt/java/ && \
    rm jdk-23.0.2_linux-x64_bin.tar.gz
USER spark
ENV APP_HOME=/opt/lakehouse-task-spark-apps

# Add application
ADD lakehouse-task-proxy-for-spark-0.10.0-jar-with-dependencies.jar ${APP_HOME}/

EXPOSE 8500 8500

