# Сборка приложений и образов

## Требования к установке

- обеспечить наличие java 23
- Установить maven (mvn) 3.9.9 и плагины
- Установить docker

Перед сборкой нужно убедится что установлены java 23 по умолчанию и mvn

> \[ERROR\]  Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on
> project lakehouse-common: Fatal error compiling: error: release version 17 not supported -> [Help 1]

```shell
 java -version
```
```
openjdk version "23.0.1" 2024-10-15
OpenJDK Runtime Environment (build 23.0.1+11-39)
OpenJDK 64-Bit Server VM (build 23.0.1+11-39, mixed mode, sharing)
```

Если версия java ниже 23, то нужно добавить команды в терминал

```
export JAVA_HOME=[путь к папке с установленной java17]
export PATH=$JAVA_HOME/bin/:$PATH
```

Снова проверить версию
```shell
java -version
```
Ожидается, что в выводе будет java 23 или выше

Также можно проверить наличие mvn

```shell
mvn -version
```
```
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /home/share/gigaide/plugins/maven/lib/maven3
Java version: 23.0.1, vendor: Oracle Corporation, runtime: /home/share/.jdks/openjdk-23.0.1
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.4.0-150600.23.103-default", arch: "amd64", family: "unix"

Process finished with exit code 0

```
Ожидается, что в выводе будет java 23 или выше, maven 3.9.9 и любая операционная система с поддержкой java.

> Еще один момент на который следует обратить внимание - это используемый по умолчанию docker mirror.
> Он должен быть хорошо доступен в вашем регионе. Тут описание как настроить https://docs.docker.com/docker-hub/image-library/mirror/#configure-the-docker-daemon
## Сборка образа

Файл docker/build.bash запускает сборку проекта, образа и раскладывает артефакты по папкам

```shell
cd docker
```
```shell
sh build.bash
```

Сборку можно считать успешной если все 4 образа можно найти в локальном репозитории

```shell
docker images | grep lakehouse | grep '0.11.0'
```
```
lakehouse-s3-check                       0.11.0                           70a21c6fc79c   3 minutes ago    119MB
lakehouse-hms                            0.11.0                           f81700fd2891   3 minutes ago    893MB
lakehouse-spark-aws                      0.11.0                           42e9f3a122aa   4 minutes ago    1.54GB
lakehouse                                0.11.0                           c8f6f2da61dc   5 minutes ago    1.36GB
```