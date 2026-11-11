# yarina-credential-providers-spark

Серия Java-модулей для Apache Spark 3.5.x, обеспечивающих динамическое извлечение секретов из **OpenBao / HashiCorp Vault** и **Yandex Cloud Lockbox** на этапе выполнения задач на Драйвере и Экзекуторах — без использования Spring Framework.

Проект разделён на два модуля:

| Модуль | Содержимое |
|--------|------------|
| `yarina-credential-providers-jdbc` | Общий, Spark-независимый код: SPI `SecretProvider`, HTTP-клиенты `VaultHttp` / `LockboxHttp`, `SecretCache`, `SecurityMetrics`, JDBC-провайдеры (`BaoJdbcSecretProvider`, `YcLockboxJdbcSecretProvider`), `YarinaSecurityContext` и помощник `SecretResolver`. Переиспользуется сервисами платформы (например `yarina-task-executor-api`). |
| `yarina-credential-providers-spark` | Spark-специфичный код: S3-провайдеры (`BaoS3CredentialsProvider`, `YcLockboxS3CredentialsProvider`) и `YarinaSecureJDBCTableCatalog`. Зависит от общего модуля. |

---

## 1. Общее описание и условия применения

### Назначение модуля

| Проблема | Решение |
|----------|---------|
| Пароли JDBC и ключи S3 захардкожены в `spark-defaults.conf` | Провайдеры подтягивают секреты при выполнении из Vault или Lockbox |
| Секреты попадают в Spark UI, логи, дампы конфигурации | Значения никогда не логируются; при ошибках выводятся только HTTP-коды |
| Экзекуторы обрабатывают тысячи партиций в секунду → риск DDoS на API секретов | JVM-wide кэш с TTL 5 минут и single-flight загрузкой: один запрос к Vault на секрет за TTL-окно, независимо от числа экземпляров провайдера и партиций |

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
mvn clean package -pl yarina-credential-providers-jdbc,yarina-credential-providers-spark -am -DskipTests
```

### Деплой в Spark

Скопируйте **оба** JAR-файла в директорию `spark/jars/` на каждом узле, **либо** передайте через `--jars` при запуске задачи:

```bash
spark-submit \
  --jars yarina-credential-providers-jdbc-1.1.1.jar,yarina-credential-providers-spark-1.1.1.jar \
  ...
```

Оба JAR-файла не содержат транзитивных runtime-зависимостей — все рантайм-зависимости (Spark, Hadoop, AWS SDK, Jackson) уже есть в classpath кластера. В демостенде оба JAR-а копируются в образ скриптом `docker/yarina-spark-aws/build.bash`.

Актуальная версия — **1.1.1**. Описания релизов и история breaking changes ведутся в [`CHANGELOG.md`](../../CHANGELOG.md) в корне репозитория.

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
spark.sql.catalog.processingdb.secretProvider=org.yarina.security.jdbc.YcLockboxJdbcSecretProvider
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
spark.sql.catalog.processingdb.secretProvider=org.yarina.security.jdbc.BaoJdbcSecretProvider
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
| `vault-role` | нет | Роль для Kubernetes Auth (по умолчанию `yarina` — см. [Аутентификация](#6-аутентификация)) |
| `vault-k8s-auth-path` | нет | Путь маунта K8s auth (по умолчанию `kubernetes`) |

### 3.3 S3A / MinIO — Yandex Cloud Lockbox

```properties
# ── Файловая система S3A ───────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=https://storage.yandexcloud.net
spark.hadoop.fs.s3a.path.style.access=true

