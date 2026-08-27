# lakehouse-spark-credential-providers

Легковесный Java-модуль для Apache Spark 3.5.x, обеспечивающий динамическое извлечение секретов из **OpenBao / HashiCorp Vault** и **Yandex Cloud Lockbox** на этапе выполнения задач на Драйвере и Экзекуторах — без использования Spring Framework.

---

## 1. Общее описание и условия применения

### Назначение модуля

| Проблема | Решение |
|----------|---------|
| Пароли JDBC и ключи S3 захардкожены в `spark-defaults.conf` | Провайдеры подтягивают секреты при выполнении из Vault или Lockbox |
| Секреты попадают в Spark UI, логи, дампы конфигурации | Значения никогда не логируются; при ошибках выводятся только HTTP-коды |
| Экзекуторы обрабатывают тысячи партиций в секунду → риск DDoS на API секретов | Внутри-JVM кэш с TTL ~5 минут |

### Поддерживаемые бэкенды

| Бэкенд | JDBC-провайдер | S3-провайдер | Способ аутентификации |
|--------|----------------|--------------|-----------------------|
| OpenBao / Vault KV v2 | `BaoJdbcSecretProvider` | `BaoS3CredentialsProvider` | Env-переменная `VAULT_TOKEN` или JWT Kubernetes ServiceAccount |
| Yandex Cloud Lockbox | `YcLockboxJdbcSecretProvider` | `YcLockboxS3CredentialsProvider` | Instance Metadata (IAM-токен) или Authorized Key |

### Условия для OpenBao / Vault

1. Работающий экземпляр OpenBao или HashiCorp Vault с включённым KV v2 secrets engine.
2. Сетевая связность от каждого Spark Executor до HTTP API Vault (по умолчанию порт `8200`).
3. **Аутентификация — выбрать один из вариантов:**
   - **Token Auth** — установить переменную окружения `VAULT_TOKEN` на каждом узле кластера.
   - **Kubernetes Auth** — Pod-ы Spark должны иметь ServiceAccount с Vault-ролью; токен автоматически читается из `/var/run/secrets/kubernetes.io/serviceaccount/token`.

### Условия для Yandex Cloud Lockbox

1. Секрет в Yandex Cloud Lockbox, содержащий необходимые ключи (например `access_key`, `secret_key`, `password`).
2. Каждый Spark Worker VM или Pod должен иметь привязанный **сервисный аккаунт** с ролью `lockbox.payloadViewer`.
3. Явные credentials не нужны — IAM-токен получается автоматически через Сервис Метаданных по адресу `http://169.254.169.254/computeMetadata/v1/...`.
4. Альтернативно: установить переменную окружения `YC_AUTH_KEY_PATH` с путём к файлу authorized key, содержащему OAuth-токен.

---

## 2. Установка

### Сборка

```bash
mvn clean package -pl lakehouse-spark-credential-providers -am -DskipTests
```

### Деплой в Spark

Скопируйте JAR в директорию `spark/jars/` на каждом узле, **либо** передайте через `--jars` при запуске задачи:

```bash
spark-submit \
  --jars lakehouse-spark-credential-providers-0.8.0.jar \
  ...
```

JAR не содержит транзитивных зависимостей — все рантайм-зависимости (Spark, Hadoop, AWS SDK, Jackson) уже есть в classpath кластера.

---

## 3. Конфигурация через `spark-defaults.conf`

### 3.1 JDBC Catalog — Yandex Cloud Lockbox

```properties
# ── Подключение каталога ────────────────────────────────────────
spark.sql.catalog.processingdb=org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog
spark.sql.catalog.processingdb.url=jdbc:postgresql://db-host:5432/mydb
spark.sql.catalog.processingdb.driver=org.postgresql.Driver
spark.sql.catalog.processingdb.user=app_user

# ── Провайдер секретов ─────────────────────────────────────────
spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.YcLockboxJdbcSecretProvider
spark.sql.catalog.processingdb.secret-id=eirvjuabcdef12345678
spark.sql.catalog.processingdb.secret-key=password
```

