FROM eclipse-temurin:17
#from maven:3.8.1-openjdk-17-slim

RUN pwd
RUN ls -lah
ADD ./distr/opt /opt
#RUN ls -lah lakehouse/distr
#RUN cp -r /lakehouse/distr/opt /opt/
RUN ls -lah /opt
#RUN useradd -ms /bin/bash --home /lakehouse builder
#USER builder
#ENV HOME /lakehouse
#WORKDIR $HOME
#RUN ls -lah .
#RUN mvn -o package  -Dmaven.test.skip
