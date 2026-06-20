FROM alpine:3.19

# Устанавливаем curl для скачивания mc и ca-certificates для работы по HTTPS
RUN apk add --no-cache curl ca-certificates && \
    curl -s -O https://dl.min.io/aistor/mc/release/linux-amd64/mc && \
    chmod +x mc && \
    mv mc /usr/local/bin/mc

# Скрипт проверки и инициализации хранилища
COPY init-storage.sh /usr/local/bin/init-storage.sh
RUN chmod +x /usr/local/bin/init-storage.sh

ENTRYPOINT ["/usr/local/bin/init-storage.sh"]
