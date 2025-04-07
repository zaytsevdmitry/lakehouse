# 
build.bash запускает сборку проекта и раскладывает артефакты по папкам
Перед сборкой нужно убедится что установлена java 17 по умолчанию
```
@localhost:~/projects/my/lakehouse/buld> java -version
openjdk version "17.0.13" 2024-10-15
OpenJDK Runtime Environment Temurin-17.0.13+11 (build 17.0.13+11)
OpenJDK 64-Bit Server VM Temurin-17.0.13+11 (build 17.0.13+11, mixed mode, sharing)
```
Если это не так, то нужно добавить команды в терминал

```
export JAVA_HOME=[путь к папке с установленной java17]
export PATH=$JAVA_HOME/bin/:$PATH
```
Снова проверить версию > java -version
ожидается что в выводе будет java 17

После успешной сборки можно собирать docker container и запускать демо docker-compose