| Свойство | Обязательно | Описание |
|----------|-------------|----------|
| `secretProvider` | да | Полное имя класса провайдера |
| `secret-id` | да | ID секрета в Lockbox |
| `secret-key` | да | Ключ внутри payload секрета |
| `secret-version` | нет | Конкретная версия; по умолчанию `latest` |

### 3.2 JDBC Catalog — OpenBao / Vault

```properties
# ── Подключение каталога ────────────────────────────────────────
spark.sql.catalog.processingdb=org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog
spark.sql.catalog.processingdb.url=jdbc:postgresql://db-host:5432/mydb
spark.sql.catalog.processingdb.driver=org.postgresql.Driver
spark.sql.catalog.processingdb.user=app_user

# ── Провайдер секретов ─────────────────────────────────────────
spark.sql.catalog.processingdb.secretProvider=org.lakehouse.security.jdbc.BaoJdbcSecretProvider
spark.sql.catalog.processingdb.vault-url=http://vault:8200
spark.sql.catalog.processingdb.secret-path=secret/data/processingdb
spark.sql.catalog.processingdb.secret-key=password
```

| Свойство | Обязательно | Описание |
|----------|-------------|----------|
| `secretProvider` | да | Полное имя класса провайдера |
| `vault-url` | да | Базовый URL HTTP API Vault |
| `secret-path` | да | Путь в KV v2 (например `secret/data/myapp/db`) |
| `secret-key` | да | Ключ внутри секрета |
| `vault-role` | нет | Роль для Kubernetes Auth (по умолчанию `lakehouse`) |
| `vault-k8s-auth-path` | нет | Путь маунта K8s auth (по умолчанию `kubernetes`) |

### 3.3 S3A / MinIO — Yandex Cloud Lockbox

```properties
# ── Файловая система S3A ───────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=https://storage.yandexcloud.net
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.impl.disable.cache=true

# ── Провайдер секретов ─────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.lakehouse.security.s3.YcLockboxS3CredentialsProvider
spark.hadoop.fs.s3a.lockbox.secret-id=eirvjuabcdef12345678
spark.hadoop.fs.s3a.lockbox.access-key-secret=access_key
spark.hadoop.fs.s3a.lockbox.secret-key-secret=secret_key
```

| Свойство | Обязательно | По умолчанию | Описание |
|----------|-------------|-------------|----------|
| `lockbox.secret-id` | да | — | ID секрета в Lockbox |
| `lockbox.access-key-secret` | нет | `access_key` | Имя ключа для AWS access key |
| `lockbox.secret-key-secret` | нет | `secret_key` | Имя ключа для AWS secret key |

### 3.4 S3A / MinIO — OpenBao / Vault

```properties
# ── Файловая система S3A ───────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=http://minio:9000
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.impl.disable.cache=true

# ── Провайдер секретов ─────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.lakehouse.security.s3.BaoS3CredentialsProvider
spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200
spark.hadoop.fs.s3a.bao.secret-path=secret/data/s3
spark.hadoop.fs.s3a.bao.access-key-secret=access_key
spark.hadoop.fs.s3a.bao.secret-key-secret=secret_key
```

| Свойство | Обязательно | По умолчанию | Описание |
|----------|-------------|-------------|----------|
| `bao.vault-url` | да | — | Базовый URL HTTP API Vault |
| `bao.secret-path` | да | — | Путь в KV для S3-credentials |
| `bao.access-key-secret` | нет | `access_key` | Имя ключа для AWS access key |
| `bao.secret-key-secret` | нет | `secret_key` | Имя ключа для AWS secret key |

---

## 4. Использование `LakehouseSecurityContext` в коде приложения

При написании кастомных Spark-трансформаций (`map`, `foreachPartition`, UDF), которым нужен секрет непосредственно внутри распределённого кода на Экзекуторе, используйте статический контекст:

```java
import org.lakehouse.security.context.LakehouseSecurityContext;

df.foreachPartition(partition -> {
    // Ленивая инициализация: клиент Vault/Lockbox создаётся один раз на JVM Экзекутора
    String dbPassword = LakehouseSecurityContext.getSecret(
        "lockbox", "eirvjuabcdef12345678", "password"
    );

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "app_user", dbPassword)) {
        // обработка партиции...
    }
});
```

