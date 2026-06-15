# Cборка приложений и образов

## Требования к установке

- обеспечить наличие java 17
- Установить maven (mvn) и плагины
- Установить docker

Перед сборкой нужно убедится что установлены java 17 по умолчанию и mvn

> \[ERROR\] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on
> project lakehouse-common: Fatal error compiling: error: release version 17 not supported -> [Help 1]

```
@localhost:~/projects/my/lakehouse/docker> java -version
openjdk version "17.0.13" 2024-10-15
OpenJDK Runtime Environment Temurin-17.0.13+11 (build 17.0.13+11)
OpenJDK 64-Bit Server VM Temurin-17.0.13+11 (build 17.0.13+11, mixed mode, sharing)
```

Если это не так, то нужно добавить команды в терминал

```
export JAVA_HOME=[путь к папке с установленной java17]
export PATH=$JAVA_HOME/bin/:$PATH
```

Снова проверить версию
```commandline
 java -version
```
Ожидается что в выводе будет java 17

## Сборка образа

Файл  docker/build.bash запускает сборку проекта, образа и раскладывает артефакты по папкам
```commandline
cd docker
```
```commandline
sh build.bash
```