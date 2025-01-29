from maven:3.8.1-openjdk-17-slim


ADD . lakehouse
WORKDIR lakehouse
RUN mvn compile
RUN ls /root
RUN mvn package -Dmaven.test.skip
