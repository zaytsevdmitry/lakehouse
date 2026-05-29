# Подготовка
- Установить minikube 
- обновить или просто скачать kubectl v1.36.0
> curl -LO https://dl.k8s.io/release/v1.36.0/bin/linux/amd64/kubectl

Пример приведен для linux. [Тут](https://kubernetes.io/docs/tasks/tools/) можно подобрать под свою операционную систему.
Файл нужно расположить "поближе" в переменной окружения PATH. На пример $HOME/bin
>mv kubectl $HOME/bin

- установить helm
  - для удобства можно добавить helm-bash-completion или другой удобный
Запустить minikube
> minikube start --cpus 4 --memory 8192 --registry-mirror=https://dh-mirror.gitverse.ru

Сборка тестировалась с применением ближайшего registry-mirror. Можно указать любой либо убрать и использовать настройку по умолчанию.

Нужно обеспечить наличие docker images 
Для сборки проектов и упаковки в образы нужно перейти в корневой каталог проект lakehouse, найти в нем папку docker, перейти в нее и запустить файл build.bash
> sh build.bash


