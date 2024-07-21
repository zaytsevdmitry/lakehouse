env /bin/sh

docker run \
    --name devdb \
    -p 5432:5432 \
    -e POSTGRES_USER=postgresUser \
    -e POSTGRES_PASSWORD=postgresPW \
    -e POSTGRES_DB=postgresDB \
    -d postgres
    docker exec -it 425adfca8d67 /bin/bash
psql -h localhost -p 5432 -U postgresUser  -W postgresDB