# ── Провайдер секретов ─────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.YcLockboxS3CredentialsProvider
spark.hadoop.fs.s3a.lockbox.secret-id=eirvjuabcdef12345678
spark.hadoop.fs.s3a.lockbox.access-key-secret=access_key
spark.hadoop.fs.s3a.lockbox.secret-key-secret=secret_key
```

| Свойство | Обязательно | По умолчанию | Описание |
|----------|-------------|-------------|----------|
| `lockbox.secret-id` | да | — | ID секрета в Lockbox |
| `lockbox.access-key-secret` | нет | `access_key` | Имя ключа для AWS access key |
| `lockbox.secret-key-secret` | нет | `secret_key` | Имя ключа для AWS secret key |

> `spark.hadoop.fs.s3a.impl.disable.cache=true` — **необязательная** опция, см. примечание в [3.4](#34-s3a--minio--openbao--vault).

### 3.4 S3A / MinIO — OpenBao / Vault

```properties
# ── Файловая система S3A ───────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.endpoint=http://minio:9000
spark.hadoop.fs.s3a.path.style.access=true

# ── Провайдер секретов ─────────────────────────────────────────
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.BaoS3CredentialsProvider
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
| `bao.vault-role` | нет | `yarina` | Роль Kubernetes Auth (см. [Аутентификация](#6-аутентификация)) |
| `bao.vault-k8s-auth-path` | нет | `kubernetes` | Путь маунта Kubernetes Auth в Vault |
| `bao.vault-k8s-token-path` | нет | `/var/run/secrets/kubernetes.io/serviceaccount/token` | Путь к JWT ServiceAccount |

Примечания:

- Оба ключа пары credentials (`access_key` + `secret_key`) читаются **одним** HTTP-запросом к Vault (batch-чтение всего payload секрета).
- Провайдер падает быстро (fail-fast в конструкторе), если не заданы `fs.s3a.bao.vault-url` или `fs.s3a.bao.secret-path`.
- **Про `spark.hadoop.fs.s3a.impl.disable.cache=true`:** эта опция **не обязательна** для работы провайдеров. Если она включена, каждый `FileSystem.get` создаёт новый `S3AFileSystem` **и новый экземпляр провайдера** — с версии 1.1.1 все экземпляры в одной JVM делят общий кэш credentials и Vault-клиент, поэтому опция больше не множит запросы к OpenBao (см. [5.1 Область действия кэша](#51-область-действия-кэша)); она по-прежнему множит объекты S3A-клиентов, поэтому оставляйте её только если платформа полагается на изоляцию файловых систем между задачами.

### 3.5 Мульти-бакетность и мульти-облако

Каждый бакет может переопределить опции провайдера через стандартный для S3A бакет-скоуп префикс `fs.s3a.bucket.<name>.*` — S3A копирует эти ключи поверх глобальных `fs.s3a.*` для файловой системы этого бакета:

```properties
# ── Общие настройки ─────────────────────────────────────────────
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.path.style.access=true
spark.hadoop.fs.s3a.aws.credentials.provider=org.yarina.security.s3.BaoS3CredentialsProvider
spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200

# ── Бакет "data": свой secret-path ──────────────────────────────
spark.hadoop.fs.s3a.bucket.data.endpoint=http://minio-prod:9000
spark.hadoop.fs.s3a.bucket.data.bao.secret-path=secret/data/s3-prod

# ── Бакет "archive": другой секрет, другая роль, другой Vault ────
spark.hadoop.fs.s3a.bucket.archive.endpoint=http://minio-archive:9000
spark.hadoop.fs.s3a.bucket.archive.bao.vault-url=http://vault-archive:8200
spark.hadoop.fs.s3a.bucket.archive.bao.secret-path=secret/data/s3-archive
spark.hadoop.fs.s3a.bucket.archive.bao.vault-role=archive-reader
```

| Сценарий | Поддержка с 1.1.1 |
|----------|-------------------|
| Один OpenBao, несколько бакетов с разными `secret-path` | Да — каждая пара `(vault-url, secret-path)` является независимой записью кэша; `refresh()` одного бакета не затрагивает остальные |
| Несколько инстансов OpenBao (`bao.vault-url` различается по бакетам) | Да — отдельный Vault-клиент на каждую комбинацию `(vault-url, vault-role, auth-path)` |
| Kubernetes Auth с разными ролями по бакетам (`bao.vault-role`) | Да — роль входит в ключ клиента |
| Разные статические `VAULT_TOKEN` для разных OpenBao | **Нет** — переменная окружения общая на всю JVM, статические токены не обновляются. Перегрузка `VaultHttp` с явным токеном существует только для программного использования и не выставлена как S3A/JDBC-опция; используйте Kubernetes Auth с разными ролями |

---

## 4. Использование `YarinaSecurityContext` в коде приложения

При написании кастомных Spark-трансформаций (`map`, `foreachPartition`, UDF), которым нужен секрет непосредственно внутри распределённого кода на Экзекуторе, используйте статический контекст:

```java
import org.yarina.security.context.YarinaSecurityContext;

df.foreachPartition(partition -> {
    // Ленивая инициализация: клиент Vault/Lockbox создаётся один раз на JVM Экзекутора
    String dbPassword = YarinaSecurityContext.getSecret(
        "lockbox", "eirvjuabcdef12345678", "password"
    );

    try (Connection conn = DriverManager.getConnection(jdbcUrl, "app_user", dbPassword)) {
        // обработка партиции...
    }
});
```

### Почему нельзя создать клиент на Драйвере и передать его в лямбду?

Объекты, захваченные замыканием Spark, **сериализуются** и доставляются на Экзекуторы через Java serialization. HTTP-клиенты (`HttpClient`, клиенты Vault/Lockbox) содержат открытые сокеты, пулы потоков и несериализуемое состояние — отправка их по сети вызывает `java.io.NotSerializableException` или непредсказуемое поведение.

`YarinaSecurityContext` решает эту проблему: статическое поле `CLIENTS` живёт в JVM Экзекутора и **никогда не сериализуется**. Клиент создаётся лениво при первом вызове внутри лямбды `foreachPartition` на Экзекуторе, а затем переиспользуется для всех последующих партиций в рамках одной JVM.

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

1. System property `yarina.vault.url`
2. Переменная окружения `VAULT_URL`
3. Значение по умолчанию: `http://vault:8200`

### Использование провайдеров в сервисах платформы (JDBC-путь)

Общий модуль также используется не-Spark сервисами платформы. Фабрика JDBC-соединений
`yarina-task-executor-api` (`JdbcConnectionFactory`) применяет тот же контракт опций:
если в `ServiceDTO.properties` присутствует `secretProvider`, пароль резолвится перед вызовом
`DriverManager.getConnection(...)`, а ключи безопасности удаляются из свойств JDBC.

```properties
# Service.properties JDBC-подключения (хранение ServiceDTO.properties в yarina-config-svc)
url            jdbc:postgresql://postgres:5432/yarina_db
user           yarina_user
secretProvider org.yarina.security.jdbc.BaoJdbcSecretProvider
secret-key     kv/data/yarina/database:password
vault-url      http://openbao:8200
```

Поддерживаемые ключи: `secretProvider`, `secret-key` (комбинированный формат `path:key`),
а также специфичные для провайдеров `vault-url`, `vault-role`, `vault-k8s-auth-path`,
`secret-id`, `secret-version`. Все они удаляются до передачи опций JDBC-драйверу. При отсутствии
`secretProvider` карта передаётся без изменений — существующие конфигурации работают как раньше.

---

## 5. Кэширование и нагрузка на OpenBao

### 5.1 Область действия кэша

Секреты кэшируются **на уровне JVM** (процесс Spark Driver или Экзекутора), а не на уровне экземпляра провайдера:

- Кэш credentials и Vault-клиенты `BaoS3CredentialsProvider` живут в `static`-полях и
  разделяются **всеми** экземплярами провайдера в JVM. Hadoop S3A может создавать множество
  экземпляров (по одному на `FileSystem`; при `fs.s3a.impl.disable.cache=true` — по одному
  на каждый `FileSystem.get`), но все они используют общий кэш, поэтому нагрузка на OpenBao
  пропорциональна TTL, а не количеству экземпляров.
- **TTL: 5 минут.** Просроченная запись удаляется из кэша при следующем обращении; следующий
  вызов читает свежие credentials. Ошибки **никогда** не кэшируются — неудачное чтение не
  «залипает», следующая попытка снова идёт в Vault.
- **Ключ кэша:** `bao:s3:<vault-url>:<secret-path>` — разные бакеты (разные secret-path)
  и разные Vault-серверы никогда не делят запись.
- **Single-flight:** при одновременном промахе многих потоков (например 50 конкурентных
  партиций) выполняется ровно **один** HTTP-запрос; остальные потоки ждут его результата.
  Один JVM-wide `HttpClient` разделяется на весь Vault-трафик — соединения переиспользуются,
  а не множатся.
- **Batch-чтение:** пара access/secret key читается **одним** HTTP-запросом
  (`GET /v1/<secret-path>`) вместо запроса на каждый ключ.
- `refresh()` инвалидирует только свою запись `(vault-url, secret-path)` — credentials
  соседних бакетов в той же JVM остаются в кэше.

### 5.2 Ротация секретов

- **S3-путь:** Hadoop S3A вызывает `refresh()` на провайдере при обнаружении истёкших
  credentials — это сбрасывает запись кэша только этого провайдера, и следующий запрос
  перечитывает секрет из Vault. Независимо от `refresh()`, любой ротированный секрет
  подхватывается не позднее чем через TTL (5 минут).
- **JDBC-путь:** пароль резолвится при создании соединения; ротированный секрет подхватывается
  при следующем промахе кэша (не позднее 5 минут). Уже открытые соединения продолжают работать
  со старым паролем — перезапустите приложение или дождитесь ротации соединений в пуле.
- **Ротация токена Vault** обрабатывается отдельно — см. ниже.

### 5.3 Обновление токена Vault при HTTP 403

- Токен, полученный через **Kubernetes Auth**, обновляемый: когда Vault отклоняет чтение
  HTTP 403, клиент переаутентифицируется (один Kubernetes login, даже при конкурентных
  вызовах) и повторяет запрос **ровно один раз** — шторма ретраев нет.
- Токен из `VAULT_TOKEN` (или переданный программно) — **статический**: при 403 запрос
  сразу падает без переаутентификации. Почему Kubernetes Auth рекомендован в production —
  см. [Аутентификация](#6-аутентификация).

---

## 6. Аутентификация

Приоритет разрешения токена в `VaultHttp`:

1. Явный токен, переданный программно в конструктор `VaultHttp` (не выставлен как
   Spark/Hadoop-опция);
2. Переменная окружения `VAULT_TOKEN`;
3. Kubernetes Auth (JWT ServiceAccount из
   `/var/run/secrets/kubernetes.io/serviceaccount/token` по умолчанию, настраивается через
   `fs.s3a.bao.vault-k8s-token-path`).

Рекомендации:

- **В production используйте Kubernetes Auth.** Токен ServiceAccount монтируется в Pod
  средствами kubelet, короткоживущий, привязан к Vault-роли и никогда не попадает в файлы
  конфигурации. Отклонённый токен самолечится переаутентификацией.
- **`VAULT_TOKEN` виден** любому процессу и пользователю узла через `/proc/<pid>/environ`,
  общий для всех задач на узле и никогда не обновляется — истёкший токен роняет все задачи
  с `HTTP 403` до ручной замены переменной.
- **`vault-role` по умолчанию — `yarina`.** Если роль, привязанная в OpenBao для метода
  `kubernetes`, называется иначе, её **обязательно** задать явно (`vault-role` для JDBC,
  `fs.s3a.bao.vault-role` для S3A) — несовпадение даёт
  `Vault k8s auth failed, HTTP 403` на каждом login.

---

## 7. TLS / безопасность транспорта

- Оба HTTP-клиента (`VaultHttp`, `LockboxHttp`) используют JDK `HttpClient` и полностью
  поддерживают `https://` URL Vault/Lockbox: TLS согласуется стандартным стеком JSSE,
  верификация имени хоста всегда включена.
- **Кастомный CA:** импортируйте сертификат в JVM truststore, используемый Spark-нодами
  (`keytool -importcert -cacerts`, либо укажите
  `-Djavax.net.ssl.trustStore=/path/to/truststore.jks` на кастомный). Это стандартный
  механизм для самоподписанных и корпоративных PKI-сертификатов.
- **mTLS не поддерживается** для соединения с Vault в этой версии: клиент не предъявляет
  клиентский сертификат. Аутентификация API Vault токен-ориентированная, поэтому это важно
  только если корпоративный TLS-прокси требует клиентские сертификаты — в этом случае
  терминируйте TLS в sidecar-контейнере (например `stunnel`/`envoy`) и направьте провайдер
  на локальный plaintext-endpoint.

---

## 8. Метрики и диагностика

Все счётчики JVM-wide и не требуют зависимостей (`SecurityMetrics` из общего модуля). Два способа экспорта доступны всегда:

1. **JMX** — MBean `org.yarina.security:type=SecurityMetrics` предоставляет атрибут
   `PrometheusText` (весь реестр в текстовом формате Prometheus) и таблицу `Counters`
   (одна строка на счётчик). Смотрите через `jconsole`/`jmxterm`; раздел 9 показывает,
   как доставить это в Prometheus через javaagent JMX exporter.
2. **Программно** — `org.yarina.security.SecurityMetrics.scrapePrometheus()` отдаёт тот же
   текст; сервис платформы со Spring Actuator/Micrometer (например
   `yarina-task-proxy-for-spark`) может публиковать его периодически под своим endpoint
   `/actuator/prometheus`.

| Метрика | Источник | Смысл |
|---------|----------|-------|
| `cache_hits_total` | `SecretCache` | Обращения, отработанные из кэша |
| `cache_misses_total` | `SecretCache` | Обращения, потребовавшие загрузки |
| `cache_evictions_total` | `SecretCache` | Удалённые записи (TTL, `refresh()`) |
| `vault_requests_total{op="read"}` | провайдеры | Инициированные чтения секретов |
| `vault_requests_total{op="auth"}` | провайдеры | Создания Vault-клиента |
| `provider_instances_created_total` | провайдеры | Созданные в JVM экземпляры провайдера |
| `vault_http_requests_total{status="…"}` | `VaultHttp` | «Сырые» HTTP-чтения Vault по кодам ответа |
| `vault_auth_total` | `VaultHttp` | Выполненные Kubernetes login |
| `vault_token_refresh_total` | `VaultHttp` | Обновления токена по 403 |

Как проверить нагрузку на OpenBao:

- **`cache_hits_total` должен доминировать над `cache_misses_total`** — здоровая JVM даёт
  один всплеск промахов на секрет за 5-минутное окно и тысячи попаданий между ними.
- `vault_auth_total` ≈ числу уникальных комбинаций `(vault-url, vault-role, auth-path)` в
  JVM — **не** один login на экземпляр провайдера. Шторм login-ов в access-логе OpenBao
  (`POST /v1/auth/kubernetes/login`) означает клиентов до-1.1.1 или сломанный деплой.
- Рост `provider_instances_created_total` (например по одному на `FileSystem.get` при
  `fs.s3a.impl.disable.cache=true`) — ожидаем и безвреден, пока `vault_auth_total` и
  `vault_requests_total{op="read"}` остаются пропорциональными TTL.

---

## 9. Настройка передачи метрик в Prometheus

В Spark-деплое с этими провайдерами существуют два независимых потока метрик, и каждый
подключается к Prometheus отдельно:

| Поток | Счётчики | Где живут | Экспорт |
|-------|----------|-----------|---------|
| Здоровье провайдеров секретов (раздел 8) | `cache_hits_total`, `vault_auth_total`, … | JVM каждого driver/executor Spark | javaagent JMX exporter → HTTP scrape (этот раздел) |
| Здоровье сервиса-прокси | `yarina_task_proxy4spark_*` | JVM `yarina-task-proxy-for-spark` | Spring Boot Actuator `/actuator/prometheus` — см. README прокси, раздел «Настройка Prometheus» |

Провайдеры — библиотека внутри JVM Spark и не тянут рантайм-зависимостей, поэтому сами
никуда ничего не пушат: экспорт — чисто инфраструктурная настройка. `SecurityMetrics`
публикует JMX MBean `org.yarina.security:type=SecurityMetrics`, атрибут `Counters`
которого содержит одну строку на счётчик; [Prometheus JMX exporter](https://github.com/prometheus/jmx_exporter)
в режиме javaagent превращает эти строки в HTTP-endpoint для scrape. Изменений в коде
приложений не требуется.

### 9.1 Подключение javaagent JMX exporter к JVM Spark

1. Скачайте javaagent (`jmx_prometheus_javaagent-<version>.jar`, например 1.7.0) из
   [releases JMX exporter](https://github.com/prometheus/jmx_exporter/releases) и
   разместите на всех узлах Spark — для `docker/yarina-spark-aws` добавьте его в сборку
   образа, например в `/opt/spark/jmx/`.

2. Создайте конфиг экспортёра `/opt/spark/jmx/security-metrics.yaml`. Каждая строка
   атрибута `Counters` приходит в движок правил в виде
   `org.yarina.security<type=SecurityMetrics, key=<идентификатор-метрики>><>Counters: <значение>`:

   ```yaml
   rules:
     - pattern: 'org.yarina.security<type=SecurityMetrics, key=([^>]+)><>Counters: (\d+)'
       name: yarina_security_counters
       value: '$2'
       labels:
         metric: '$1'
       type: COUNTER
       help: 'SecurityMetrics counters of the yarina credential providers (see the README section "Metrics and diagnostics")'
   ```

3. Включите javaagent на driver и executor в `spark-defaults.conf`
   (порты разные — каждая JVM обслуживает свой endpoint):

   ```properties
   # ── Экспорт метрик в Prometheus (javaagent JMX exporter) ─────────
   spark.driver.extraJavaOptions   -javaagent:/opt/spark/jmx/jmx_prometheus_javaagent-1.7.0.jar=9091:/opt/spark/jmx/security-metrics.yaml
   spark.executor.extraJavaOptions -javaagent:/opt/spark/jmx/jmx_prometheus_javaagent-1.7.0.jar=9092:/opt/spark/jmx/security-metrics.yaml
   ```

Порт driver должен быть свободен на узле driver, порт executor — на каждом узле worker.
Нескольким JVM одного типа на одном узле (например нескольким executor-ам воркера) нужен
свой порт каждой — иначе держите один executor на узел. В deploy-режиме `cluster` (так
отправляет приложения `yarina-task-proxy-for-spark`) driver тоже запускается на узле
worker.

Быстрая проверка во время работы задачи:

```bash
curl -s http://<узел-driver>:9091/metrics | grep yarina_security
```

Каждый счётчик из раздела 8 экспортируется как один семпл единого семейства метрик
(экспортёр автоматически добавляет суффикс `_total` к именам counter):

```
yarina_security_counters_total{metric="cache_hits_total"} 18234
yarina_security_counters_total{metric="vault_auth_total"} 3
yarina_security_counters_total{metric="vault_requests_total{op=\"read\"}"} 29
```

Демо-стенд содержит эту настройку в готовом виде: агент запечён в образ
`yarina-spark-aws` (`/opt/spark/jmx/`), конфиг правил лежит в
`demo/compose/conf_infra/security-metrics.yaml`, а опции `-javaagent` уже вписаны в
`demo/compose/conf_infra/spark-defaults.conf` (раздел 7) вместе с соответствующими
scrape-задачами Prometheus.

### 9.2 Конфигурация scrape в Prometheus

`prometheus.yml` (имена хостов соответствуют демо-стенду `demo/compose` — замените на
реальные; эталонная конфигурация лежит в
`demo/compose/conf_infra/prometheus/prometheus.yml`):

```yaml
scrape_configs:
  - job_name: spark-security-driver
    static_configs:
      - targets: ['spark-worker-1:9091']
        labels:
          role: driver
  - job_name: spark-security-executors
    static_configs:
      - targets: ['spark-worker-1:9092']
        labels:
          role: executor
```

### 9.3 Чтение метрик в PromQL / Grafana

Идентификатор метрики из раздела 8 передаётся в label `metric`:

| Что контролировать (раздел 8) | PromQL |
|------------------------------|--------|
| Доля попаданий в кэш | `rate(yarina_security_counters_total{metric="cache_hits_total"}[5m]) / (rate(yarina_security_counters_total{metric="cache_hits_total"}[5m]) + rate(yarina_security_counters_total{metric="cache_misses_total"}[5m]))` |
| Чтения Vault | `sum by (instance) (rate(yarina_security_counters_total{metric=~"vault_requests_total.*"}[5m]))` |
| Kubernetes login | `rate(yarina_security_counters_total{metric="vault_auth_total"}[5m])` |
| Обновления токена по 403 | `rate(yarina_security_counters_total{metric="vault_token_refresh_total"}[5m])` |
| HTTP к Vault по кодам ответа | `rate(yarina_security_counters_total{metric=~"vault_http_requests_total.*"}[5m])` |

Рост `vault_auth_total` / `vault_requests_total{op="read"}` в расчёте на `instance` —
сигнатура «login-шторма» из раздела 8; `vault_http_requests_total{status="5.."}` указывает
на проблемы на стороне OpenBao. На демо-стенде доступен Grafana с преднастроенным
источником данных Prometheus — см. `demo/compose/README-INSTALL.md`.

### 9.4 Проверка без Prometheus

- `curl http://<узел-driver>:9091/metrics` — вывод javaagent, показанный выше.
- `jconsole` / `jmxterm` — MBean `org.yarina.security:type=SecurityMetrics`: атрибут
  `PrometheusText` (весь реестр текстом) и таблица `Counters`. Javaagent не нужен,
  работает в любой JVM с провайдерами в classpath.
- `SecurityMetrics.scrapePrometheus()` — тот же текст программно; сервис платформы,
  встраивающий провайдеры (например `yarina-task-executor-api`), может публиковать его
  под собственным actuator-endpoint.

---

## 10. Безопасность и отладка

### 10.1 Защита секретов в Spark UI

Включите встроенную маскировку Spark, чтобы секреты не попадали в Web UI и event logs:

```properties
spark.ui.redaction.regex=url|password|secret|token|key
```

### 10.2 Таймауты

| Параметр | Значение |
|----------|----------|
| Connection timeout | 3 секунды |
| Read timeout | 5 секунд |

Если сервер секретов недоступен, провайдер выбрасывает `SecurityException` — **никаких секретов или токенов в логах**. Ответы не-200 (кроме 403 с обновляемым токеном, который вызывает ровно одну переаутентификацию + повтор) **не ретраятся**.

### 10.3 Что проверять при падении задачи

| Симптом | Где смотреть |
|---------|-------------|
| `SecurityException: Vault access denied, HTTP 403` | Токен Vault истёк или нет прав на чтение по указанному пути. С `VAULT_TOKEN` это падает навсегда (статический токен не обновляется) — используйте Kubernetes Auth, который переаутентифицируется автоматически |
| `SecurityException: Vault k8s auth failed, HTTP 403` | `vault-role` не совпадает с ролью, привязанной в OpenBao для метода `kubernetes`; по умолчанию роль `yarina` — задайте явно, если у вас другая |
| `SecurityException: No Vault token available` | Установите `VAULT_TOKEN` env или используйте Kubernetes Auth в Pod-е с ServiceAccount |
| `SecurityException: Secret key not found in Vault response` | Проверьте `secret-path` и `secret-key` в конфигурации |
| `SecurityException: BaoS3CredentialsProvider: secret key 'access_key' not found in Vault payload` | В KV-секрете по `bao.secret-path` нет ключей с заданными именами (`bao.access-key-secret` / `bao.secret-key-secret`) |
| Много записей `POST /v1/auth/kubernetes/login` в access-логе OpenBao | Признак пересоздания Vault-клиентов (поведение до 1.1.1: клиент на каждый экземпляр провайдера). С 1.1.1 JVM выполняет один login на комбинацию `(vault-url, vault-role, auth-path)`; обновите и передеплойте JAR-ы |
| `SecurityException: Lockbox access denied, HTTP 403` | Сервисный аккаунт не имеет роли `lockbox.payloadViewer` |
| `SecurityException: Metadata service unreachable` | Spark Worker не находится внутри Yandex Cloud или сервис метаданных заблокирован |
| JDBC-соединение падает (пароль верный в Vault) | Проверьте `user` и `url` в конфиге каталога — **только пароль** берётся из провайдера |

**Важно:** пароли и токены **никогда** не пишутся в логи ни на каком уровне (INFO, DEBUG, TRACE). При ошибках логируются только HTTP-коды ответов и типы исключений.

### 10.4 Сообщения в логах

| Уровень | Пример сообщения |
|---------|-----------------|
| `INFO` | `BaoJdbcSecretProvider initialised` |
| `INFO` | `BaoS3CredentialsProvider initialised` |
| `INFO` | `Creating Vault client for [redacted]` |
| `INFO` | `Vault token was rejected (HTTP 403); re-authenticating` |
| `ERROR` | `Vault returned HTTP 403 for path [redacted]` |
| `ERROR` | `Metadata service request failed: Connection refused` |

Путь/ID секрета всегда замаскирован как `[redacted]` в выводе логов.

---

## 11. Примеры payload секретов

### Vault KV v2 (OpenBao)

```bash
# записать пару S3-credentials (значения для примера; реальные — не коммитить)
bao kv put secret/data/s3 \
    access_key='mInIo-ak-9f2e11a8b64c' \
    secret_key='REDACTED-32-chars-secret-value'
```

Ответ `GET /v1/secret/data/s3` (значения замаскированы):

```json
{
  "data": {
    "data": {
      "access_key": "mInIo-ak-9f2e11••••",
      "secret_key": "••••••••••••••••••••••••"
    },
    "metadata": { "created_time": "2026-11-03T10:15:00Z", "version": 3 }
  }
}
```

Провайдер отображает `access_key` → AWS access key и `secret_key` → AWS secret key
(имена ключей настраиваются через `bao.access-key-secret` / `bao.secret-key-secret`).
JDBC-секрет (`secret/data/myapp/db`) имеет тот же формат, например с ключом `password`.

### Yandex Cloud Lockbox

Секрет Lockbox с ID `eirvjuabcdef12345678` содержит `entries` (значения замаскированы):

```json
{
  "entries": [
    { "key": "access_key", "value": "YCAJEvh7yZ•••••••••" },
    { "key": "secret_key", "value": "••••••••••••••••••••••••••••" }
  ]
}
```

Провайдер отображает entries по имени ключа; `lockbox.access-key-secret` /
`lockbox.secret-key-secret` выбирают, какие ключи содержат пару AWS-credentials.
