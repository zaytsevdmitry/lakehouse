FROM eclipse-temurin:17
ADD ./opt /opt
RUN ls -lah /opt
RUN echo 'app:x:1000:' >> /etc/group && echo 'app:x:1000:100:app:/home/app:/bin/sh' >> /etc/passwd &&  mkdir -p /home/app && chown app:users /home/app
RUN chown -R app:users /opt
USER app