### Почему нельзя создать клиент на Драйвере и передать его в лямбду?

Объекты, захваченные замыканием Spark, **сериализуются** и доставляются на Экзекуторы через Java serialization. HTTP-клиенты (`HttpClient`, клиенты Vault/Lockbox) содержат открытые сокеты, пулы потоков и несериализуемое состояние — отправка их по сети вызывает `java.io.NotSerializableException` или непредсказуемое поведение.

`LakehouseSecurityContext` решает эту проблему: статическое поле `CLIENTS` живёт в JVM Экзекутора и **никогда не сериализуется**. Клиент создаётся лениво при первом вызове внутри лямбды `foreachPartition` на Экзекуторе, а затем переиспользуется для всех последующих партиций в рамках одной JVM.

### API

```java
// Трёхаргументный вызов (рекомендуемый)
public static String getSecret(String providerType, String path, String key)

// Двухаргументный (путь должен содержать ':' как разделитель)
public static String getSecret(String providerType, String pathWithKey)
```

| Параметр | Значения |
|----------|----------|
| `providerType` | `"bao"` — OpenBao/Vault; `"lockbox"` — Yandex Lockbox |
| `path` | Vault: путь в KV (например `secret/data/db`); Lockbox: ID секрета |
| `key` | Ключ внутри секрета (например `password`, `access_key`) |

### Конфигурация через system properties / env vars

Vault-клиент читает URL Vault (в порядке приоритета):

1. System property `lakehouse.vault.url`
2. Переменная окружения `VAULT_URL`
3. Значение по умолчанию: `http://vault:8200`

---

## 5. Безопасность и отладка

### 5.1 Защита секретов в Spark UI

Включите встроенную маскировку Spark, чтобы секреты не попадали в Web UI и event logs:

```properties
spark.ui.redaction.regex=url|password|secret|token|key
```

### 5.2 Поведение кэширования

- Секреты кэшируются в памяти на JVM с **TTL 5 минут**.
- Если сервер секретов вернул ошибку, провайдер выбрасывает `SecurityException` (задача Spark падает) — ошибки **не** кэшируются.
- `refresh()` вызывается Hadoop S3A при истечении срока действия credentials — это очищает кэш и форсирует повторное получение.

### 5.3 Таймауты

| Параметр | Значение |
|----------|----------|
| Connection timeout | 3 секунды |
| Read timeout | 5 секунды |

Если сервер секретов недоступен, провайдер выбрасывает `SecurityException` с сообщением `"<сервис> unreachable: <причина>"` — **никаких секретов или токенов в логах**.

### 5.4 Что проверять при падении задачи

| Симптом | Где смотреть |
|---------|-------------|
| `SecurityException: Vault access denied, HTTP 403` | Токен Vault истёк или нет прав на чтение по указанному пути |
| `SecurityException: Lockbox access denied, HTTP 403` | Сервисный аккаунт не имеет роли `lockbox.payloadViewer` |
| `SecurityException: Metadata service unreachable` | Spark Worker не находится внутри Yandex Cloud или сервис метаданных заблокирован |
| `SecurityException: No Vault token available` | Установите `VAULT_TOKEN` env или используйте Kubernetes Auth в Pod-е с ServiceAccount |
| `IllegalArgumentExceptiSecret key not found in Vault response` | Проверьте `secret-path` и `secret-key` в конфигурации |
| JDBC-соединение падает (пароль верный в Vault) | Проверьте `user` и `url` в конфиге каталога — **только пароль** берётся из провайдера |

**Важно:** пароли и токены **никогда** не пишутся в логи ни на каком уровне (INFO, DEBUG, TRACE). При ошибках логируются только HTTP-коды ответов и типы исключений.

### 5.5 Сообщения в логах

| Уровень | Пример сообщения |
|---------|-----------------|
| `INFO` | `BaoJdbcSecretProvider initialised` |
| `ERROR` | `Vault returned HTTP 403 for path [redacted]` |
| `ERROR` | `Metadata service request failed: Connection refused` |

Путь/ID секрета всегда замаскирован как `[redacted]` в выводе логов.
