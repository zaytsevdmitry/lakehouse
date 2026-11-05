# Безопасность (RU)

## Модель

Интеграция использует ту же модель безопасности service-to-service, что и
остальная экосистема yarina:

| Элемент | Значение |
|:--------|:---------|
| Identity provider | Keycloak, realm **`yarina`** |
| Клиент | **`yarina-internal-client`** (confidential, включены service accounts) |
| Grant | OAuth2 **`client_credentials`** |
| Токен | JWT access token в заголовке `Authorization: Bearer <token>` |
| Транспорт | HTTP(S) от Airflow worker к `yarina-state-svc` / `yarina-config-svc` / `yarina-task-proxy-for-spark` |

Hook'и получают токен по `extra.auth_url`, кэшируют его на время жизни экземпляра
hook'а (один вызов `execute` оператора) и передают целевому сервису. Между задачами
создаётся новый hook, поэтому запрашивается новый токен - токен не переиспользуется
между задачами и не пишется в XCom.

Ссылка: [Безопасность в репозитории yarina](../../doc/security/security.md).

## Роли не требуются

**Роли realm не требуются.** `yarina-state-svc`, `yarina-config-svc` и
`yarina-task-proxy-for-spark` проверяют только то, что JWT подписан realm `yarina` и
выдан `yarina-internal-client`; для используемых модулем эндпоинтов достаточно
валидного JWT.

## Аудит

`AuditLoggingFilter` бэкенда видит все вызовы Airflow как внутренний технический
клиент. В audit-логе они атрибутируются имени service account **`system`** -
точно так же, как вызовы из `yarina-task-executor-svc` и других backend-сервисов.
Реальные пользователи в этих audit-записях не представлены.

## Хранение и ротация секретов

* Client secret хранится в Connection Airflow (`password`) или во внешнем secret
  backend, на который ссылается Connection.
* В развёртываниях, где секрет передаётся через окружение, используйте
  `KEYCLOAK_INTERNAL_CLIENT_SECRET`; ротация значения в Keycloak и secret store
  **не требует изменения DAG**.
* Токен и пароль никогда не логируются. Hook'и пишут только client id, датасет,
  интервал, статус и lockSource.
* Ограничьте доступ к метаданным Airflow и включите Fernet-шифрование (или
  secret backend) для паролей Connection.

## Локальная разработка

`extra.security_enabled=false` отключает OAuth2 и отправляет запросы без заголовка
`Authorization`. Это предназначено **только для локальной разработки** с сервисами,
запущенными с `yarina.security.enabled=false`. В production обязателен Keycloak.

## Spark-прокси: отсутствие kill-эндпоинтов

`YarinaSparkProxyHook` предоставляет только `create_submission` и `get_status`. Он
не реализует эндпоинты прокси `/kill`, `/killall` и `/clear`, поэтому задача
Airflow не может остановить workload кластера через эту интеграцию.

Если `timeout_seconds` истёк, задача Airflow падает, но Spark-приложение может
продолжать выполняться. Используйте `submissionId` из исключения, чтобы проверить
или остановить его вручную вне Airflow. Ставьте `timeout_seconds` с запасом и не
рассчитывайте, что retry Airflow отменит предыдущую попытку: retry создаёт новую
submission в прокси, а прокси не выполняет дедупликацию.

## Операционные рекомендации

1. Используйте HTTPS между Airflow и Keycloak/yarina-сервисами (или доверенный
   service mesh) в production.
2. Выдавайте `yarina-internal-client` только необходимые права; не переиспользуйте
   секрет UI-клиента.
3. Храните секрет Connection в Vault/OpenBao, Kubernetes Secrets или облачном
   secret manager - не в `airflow.cfg` и не в DAG.
4. Следите за ответами 401/403: они означают истёкший/неверный секрет или
   неверную настройку клиента Keycloak.
5. Не печатайте payload'ы с токенами; модуль намеренно логирует только
   нечувствительные метаданные.
