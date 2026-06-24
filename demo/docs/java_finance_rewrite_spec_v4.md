# Техническое руководство по переписыванию системы финансовой аналитики Wildberries на Java

Версия 4. Docker и Docker Compose зафиксированы как единственное дополнительное инфраструктурное решение. Kafka и другие брокеры сообщений исключены.

## 1. Назначение документа

Документ описывает целевую Java-версию существующего Laravel-приложения и предназначен для последовательной реализации проекта с помощью ИИ. Основанием служат текущий код, проектная записка и зафиксированные изменения архитектуры.

Документ следует использовать как основной источник требований. При генерации кода отдельные запросы к ИИ должны ссылаться на конкретный раздел, класс, таблицу или сценарий этого документа. Новые функции, отсутствующие в документе, не добавляются без отдельного решения.

## 2. Зафиксированные границы системы

### 2.1 Функции, которые сохраняются

- регистрация и вход пользователя;
- сессионная аутентификация Spring Security;
- разделение данных по пользователю;
- объединенная страница профиля и настроек;
- хранение API-токена Wildberries;
- настройка налоговой ставки;
- история себестоимости товара;
- ручной ввод и изменение себестоимости;
- загрузка финансовой детализации Wildberries через API;
- журнал загрузок;
- календарь покрытия дат;
- выбор периода формирования отчета;
- проверка полного покрытия выбранного периода;
- скачивание расширенного Excel-отчета;
- аналитический блок за выбранный период;
- ABC-анализ внутри Excel-файла.

### 2.2 Функции, которые изменяются

- профиль и настройки объединяются в раздел `Аккаунт`;
- финансовый контур сокращается до двух финансовых таблиц;
- первый слой хранит исходные операции API;
- второй слой хранит рассчитанные дневные строки товаров и одну общую строку дня;
- итог отчета не хранится по импорту;
- отчет за период формируется суммированием дневных строк;
- распределение сохраняется только для логистического пула и связанных с ним вознаграждений ПВЗ;
- распределение выполняется равными долями;
- краткая таблица в интерфейсе заменяется аналитическим блоком;
- кнопка скачивания Excel размещается перед аналитическим блоком;
- структура затрат показывается круговой диаграммой, справа выводятся суммы и проценты.

### 2.3 Функции, которые удаляются

- входной импорт финансовых Excel-файлов;
- импорт себестоимости из Excel;
- смешанный режим API и Excel;
- отдельный слой распределенных операций;
- таблица агрегатов по импорту;
- проекция итогового отчета по импорту;
- модуль автоакций;
- универсальная категория `Прочие расходы`;
- глобальное распределение хранения, штрафов, удержаний и других категорий по товарам;
- отладочное сравнение старой и новой расчетной реализации.

## 3. Пользовательский сценарий финансового анализа

Последовательность сохраняет текущую страницу приложения:

1. Пользователь выбирает период загрузки.
2. Система загружает детализацию Wildberries.
3. Система сохраняет исходные операции.
4. Система определяет затронутые даты.
5. Для затронутых дат перестраиваются дневные показатели.
6. Календарь показывает покрытые и отсутствующие даты.
7. Пользователь выбирает период отчета.
8. Система проверяет полное покрытие периода.
9. При полном покрытии становится доступна кнопка скачивания Excel.
10. Ниже отображается аналитический блок по тому же периоду.

## 4. Технологический стек

### 4.1 Backend

- Java 21;
- Spring Boot;
- Spring Web MVC;
- Spring Data JPA;
- JdbcClient для агрегирующих запросов;
- Spring Security;
- Jakarta Bean Validation;
- PostgreSQL;
- Flyway;
- springdoc-openapi;
- Apache POI;
- JUnit 5;
- Mockito;
- Testcontainers;
- Maven.

### 4.2 Frontend

- Thymeleaf;
- Bootstrap;
- Chart.js;
- обычный JavaScript;
- серверная сессия и CSRF Spring Security.

### 4.3 Инфраструктура

- Docker;
- Docker Compose;
- GitHub;
- GitHub Actions.

В инфраструктуру проекта добавляются только Docker и Docker Compose. Kafka, RabbitMQ, Redis, Kubernetes, Spring Batch и отдельные брокеры сообщений не используются. Это ограничение является частью утвержденной архитектуры: приложение остается модульным монолитом, PostgreSQL является единственным постоянным хранилищем, а загрузка финансовых данных запускается внутри приложения.

## 5. Архитектурный стиль

Приложение реализуется как модульный монолит.

```text
Browser
  ↓
Thymeleaf pages + REST endpoints
  ↓
MVC controllers / REST controllers
  ↓
Application services
  ↓
Domain calculators and policies
  ↓
JPA repositories / JdbcClient / WB client
  ↓
PostgreSQL / Wildberries API
```

### 5.1 Правила зависимостей

- MVC-контроллеры не содержат финансовых формул.
- REST-контроллеры не возвращают JPA Entity.
- Сетевой вызов к WB API не выполняется внутри транзакции.
- Транзакция начинается на уровне application service.
- Финансовые вычисления выполняются через `BigDecimal`.
- Второй слой пересчитывается только для затронутых дат.
- Отчет не записывает новую проекцию.
- Все запросы фильтруются по текущему пользователю.
- Неизвестные операции не скрываются в общей категории.

## 6. Структура пакетов

```text
ru.marketplace.finance
├── account
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── cost
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── synchronization
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── finance
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── export
│   ├── application
│   └── infrastructure
└── common
    ├── config
    ├── exception
    ├── security
    ├── validation
    └── web
```

## 7. База данных

Финансовых таблиц ровно две:

1. `financial_operations_raw`;
2. `daily_finance_entries`.

Служебные таблицы пользователей, токенов маркетплейса, себестоимости и журнала загрузок не считаются финансовыми слоями.

Целевая Java-версия не копирует Laravel-схему один к одному. Из Laravel переносится бизнес-смысл, а не устаревшие промежуточные таблицы:

- `users` сохраняется, но вместо `name`, `password`, `is_admin` используются `display_name`, `password_hash`, `role`;
- `user_settings` не создаётся, потому что единственная сохраняемая настройка — налоговая ставка — хранится в `users.tax_percent`;
- `wb_api_key_stats` из Laravel-настроек переносится в отдельную таблицу `marketplace_credentials`, чтобы отделить секреты от профиля пользователя;
- `product_costs` сохраняется как история себестоимости с датой начала действия;
- `wb_imports` заменяется журналом `sync_jobs`;
- `wb_fin_ops_raw` заменяется первым финансовым слоем `financial_operations_raw`;
- `wb_fin_ops_items`, `wb_fin_ops_agg`, `wb_fin_report_items`, `excel_fin_report_items`, `sales_imports` и `sales_rows` не переносятся;
- отдельная таблица покрытия дат не создаётся, покрытие определяется по `daily_finance_entries`.

### 7.1 `users`

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    tax_percent     NUMERIC(7,4) NOT NULL DEFAULT 0.0000,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    role            VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_tax_percent CHECK (tax_percent >= 0 AND tax_percent <= 100)
);
```

`tax_percent` хранит итоговый пользовательский процент налога. Интерфейс может предлагать налоговые режимы как пресеты, но в базу сохраняется только процент. Значение по умолчанию равно `0.0000`, чтобы пользователь явно выбрал ставку или режим налога в аккаунте.

### 7.2 `marketplace_credentials`

```sql
CREATE TABLE marketplace_credentials (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE REFERENCES users(id),
    provider          VARCHAR(30) NOT NULL DEFAULT 'WILDBERRIES',
    encrypted_token   TEXT NOT NULL,
    token_mask        VARCHAR(30),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Токен не возвращается через API и не выводится в логах.

### 7.3 `product_costs`

```sql
CREATE TABLE product_costs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    nm_id           BIGINT NOT NULL,
    product_name    VARCHAR(255),
    valid_from      DATE NOT NULL,
    cost_amount     NUMERIC(19,2) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_product_cost_non_negative CHECK (cost_amount >= 0),
    CONSTRAINT uq_product_cost_date UNIQUE (user_id, nm_id, valid_from)
);

CREATE INDEX idx_product_cost_lookup
    ON product_costs(user_id, nm_id, valid_from DESC);
```

Стоимость на дату выбирается как последняя запись с `valid_from <= business_date`.

### 7.4 `sync_jobs`

```sql
CREATE TABLE sync_jobs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    date_from           DATE NOT NULL,
    date_to             DATE NOT NULL,
    status              VARCHAR(40) NOT NULL,
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    received_rows       INTEGER NOT NULL DEFAULT 0,
    inserted_rows       INTEGER NOT NULL DEFAULT 0,
    updated_rows        INTEGER NOT NULL DEFAULT 0,
    duplicate_rows      INTEGER NOT NULL DEFAULT 0,
    affected_days       INTEGER NOT NULL DEFAULT 0,
    unrecognized_rows   INTEGER NOT NULL DEFAULT 0,
    error_code          VARCHAR(100),
    error_message       TEXT,
    CONSTRAINT chk_sync_period CHECK (date_from <= date_to)
);
```

Статусы:

```java
public enum SyncStatus {
    CREATED,
    RUNNING,
    RAW_SAVED,
    DAILY_RECALCULATED,
    COMPLETED,
    FAILED
}
```

### 7.5 Первый финансовый слой `financial_operations_raw`

```sql
CREATE TABLE financial_operations_raw (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    sync_job_id             BIGINT NOT NULL REFERENCES sync_jobs(id),

    row_hash                VARCHAR(64) NOT NULL,
    external_operation_id   VARCHAR(255),
    srid                    VARCHAR(255),
    nm_id                   BIGINT,

    supplier_oper_name      VARCHAR(255),
    document_type           VARCHAR(100),

    order_at                TIMESTAMPTZ,
    sale_at                 TIMESTAMPTZ,
    report_at               TIMESTAMPTZ,
    business_date           DATE NOT NULL,

    quantity                INTEGER,
    retail_amount           NUMERIC(19,2),
    retail_amount_with_discount NUMERIC(19,2),
    seller_amount           NUMERIC(19,2),
    commission_amount       NUMERIC(19,2),
    logistics_amount        NUMERIC(19,2),
    rebill_logistics_amount NUMERIC(19,2),
    pvz_reward_amount       NUMERIC(19,2),
    acquiring_amount        NUMERIC(19,2),
    storage_amount          NUMERIC(19,2),
    acceptance_amount       NUMERIC(19,2),
    penalty_amount          NUMERIC(19,2),
    deduction_amount        NUMERIC(19,2),

    classification_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    classification_code     VARCHAR(60),
    raw_payload             JSONB NOT NULL,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_raw_row_hash UNIQUE (user_id, row_hash)
);

CREATE INDEX idx_raw_user_date
    ON financial_operations_raw(user_id, business_date);

CREATE INDEX idx_raw_user_srid_date
    ON financial_operations_raw(user_id, srid, business_date);

CREATE INDEX idx_raw_user_nm_date
    ON financial_operations_raw(user_id, nm_id, business_date);
```

Назначение:

- хранение полного ответа WB;
- дедупликация;
- аудит;
- повторный пересчет;
- восстановление происхождения суммы;
- диагностика неизвестных операций.

### 7.6 Второй финансовый слой `daily_finance_entries`

Таблица содержит одну строку на товар за день и ровно одну общую строку без товара за каждый успешно покрытый день.

```sql
CREATE TABLE daily_finance_entries (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL REFERENCES users(id),
    business_date                   DATE NOT NULL,
    nm_id                           BIGINT,
    product_name                    VARCHAR(255),

    sales_quantity                  INTEGER NOT NULL DEFAULT 0,
    return_quantity                 INTEGER NOT NULL DEFAULT 0,
    net_quantity                    INTEGER NOT NULL DEFAULT 0,

    sales_amount                    NUMERIC(19,2) NOT NULL DEFAULT 0,
    returns_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    net_revenue_amount              NUMERIC(19,2) NOT NULL DEFAULT 0,

    commission_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    logistics_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,

    cost_amount                     NUMERIC(19,2) NOT NULL DEFAULT 0,
    tax_amount                      NUMERIC(19,2) NOT NULL DEFAULT 0,
    product_profit_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,

    acquiring_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    storage_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    acceptance_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    penalty_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    additional_deductions_amount    NUMERIC(19,2) NOT NULL DEFAULT 0,

    has_cost                        BOOLEAN NOT NULL DEFAULT TRUE,
    calculation_version             INTEGER NOT NULL,
    calculated_at                   TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_daily_row_shape CHECK (
        (nm_id IS NOT NULL)
        OR
        (
            nm_id IS NULL
            AND sales_quantity = 0
            AND return_quantity = 0
            AND net_quantity = 0
            AND sales_amount = 0
            AND returns_amount = 0
            AND net_revenue_amount = 0
            AND commission_amount = 0
            AND logistics_amount = 0
            AND cost_amount = 0
            AND tax_amount = 0
            AND product_profit_amount = 0
        )
    )
);

CREATE UNIQUE INDEX uq_daily_product
    ON daily_finance_entries(user_id, business_date, nm_id)
    WHERE nm_id IS NOT NULL;

CREATE UNIQUE INDEX uq_daily_common
    ON daily_finance_entries(user_id, business_date)
    WHERE nm_id IS NULL;

CREATE INDEX idx_daily_report_period
    ON daily_finance_entries(user_id, business_date);
```

Покрытие периода определяется по наличию строк в `daily_finance_entries`. После успешной загрузки за каждую дату периода создаётся общая строка `nm_id IS NULL`, даже если за дату нет операций и все суммы равны нулю. Если день отсутствует в `daily_finance_entries`, он считается непокрытым.

#### Товарная строка

Заполнены:

- `nm_id`;
- количество;
- продажи;
- возвраты;
- комиссия;
- распределенная логистика;
- себестоимость;
- налог;
- прибыль.

Поля общих расходов равны нулю.

#### Общая строка дня

`nm_id = null`.

Заполнены:

- эквайринг;
- хранение;
- приемка;
- штрафы;
- дополнительные удержания.

Товарные поля равны нулю.

## 8. JPA-модели

### 8.1 `RawFinancialOperationEntity`

```java
@Entity
@Table(
    name = "financial_operations_raw",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_raw_row_hash",
        columnNames = {"user_id", "row_hash"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawFinancialOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sync_job_id", nullable = false)
    private Long syncJobId;

    @Column(name = "row_hash", nullable = false, length = 64)
    private String rowHash;

    private String srid;

    @Column(name = "nm_id")
    private Long nmId;

    @Column(name = "supplier_oper_name")
    private String supplierOperationName;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    private Integer quantity;

    @Column(name = "retail_amount", precision = 19, scale = 2)
    private BigDecimal retailAmount;

    @Column(name = "commission_amount", precision = 19, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "logistics_amount", precision = 19, scale = 2)
    private BigDecimal logisticsAmount;

    @Column(name = "pvz_reward_amount", precision = 19, scale = 2)
    private BigDecimal pvzRewardAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false)
    private ClassificationStatus classificationStatus;
}
```

Entity не содержит финансовых расчетов. Она отражает сохраненную внешнюю операцию.

### 8.2 `DailyFinanceEntryEntity`

```java
@Entity
@Table(name = "daily_finance_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyFinanceEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "nm_id")
    private Long nmId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "sales_quantity", nullable = false)
    private int salesQuantity;

    @Column(name = "return_quantity", nullable = false)
    private int returnQuantity;

    @Column(name = "net_quantity", nullable = false)
    private int netQuantity;

    @Column(name = "sales_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal salesAmount;

    @Column(name = "returns_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal returnsAmount;

    @Column(name = "net_revenue_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal netRevenueAmount;

    @Column(name = "commission_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "logistics_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal logisticsAmount;

    @Column(name = "cost_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal costAmount;

    @Column(name = "tax_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "product_profit_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal productProfitAmount;

    @Column(name = "acquiring_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal acquiringAmount;

    @Column(name = "storage_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal storageAmount;

    @Column(name = "acceptance_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal acceptanceAmount;

    @Column(name = "penalty_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal penaltyAmount;

    @Column(name = "additional_deductions_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal additionalDeductionsAmount;

    @Column(name = "has_cost", nullable = false)
    private boolean hasCost;

    @Column(name = "calculation_version", nullable = false)
    private int calculationVersion;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
```

Создание строк выполняется через фабричные методы или отдельный builder. Публичные setter-методы не используются.

## 9. Репозитории

### 9.1 RAW

```java
public interface RawFinancialOperationRepository
        extends JpaRepository<RawFinancialOperationEntity, Long> {

    List<RawFinancialOperationEntity> findAllByUserIdAndBusinessDate(
        Long userId,
        LocalDate businessDate
    );

    boolean existsByUserIdAndRowHash(Long userId, String rowHash);

    @Query("""
        select distinct r.businessDate
        from RawFinancialOperationEntity r
        where r.userId = :userId
          and r.syncJobId = :syncJobId
    """)
    Set<LocalDate> findAffectedDates(Long userId, Long syncJobId);
}
```

Для массовой вставки допускается отдельный `RawOperationBatchRepository` на `JdbcClient`.

### 9.2 Дневной слой

```java
public interface DailyFinanceEntryRepository
        extends JpaRepository<DailyFinanceEntryEntity, Long> {

    @Modifying
    @Query("""
        delete from DailyFinanceEntryEntity d
        where d.userId = :userId
          and d.businessDate = :businessDate
    """)
    void deleteDay(Long userId, LocalDate businessDate);

    List<DailyFinanceEntryEntity> findAllByUserIdAndBusinessDateBetween(
        Long userId,
        LocalDate dateFrom,
        LocalDate dateTo
    );
}
```

Агрегаты отчета выполняются через `JdbcClient`, чтобы избежать загрузки всех строк в память.

## 10. Получение данных Wildberries

### 10.1 Порт клиента

```java
public interface MarketplaceFinanceClient {
    FinancePage loadFinancePage(
        String token,
        Instant from,
        Long cursor
    );
}
```

### 10.2 Адаптер

```java
@Component
@RequiredArgsConstructor
public class WildberriesFinanceClient
        implements MarketplaceFinanceClient {

    private final RestClient restClient;
    private final WildberriesProperties properties;
}
```

Клиент отвечает только за HTTP:

- URL;
- заголовок авторизации;
- timeout;
- пагинацию;
- 401;
- 429;
- 5xx;
- преобразование JSON во внешние DTO.

Клиент не сохраняет данные и не рассчитывает отчет.

### 10.3 Исключения

```text
WildberriesUnauthorizedException
WildberriesRateLimitException
WildberriesUnavailableException
WildberriesInvalidResponseException
```

Повторы выполняются только для 429 и временных 5xx. Количество попыток ограничено.

## 11. Определение бизнес-даты

Текущее правило переносится в один класс:

```text
report_at → sale_at → order_at
```

```java
@Component
public class BusinessDateResolver {

    public LocalDate resolve(WbFinanceRowDto row) {
        if (row.reportAt() != null) {
            return row.reportAt().toLocalDate();
        }
        if (row.saleAt() != null) {
            return row.saleAt().toLocalDate();
        }
        if (row.orderAt() != null) {
            return row.orderAt().toLocalDate();
        }
        throw new MissingBusinessDateException();
    }
}
```

Ни один другой класс не повторяет это правило.

## 12. Хэш и дедупликация

Хэш строится только по стабильным полям:

```text
external operation id
srid
nm_id
supplier_oper_name
document_type
business date
quantity
retail amount
seller amount
commission
logistics
storage
acceptance
penalty
deduction
```

```java
public interface OperationHashCalculator {
    String calculate(WbFinanceRowDto row, LocalDate businessDate);
}
```

Используется SHA-256. Перед объединением значения нормализуются:

- `null` заменяется пустой строкой;
- даты приводятся к ISO;
- `BigDecimal` приводится через `stripTrailingZeros().toPlainString()`;
- разделитель полей фиксирован.

## 13. Классификация операций

### 13.1 Enum

```java
public enum OperationClass {
    SALE,
    RETURN,
    LOGISTICS,
    STORAGE,
    ACQUIRING,
    ACCEPTANCE,
    PENALTY,
    DEDUCTION,
    PVZ_REWARD,
    UNRECOGNIZED
}
```

### 13.2 Классификатор

```java
public interface OperationClassifier {
    OperationClass classify(WbFinanceRowDto row);
}
```

В первой версии правила соответствуют текущему коду:

- точное название `Продажа` относится к `SALE`;
- точное название `Возврат` относится к `RETURN`;
- логистические операции определяются утвержденным набором названий;
- хранение, приемка, штраф и удержание определяются отдельно;
- вознаграждение ПВЗ рассматривается как часть логистического пула только для операций, признанных логистическими;
- неизвестные названия получают `UNRECOGNIZED`.

Полный список реальных `supplier_oper_name` необходимо вынести в конфигурацию или Java-класс и покрыть тестами на основе данных текущего приложения.

## 14. Расчет исходной выручки

Для продажи:

```text
sales_amount += abs(resolved revenue)
sales_quantity += abs(quantity)
```

Для возврата:

```text
returns_amount += abs(resolved revenue)
return_quantity += abs(quantity)
```

Чистая выручка:

```text
net_revenue = sales_amount - returns_amount
```

Чистое количество:

```text
net_quantity = sales_quantity - return_quantity
```

Функция `resolveRevenue` должна быть перенесена из текущего кода после точной фиксации приоритета полей. Это отдельный доменный класс:

```java
public interface RevenueResolver {
    BigDecimal resolve(WbFinanceRowDto row);
}
```

## 15. Логистический пул

Распределение сохраняется только для:

- логистики без прямой товарной строки внутри группы;
- вознаграждения ПВЗ, относящегося к логистической операции.

### 15.1 Группа

Ключ группы:

```text
user_id + business_date + srid
```

Если `srid` отсутствует, используется специальный ключ `NO_SRID` внутри даты.

### 15.2 Формула пула

```text
logistics_pool =
    logistics_amount
  + rebill_logistics_amount
  + pvz_reward_component
```

Знак вознаграждения ПВЗ нельзя безусловно преобразовывать через `abs`. Он определяется классификацией операции. Компенсация должна уменьшать расход.

### 15.3 Распределение внутри группы

Если в группе есть товары, пул распределяется поровну между уникальными `nm_id`.

```text
share = pool / number_of_unique_products
```

Число исходных строк не влияет на количество долей.

### 15.4 Перенос на уровень дня

Если в группе нет товара, пул добавляется во временный дневной пул.

После обработки всех групп:

- если в дне есть товары, дневной пул поровну распределяется между уникальными товарами дня;
- если в дне нет товаров, дневной пул добавляется в `additional_deductions_amount` общей строки дня.

### 15.5 Округление

```text
100 / 3 = 33,33 + 33,33 + 33,34
```

Товары сортируются по `nm_id`. Последний товар получает остаток.

```java
@Component
public class EqualMoneyAllocator {

    public Map<Long, BigDecimal> allocate(
        BigDecimal total,
        List<Long> productIds
    ) {
        // сортировка, расчет долей, остаток последнему элементу
    }
}
```

## 16. Общая строка дня

В общую строку дня накапливаются:

- эквайринг;
- хранение;
- приемка;
- штрафы;
- прямые дополнительные удержания;
- логистика и вознаграждение ПВЗ, которые невозможно распределить из-за полного отсутствия товаров в дне.

Формула:

```text
additional_deductions =
    direct_deductions
  + unresolved_day_logistics
  + unresolved_day_pvz_reward
```

В RAW значения остаются разделенными.

## 17. Себестоимость

Для каждого товара и дня выбирается стоимость:

```sql
SELECT cost_amount
FROM product_costs
WHERE user_id = :userId
  AND nm_id = :nmId
  AND valid_from <= :businessDate
ORDER BY valid_from DESC
LIMIT 1;
```

Дневная себестоимость:

```text
cost_amount = net_quantity × cost_per_unit
```

Если `net_quantity` отрицательно, себестоимость также уменьшает ранее учтенную себестоимость.

Если стоимость отсутствует:

- `has_cost = false`;
- `cost_amount = 0` для хранения;
- прибыль рассчитывается без себестоимости только как техническое значение;
- интерфейс и Excel помечают строку как неполную;
- количество товаров без себестоимости выводится в аналитике.

## 18. Налог

Текущее правило сохраняется:

```text
tax = max(net_revenue, 0) × tax_percent / 100
```

Налог не становится отрицательным при отрицательной чистой выручке.

```java
@Component
public class TaxCalculator {

    public BigDecimal calculate(
        BigDecimal netRevenue,
        BigDecimal taxPercent
    ) {
        if (netRevenue.signum() <= 0) {
            return Money.zero();
        }
        return Money.round(
            netRevenue.multiply(taxPercent)
                .divide(new BigDecimal("100"))
        );
    }
}
```

## 19. Прибыль и маржинальность

Товарная прибыль:

```text
product_profit =
    net_revenue
  - commission
  - logistics
  - cost
  - tax
```

Общие расходы кабинета в товарную прибыль не включаются.

Итоговая прибыль периода:

```text
total_profit =
    sum(product_profit)
  - acquiring
  - storage
  - acceptance
  - penalties
  - additional_deductions
```

Маржинальность:

```text
margin_percent = total_profit / net_revenue × 100
```

При нулевой чистой выручке маржинальность равна `null`.

## 20. Пересчет дня

```java
@Service
@RequiredArgsConstructor
public class RecalculateFinanceDayService {

    private final RawFinancialOperationRepository rawRepository;
    private final DailyFinanceEntryRepository dailyRepository;
    private final OperationClassifier classifier;
    private final RevenueResolver revenueResolver;
    private final EqualMoneyAllocator allocator;
    private final ProductCostResolver productCostResolver;
    private final TaxCalculator taxCalculator;

    @Transactional
    public void recalculate(Long userId, LocalDate date) {
        // 1. загрузить RAW дня
        // 2. классифицировать
        // 3. сгруппировать по srid
        // 4. построить товарные накопители
        // 5. распределить логистические пулы
        // 6. сформировать общую строку
        // 7. рассчитать себестоимость и налог
        // 8. удалить старые дневные строки
        // 9. сохранить новые строки
    }
}
```

Пересчет идемпотентен. Повторный запуск на одинаковом RAW-наборе формирует одинаковый результат.

## 21. Полный сценарий синхронизации

```java
@Service
@RequiredArgsConstructor
public class StartFinanceSyncService {

    public Long start(Long userId, SyncPeriod period) {
        // создать sync job и поставить выполнение в executor
    }
}
```

Фоновый обработчик:

1. Проверить отсутствие пересекающейся активной загрузки пользователя.
2. Получить токен.
3. Создать или перевести `sync_job` в `RUNNING`.
4. Постранично получить ответ WB.
5. Для каждой строки определить бизнес-дату.
6. Рассчитать хэш.
7. Вставить новую строку либо обновить изменившуюся.
8. Зафиксировать `RAW_SAVED`.
9. Получить список затронутых дат.
10. Для каждой даты выполнить `RecalculateFinanceDayService`.
11. Для каждой успешно загруженной даты периода гарантировать общую строку `daily_finance_entries` с `nm_id IS NULL`.
12. Установить `COMPLETED`.
13. При ошибке установить `FAILED`.

Сетевые запросы и расчет дней не объединяются одной длинной транзакцией.

## 22. Executor

```java
@Configuration
public class AsyncConfig {

    @Bean("financeSyncExecutor")
    public Executor financeSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("finance-sync-");
        executor.initialize();
        return executor;
    }
}
```

После перезапуска задачи со статусом `RUNNING` переводятся в `FAILED_INTERRUPTED` или повторно ставятся в очередь после проверки.

## 23. REST API

### 23.1 Аккаунт

```text
GET  /api/v1/account
PUT  /api/v1/account/profile
PUT  /api/v1/account/password
PUT  /api/v1/account/settings
PUT  /api/v1/account/wb-token
DELETE /api/v1/account/wb-token
```

### 23.2 Себестоимость

```text
GET    /api/v1/costs
POST   /api/v1/costs
PUT    /api/v1/costs/{id}
DELETE /api/v1/costs/{id}
GET    /api/v1/costs/{nmId}/history
```

### 23.3 Синхронизация

```text
POST /api/v1/sync-jobs
GET  /api/v1/sync-jobs
GET  /api/v1/sync-jobs/{id}
```

Запрос:

```json
{
  "dateFrom": "2026-06-01",
  "dateTo": "2026-06-30"
}
```

### 23.4 Покрытие

```text
GET /api/v1/finance/coverage?month=2026-06
GET /api/v1/finance/coverage/check?dateFrom=...&dateTo=...
```

### 23.5 Аналитика

```text
GET /api/v1/finance/report/summary
GET /api/v1/finance/report/dynamics
GET /api/v1/finance/report/cost-structure
GET /api/v1/finance/report/top-products
GET /api/v1/finance/report/loss-products
GET /api/v1/finance/report/export
```

Общие параметры:

```text
dateFrom
dateTo
```

## 24. DTO аналитики

```java
public record FinanceSummaryResponse(
    BigDecimal netRevenue,
    BigDecimal wbExpenses,
    BigDecimal costAmount,
    BigDecimal totalProfit,
    BigDecimal marginPercent,
    long productsWithoutCost
) {}
```

```java
public record FinanceDynamicsPoint(
    LocalDate date,
    BigDecimal netRevenue,
    BigDecimal productProfit,
    BigDecimal totalProfit
) {}
```

```java
public record CostStructureItem(
    String code,
    String label,
    BigDecimal amount,
    BigDecimal sharePercent
) {}
```

```java
public record ProductRankingItem(
    Long nmId,
    String productName,
    BigDecimal netRevenue,
    BigDecimal profit,
    BigDecimal marginPercent
) {}
```

## 25. SQL для аналитики

### 25.1 Сводка

```sql
SELECT
    COALESCE(SUM(net_revenue_amount), 0) AS net_revenue,
    COALESCE(SUM(commission_amount + logistics_amount), 0) AS product_wb_expenses,
    COALESCE(SUM(cost_amount), 0) AS cost_amount,
    COALESCE(SUM(product_profit_amount), 0) AS product_profit,
    COALESCE(SUM(acquiring_amount + storage_amount + acceptance_amount
        + penalty_amount + additional_deductions_amount), 0) AS common_expenses
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo;
```

### 25.2 Динамика

```sql
SELECT
    business_date,
    SUM(net_revenue_amount) AS net_revenue,
    SUM(product_profit_amount) AS product_profit,
    SUM(product_profit_amount)
      - SUM(acquiring_amount + storage_amount + acceptance_amount
          + penalty_amount + additional_deductions_amount) AS total_profit
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo
GROUP BY business_date
ORDER BY business_date;
```

### 25.3 Топ товаров

```sql
SELECT
    nm_id,
    MAX(product_name) AS product_name,
    SUM(net_revenue_amount) AS net_revenue,
    SUM(product_profit_amount) AS profit
FROM daily_finance_entries
WHERE user_id = :userId
  AND nm_id IS NOT NULL
  AND business_date BETWEEN :dateFrom AND :dateTo
GROUP BY nm_id
ORDER BY profit DESC
LIMIT 5;
```

### 25.4 Убыточные товары

Тот же запрос с условием `HAVING SUM(product_profit_amount) < 0` и сортировкой по возрастанию прибыли.

## 26. Структура затрат

Категории:

- комиссия маркетплейса;
- логистика;
- эквайринг;
- хранение;
- приемка;
- штрафы;
- дополнительные удержания;
- себестоимость;
- налог.

`Прочие расходы` отсутствуют.

Процент:

```text
category_share = category_amount / total_costs × 100
```

Категории с нулевой суммой не передаются в диаграмму.

## 27. Thymeleaf-интерфейс

### 27.1 Структура ресурсов

```text
src/main/resources
├── templates
│   ├── layout/main.html
│   ├── auth/login.html
│   ├── account/index.html
│   ├── costs/index.html
│   └── finance/index.html
└── static
    ├── css/app.css
    └── js
        ├── finance-page.js
        ├── coverage-calendar.js
        └── finance-charts.js
```

### 27.2 Страница финансов

Порядок блоков:

1. загрузка данных;
2. календарь покрытия;
3. период отчета;
4. кнопка скачивания Excel;
5. карточки;
6. линейный график;
7. круговая диаграмма и денежные значения;
8. топ-5 товаров;
9. убыточные товары.

### 27.3 Круговая диаграмма

Слева Chart.js `doughnut`, справа собственный HTML-список:

```text
Комиссия            81 000 ₽   22,2%
Логистика           29 000 ₽    7,9%
Себестоимость      214 000 ₽   58,6%
```

## 28. Excel-отчет

Кнопка располагается перед аналитическим блоком.

### 28.1 Лист `Финансовый отчет`

Колонки:

1. Артикул WB;
2. Наименование;
3. Продано;
4. Возвращено;
5. Чистое количество;
6. Продажи;
7. Возвраты;
8. Чистая выручка;
9. Комиссия;
10. Логистика;
11. Себестоимость;
12. Налог;
13. Прибыль;
14. Маржинальность;
15. Наличие себестоимости.

После товарных строк:

- эквайринг;
- хранение;
- приемка;
- штрафы;
- дополнительные удержания;
- итоговая прибыль.

### 28.2 Лист `Динамика по дням`

- дата;
- чистая выручка;
- комиссия;
- логистика;
- себестоимость;
- налог;
- прибыль товаров;
- общие расходы;
- итоговая прибыль.

### 28.3 Лист `ABC-анализ`

- артикул;
- наименование;
- чистая выручка;
- доля в общей выручке;
- накопленная доля;
- класс.

Границы:

- A до 80%;
- B от 80% до 95%;
- C оставшиеся позиции.

ABC считается по чистой выручке.

### 28.4 Оформление

- формат `#,##0.00 ₽`;
- проценты `0.00%`;
- закрепление первой строки;
- автофильтр;
- полужирная строка итогов;
- автоширина с верхним ограничением;
- пустая себестоимость помечается текстом `не задана`;
- имя файла `finance-report-YYYY-MM-DD_YYYY-MM-DD.xlsx`.

## 29. Безопасность

Используется сессионная аутентификация.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .build();
    }
}
```

## 30. Ошибки

Единый формат REST-ошибки:

```json
{
  "code": "INCOMPLETE_COVERAGE",
  "message": "Выбранный период покрыт не полностью",
  "timestamp": "2026-06-19T12:00:00Z",
  "path": "/api/v1/finance/report/summary"
}
```

Основные коды:

```text
WB_TOKEN_MISSING
WB_UNAUTHORIZED
WB_RATE_LIMIT
WB_UNAVAILABLE
SYNC_ALREADY_RUNNING
INCOMPLETE_COVERAGE
INVALID_PERIOD
COST_NOT_FOUND
UNRECOGNIZED_OPERATION
EXPORT_FAILED
```

## 31. Тестирование

### 31.1 Unit

- выбор бизнес-даты;
- хэширование;
- классификация продажи;
- классификация возврата;
- расчет выручки;
- равное распределение пула;
- остаток при делении;
- распределение внутри `srid`;
- перенос пула на уровень дня;
- запись в дополнительные удержания при отсутствии товаров;
- выбор себестоимости на дату;
- расчет налога;
- расчет прибыли;
- нулевая выручка и `null` маржинальность;
- ABC-классификация.

### 31.2 Integration с Testcontainers

- Flyway на пустой PostgreSQL;
- уникальность RAW-хэша;
- частичные уникальные индексы дневного слоя;
- пересчет дня;
- повторный пересчет без дублей;
- обновление покрытия;
- агрегирующие SQL-запросы;
- изоляция пользователей.

### 31.3 API

- запуск загрузки;
- запрет пересекающейся загрузки;
- проверка покрытия;
- скачивание Excel;
- отказ при неполном покрытии;
- доступ только к собственным данным.

## 32. Docker и Docker Compose

Docker используется как обязательный способ воспроизводимого запуска приложения. Контейнеризация не меняет предметную архитектуру и не добавляет новые модули. В Docker запускаются только два сервиса:

1. Spring Boot-приложение;
2. PostgreSQL.

Kafka, Redis, RabbitMQ и другие инфраструктурные сервисы не добавляются.

### 32.1 Цели контейнеризации

Docker должен обеспечить:

- одинаковую среду запуска на локальном компьютере и при демонстрации проекта;
- запуск PostgreSQL без ручной установки;
- автоматическое применение Flyway-миграций при старте приложения;
- передачу секретов и параметров подключения через переменные окружения;
- сохранение данных PostgreSQL между перезапусками;
- запуск системы одной командой;
- возможность собирать контейнер приложения в GitHub Actions.

### 32.2 Файлы контейнеризации

В корне репозитория должны находиться:

```text
Dockerfile
compose.yaml
.dockerignore
.env.example
```

Файл `.env` с реальными значениями не добавляется в Git.

### 32.3 Dockerfile

Используется многоэтапная сборка. На первом этапе Maven компилирует приложение и создаёт JAR. На втором этапе запускается только готовый JAR на JRE 21.

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /application

COPY --from=build /workspace/target/*.jar application.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
```

Тесты не выполняются внутри Dockerfile. Они запускаются отдельно командой `mvn verify` и в GitHub Actions. Это ускоряет повторную сборку контейнера и разделяет проверку кода и упаковку приложения.

### 32.4 `.dockerignore`

```text
.git
.github
.idea
.vscode
target
*.iml
.env
README.local.md
```

Каталог `target` исключается, поскольку JAR собирается внутри контейнера.

### 32.5 `compose.yaml`

```yaml
services:
  postgres:
    image: postgres:16
    container_name: marketplace-finance-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-marketplace_finance}
      POSTGRES_USER: ${POSTGRES_USER:-marketplace}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-marketplace}
    ports:
      - "5432:5432"
    healthcheck:
      test:
        [
          "CMD-SHELL",
          "pg_isready -U ${POSTGRES_USER:-marketplace} -d ${POSTGRES_DB:-marketplace_finance}"
        ]
      interval: 5s
      timeout: 3s
      retries: 10
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: marketplace-finance-app
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-marketplace_finance}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-marketplace}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-marketplace}
      WB_TOKEN_ENCRYPTION_KEY: ${WB_TOKEN_ENCRYPTION_KEY}
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

### 32.6 Переменные окружения

Файл `.env.example`:

```dotenv
POSTGRES_DB=marketplace_finance
POSTGRES_USER=marketplace
POSTGRES_PASSWORD=change_me
WB_TOKEN_ENCRYPTION_KEY=replace_with_32_byte_secret
```

Реальный `.env` создаётся локально на основе `.env.example`.

API-токен Wildberries не передаётся как переменная окружения контейнера. Он сохраняется пользователем через страницу аккаунта и хранится в базе в зашифрованном виде. Переменная `WB_TOKEN_ENCRYPTION_KEY` содержит только ключ шифрования.

### 32.7 Spring-профиль `docker`

Файл `src/main/resources/application-docker.yml`:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

server:
  port: 8080
```

`ddl-auto` устанавливается в `validate`. Создание и изменение схемы выполняется только Flyway-миграциями.

### 32.8 Сценарии запуска

Полный запуск приложения и базы:

```bash
docker compose up --build
```

Запуск в фоновом режиме:

```bash
docker compose up --build -d
```

Остановка контейнеров без удаления данных:

```bash
docker compose down
```

Остановка с удалением тома PostgreSQL:

```bash
docker compose down -v
```

Просмотр логов приложения:

```bash
docker compose logs -f app
```

Для разработки из IDE разрешается запускать только PostgreSQL:

```bash
docker compose up postgres
```

В этом режиме Spring Boot запускается локально с профилем `local` и подключается к `localhost:5432`.

### 32.9 Проверка готовности

После запуска должны выполняться проверки:

- `http://localhost:8080/login` открывает страницу входа;
- приложение успешно подключается к PostgreSQL;
- Flyway применяет все миграции;
- после перезапуска контейнеров данные сохраняются;
- удаление тома создаёт чистую базу при следующем запуске;
- секреты не попадают в логи;
- в Git отсутствует файл `.env`.

### 32.10 Ограничения Docker-конфигурации

Для дипломной и портфельной версии не настраиваются:

- Docker Swarm;
- Kubernetes;
- отдельный reverse proxy;
- балансировка между несколькими экземплярами приложения;
- внешнее файловое хранилище;
- Kafka и другие брокеры сообщений.

Контейнеризация используется только для воспроизводимого запуска Spring Boot и PostgreSQL.

## 33. GitHub Actions

Pipeline:

1. checkout;
2. setup Java 21;
3. `mvn verify`;
4. unit tests;
5. integration tests Testcontainers;
6. package;
7. обязательная сборка Docker-образа.

## 34. План реализации на 14 дней

### День 1

- Spring Initializr;
- Maven;
- структура пакетов;
- Dockerfile и Docker Compose;
- PostgreSQL в контейнере;
- `.env.example` и профиль `docker`;
- базовый CI.

### День 2

- Flyway;
- users;
- settings;
- credentials;
- costs.

### День 3

- Spring Security;
- вход;
- регистрация;
- аккаунт.

### День 4

- себестоимость;
- история;
- тесты выбора стоимости.

### День 5

- sync jobs;
- WB client;
- обработка HTTP-ошибок.

### День 6

- RAW entity;
- хэширование;
- массовая вставка;
- дедупликация.

### День 7

- классификация;
- revenue resolver;
- бизнес-дата.

### День 8

- дневной агрегатор;
- группировка `srid + date`;
- равное распределение логистики.

### День 9

- себестоимость;
- налог;
- прибыль;
- общая строка дня.

### День 10

- coverage;
- полный сценарий синхронизации;
- executor.

### День 11

- SQL аналитики;
- REST DTO;
- Swagger.

### День 12

- Thymeleaf;
- календарь;
- Chart.js;
- аналитический блок.

### День 13

- Apache POI;
- Excel;
- ABC.

### День 14

- Testcontainers;
- исправления;
- README;
- демонстрационные данные;
- финальная проверка.

## 35. Критерии готовности

Проект считается готовым, когда:

- приложение запускается одной командой Docker Compose;
- пользователь может зарегистрироваться и войти;
- токен WB сохраняется безопасно;
- себестоимость задается с датой действия;
- загрузка API создает RAW-операции;
- повторная загрузка не создает дублей;
- дневной слой пересчитывается только по затронутым датам;
- логистический пул распределяется равными долями;
- календарь показывает покрытие;
- неполный период не допускается к отчету;
- Excel скачивается;
- аналитический блок строится;
- структура затрат содержит денежные значения;
- ABC присутствует в Excel;
- тесты проходят в GitHub Actions.

## 36. Порядок работы с ИИ

Рекомендуемый порядок запросов:

1. Создать только Flyway-миграции по разделу 7.
2. Создать Entity без бизнес-логики.
3. Создать repositories.
4. Создать WB DTO и client.
5. Создать `BusinessDateResolver` и тесты.
6. Создать `OperationHashCalculator` и тесты.
7. Создать `OperationClassifier` и тесты.
8. Создать `EqualMoneyAllocator` и тесты.
9. Создать `RecalculateFinanceDayService`.
10. Создать интеграционный тест пересчета дня.
11. Создать sync orchestration.
12. Создать аналитические query services.
13. Создать REST-контроллеры.
14. Создать Thymeleaf-страницы.
15. Создать Excel-exporter.
16. Провести рефакторинг только после прохождения тестов.

Каждый запрос должен требовать полный код класса, тесты и краткое объяснение границ ответственности. Не следует просить ИИ сразу сгенерировать весь проект.

# Часть II. Спецификация, непосредственно переносимая в код

## 37. Окончательно зафиксированные решения

Этот раздел имеет приоритет над более ранними вариантами документа.

1. Финансовых таблиц ровно две: `financial_operations_raw` и `daily_finance_entries`.
2. `financial_operations_raw` хранит исходные строки API Wildberries и не содержит результатов бизнес-расчётов, кроме вычисленной бизнес-даты, технического хэша и статуса классификации.
3. `daily_finance_entries` содержит одну строку на товар за день и не более одной общей строки без товара за день.
4. Отдельная таблица итогов по импорту, месячная проекция и проекция под выбранный период не создаются.
5. Пользовательский отчёт за период строится суммированием дневных строк.
6. Распределяются только логистический пул и относимое к нему вознаграждение ПВЗ.
7. Распределение выполняется равными долями между уникальными товарами, а не пропорционально выручке.
8. Сначала пул распределяется внутри группы `business_date + srid`.
9. Если в группе товаров нет, пул переносится на уровень дня и поровну распределяется между уникальными товарами дня.
10. Если в дне нет товаров, нераспределённый пул добавляется в `additional_deductions_amount` общей строки дня.
11. Эквайринг, хранение, приёмка, штрафы и прямые удержания не распределяются по товарам.
12. Универсальная категория `Прочие расходы` не используется.
13. В интерфейсе сохраняются загрузка API, календарь покрытия, период отчёта и кнопка скачивания Excel. Краткая таблица заменяется аналитическим блоком.
14. Кнопка Excel располагается перед аналитическим блоком.
15. Структура затрат показывается круговой диаграммой. Справа выводятся название категории, сумма в рублях и доля в общей сумме затрат.
16. Подробная аналитика по товарам и ABC-анализ находятся в Excel.
17. Модуль автоакций и входной Excel-импорт удаляются.
18. Frontend реализуется на Thymeleaf, Bootstrap, Chart.js и обычном JavaScript.
19. Аутентификация сессионная через Spring Security. JWT не используется.
20. Kafka и другие брокеры не используются. Docker и Docker Compose являются единственным дополнительным инфраструктурным слоем; Flyway, Testcontainers, Swagger и GitHub Actions остаются частью проекта.

## 38. Состав Maven-проекта

### 38.1 Координаты проекта

```text
groupId: ru.marketplace
artifactId: finance-analytics
name: Marketplace Finance Analytics
package: ru.marketplace.finance
java: 21
build: Maven
packaging: jar
```

### 38.2 Обязательные зависимости `pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
    </dependency>

    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

MapStruct можно исключить, если преобразования DTO выполняются вручную. Lombok не должен скрывать бизнес-логику. Для JPA Entity рекомендуется использовать явный защищённый конструктор без аргументов и осмысленные методы изменения состояния.

## 39. Файловая структура проекта

```text
src/main/java/ru/marketplace/finance
├── MarketplaceFinanceApplication.java
├── account
│   ├── api
│   │   ├── AccountPageController.java
│   │   ├── AccountRestController.java
│   │   └── dto
│   ├── application
│   │   ├── AccountService.java
│   │   └── CredentialService.java
│   ├── domain
│   │   ├── User.java
│   │   └── MarketplaceCredential.java
│   └── infrastructure
│       ├── UserRepository.java
│       └── MarketplaceCredentialRepository.java
├── cost
│   ├── api
│   ├── application
│   │   ├── ProductCostService.java
│   │   └── CostResolver.java
│   ├── domain
│   │   └── ProductCost.java
│   └── infrastructure
│       └── ProductCostRepository.java
├── synchronization
│   ├── api
│   │   ├── SyncPageController.java
│   │   ├── SyncRestController.java
│   │   └── dto
│   ├── application
│   │   ├── StartSyncUseCase.java
│   │   ├── SyncOrchestrator.java
│   │   └── StaleSyncRecoveryService.java
│   ├── domain
│   │   ├── SyncJob.java
│   │   └── SyncStatus.java
│   └── infrastructure
│       └── SyncJobRepository.java
├── finance
│   ├── api
│   │   ├── FinancePageController.java
│   │   ├── FinanceReportRestController.java
│   │   └── dto
│   ├── application
│   │   ├── RawOperationImportService.java
│   │   ├── DailyFinanceRecalculationService.java
│   │   ├── FinanceReportQueryService.java
│   │   └── FinanceExportService.java
│   ├── domain
│   │   ├── RawFinancialOperation.java
│   │   ├── DailyFinanceEntry.java
│   │   ├── OperationClassifier.java
│   │   ├── BusinessDateResolver.java
│   │   ├── RowHashFactory.java
│   │   ├── LogisticsPoolAllocator.java
│   │   ├── CommissionCalculator.java
│   │   ├── TaxCalculator.java
│   │   ├── ProfitCalculator.java
│   │   ├── OperationKind.java
│   │   └── ClassificationStatus.java
│   └── infrastructure
│       ├── persistence
│       │   ├── RawFinancialOperationRepository.java
│       │   ├── DailyFinanceEntryRepository.java
│       │   └── FinanceJdbcQueryRepository.java
│       └── wb
│           ├── WildberriesFinanceClient.java
│           ├── WildberriesFinanceClientImpl.java
│           ├── WbFinanceRowDto.java
│           └── WbClientProperties.java
├── export
│   ├── application
│   │   └── FinanceWorkbookFactory.java
│   └── infrastructure
│       └── ApachePoiFinanceWorkbookFactory.java
└── common
    ├── config
    │   ├── AsyncConfig.java
    │   ├── SecurityConfig.java
    │   ├── OpenApiConfig.java
    │   └── RestClientConfig.java
    ├── exception
    ├── security
    └── web

src/main/resources
├── db/migration
├── templates
│   ├── fragments
│   ├── auth
│   ├── account
│   ├── costs
│   ├── finance
│   └── sync
├── static
│   ├── css
│   └── js
├── application.yml
└── application-local.yml
```

## 40. Полный порядок Flyway-миграций

Миграции создаются один раз в следующем порядке.

```text
V1__create_users.sql
V2__create_marketplace_credentials.sql
V3__create_product_costs.sql
V4__create_sync_jobs.sql
V5__create_financial_operations_raw.sql
V6__create_daily_finance_entries.sql
```

Индексы создаются в тех же миграциях, что и соответствующие таблицы. Отдельная миграция `create_indexes` не используется.

### 40.1 Правила миграций

- Миграции не содержат проверок `IF EXISTS` для маскировки несовместимой истории.
- Каждая миграция должна успешно выполняться на пустой PostgreSQL.
- После принятия миграции её содержимое не изменяется. Исправление выполняется новой миграцией.
- В CI обязательно выполняется `migrate` на новом контейнере PostgreSQL.
- Типы денежных полей: `NUMERIC(19,2)`.
- Проценты: `NUMERIC(7,4)`.
- Момент времени: `TIMESTAMPTZ`.
- Дата финансового дня: `DATE`.
- Исходный ответ API: `JSONB`.

### 40.2 Полная схема `financial_operations_raw`

```sql
CREATE TABLE financial_operations_raw (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL REFERENCES users(id),
    sync_job_id                 BIGINT NOT NULL REFERENCES sync_jobs(id),

    business_date               DATE NOT NULL,
    rr_dt                       TIMESTAMPTZ,
    sale_dt                     TIMESTAMPTZ,
    order_dt                    TIMESTAMPTZ,
    create_dt                   TIMESTAMPTZ,

    nm_id                       BIGINT,
    srid                        VARCHAR(255),
    supplier_oper_name          VARCHAR(255),
    doc_type_name               VARCHAR(255),

    quantity                    INTEGER,
    retail_amount               NUMERIC(19,2),
    retail_price_withdisc_rub   NUMERIC(19,2),
    ppvz_for_pay                NUMERIC(19,2),
    ppvz_reward                 NUMERIC(19,2),
    acquiring_fee               NUMERIC(19,2),
    delivery_rub                NUMERIC(19,2),
    rebill_logistic_cost        NUMERIC(19,2),
    storage_fee                 NUMERIC(19,2),
    penalty                     NUMERIC(19,2),
    deduction                   NUMERIC(19,2),
    acceptance                  NUMERIC(19,2),
    additional_payment          NUMERIC(19,2),

    operation_kind              VARCHAR(40) NOT NULL,
    classification_status       VARCHAR(40) NOT NULL,
    row_hash                    VARCHAR(64) NOT NULL,
    raw_payload                 JSONB NOT NULL,

    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_raw_user_hash UNIQUE (user_id, row_hash),
    CONSTRAINT chk_raw_nm_id CHECK (nm_id IS NULL OR nm_id > 0)
);
```

### 40.3 Полная схема `daily_finance_entries`

```sql
CREATE TABLE daily_finance_entries (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL REFERENCES users(id),
    business_date                   DATE NOT NULL,
    nm_id                           BIGINT,
    product_name                    VARCHAR(255),

    sales_quantity                  INTEGER NOT NULL DEFAULT 0,
    return_quantity                 INTEGER NOT NULL DEFAULT 0,
    net_quantity                    INTEGER NOT NULL DEFAULT 0,

    sales_amount                    NUMERIC(19,2) NOT NULL DEFAULT 0,
    return_amount                   NUMERIC(19,2) NOT NULL DEFAULT 0,
    net_revenue_amount              NUMERIC(19,2) NOT NULL DEFAULT 0,

    commission_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    logistics_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    cost_amount                     NUMERIC(19,2) NOT NULL DEFAULT 0,
    tax_amount                      NUMERIC(19,2) NOT NULL DEFAULT 0,
    product_profit_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,

    acquiring_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    storage_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    acceptance_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    penalty_amount                  NUMERIC(19,2) NOT NULL DEFAULT 0,
    additional_deductions_amount    NUMERIC(19,2) NOT NULL DEFAULT 0,

    has_cost                        BOOLEAN NOT NULL DEFAULT TRUE,
    calculation_version             INTEGER NOT NULL,
    calculated_at                   TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_daily_product_or_summary CHECK (
        (nm_id IS NOT NULL AND nm_id > 0)
        OR nm_id IS NULL
    )
);

CREATE UNIQUE INDEX uq_daily_product
    ON daily_finance_entries(user_id, business_date, nm_id)
    WHERE nm_id IS NOT NULL;

CREATE UNIQUE INDEX uq_daily_summary
    ON daily_finance_entries(user_id, business_date)
    WHERE nm_id IS NULL;
```

### 40.4 Значение общей строки дня

Строка `nm_id IS NULL` содержит только:

- `acquiring_amount`;
- `storage_amount`;
- `acceptance_amount`;
- `penalty_amount`;
- `additional_deductions_amount`.

Товарные поля общей строки равны нулю. В товарных строках общие поля равны нулю.

## 41. Контракты JPA Entity

### 41.1 Общие требования

- Entity не возвращаются из REST.
- Для денежных значений используется `BigDecimal`.
- Для даты используется `LocalDate`.
- Для времени используется `OffsetDateTime` или `Instant`.
- `equals()` и `hashCode()` не строятся по изменяемым полям.
- Коллекции связей не загружаются без необходимости.
- `FetchType.EAGER` не применяется к пользовательским связям.
- Setter для каждого поля не генерируется. Изменение выполняется через методы предметной области.

### 41.2 `RawFinancialOperation`

Обязательные методы:

```java
public static RawFinancialOperation create(
    Long userId,
    Long syncJobId,
    WbFinanceRowDto source,
    LocalDate businessDate,
    OperationKind operationKind,
    ClassificationStatus classificationStatus,
    String rowHash,
    String rawPayload
);

public boolean belongsToProduct();
public boolean hasSrid();
public BigDecimal logisticsCore();
public BigDecimal pvzRewardAbsolute();
```

Entity не вычисляет комиссию, налог, прибыль и распределение. Эти операции находятся в domain services.

### 41.3 `DailyFinanceEntry`

Фабрики:

```java
public static DailyFinanceEntry product(
    Long userId,
    LocalDate date,
    Long nmId,
    String productName,
    int calculationVersion
);

public static DailyFinanceEntry daySummary(
    Long userId,
    LocalDate date,
    int calculationVersion
);
```

Методы товарной строки:

```java
public void addSale(int quantity, BigDecimal amount);
public void addReturn(int quantity, BigDecimal amount);
public void addCommission(BigDecimal amount);
public void addLogistics(BigDecimal amount);
public void applyCost(BigDecimal amount, boolean hasCost);
public void applyTax(BigDecimal amount);
public void applyProfit(BigDecimal amount);
public void recalculateDerivedValues();
```

Методы общей строки:

```java
public void addAcquiring(BigDecimal amount);
public void addStorage(BigDecimal amount);
public void addAcceptance(BigDecimal amount);
public void addPenalty(BigDecimal amount);
public void addAdditionalDeduction(BigDecimal amount);
```

Метод не должен позволять добавить хранение в товарную строку или продажу в общую строку. При нарушении выбрасывается `IllegalStateException`.

## 42. Точные enum-классы

```java
public enum OperationKind {
    SALE,
    RETURN,
    LOGISTICS,
    STORAGE,
    ACQUIRING,
    ACCEPTANCE,
    PENALTY,
    DEDUCTION,
    COMPENSATION,
    UNKNOWN
}
```

```java
public enum ClassificationStatus {
    CLASSIFIED,
    PARTIALLY_CLASSIFIED,
    UNRECOGNIZED
}
```

```java
public enum SyncStatus {
    CREATED,
    RUNNING,
    RAW_SAVED,
    DAILY_RECALCULATED,
    COMPLETED,
    FAILED
}
```

```java
public enum UserRole {
    USER,
    ADMIN
}
```

Роль `ADMIN` можно оставить в схеме, но административный интерфейс не входит в обязательный MVP.

## 43. Классификация операций

### 43.1 Правила, перенесённые из текущего PHP-кода

Текущий код распознаёт продажи по следующим нормализованным значениям:

```text
продажа
корректная продажа
сторно возвратов
частичная компенсация брака
компенсация подмененного товара
оплата брака
оплата потерянного товара
авансовая оплата за товар без движения
коррекция продаж
```

Возвраты:

```text
возврат
корректный возврат
сторно продаж
```

Дополнительно `doc_type_name`, содержащий `возврат`, трактуется как возврат, а содержащий `продаж`, как продажа.

`компенсация ущерба` в текущем расчёте комиссии даёт нулевую комиссию.

Логистический контур определяется, если `supplier_oper_name` содержит:

```text
логист
перевоз
пвз
склад
```

Хранение определяется точным нормализованным значением `хранение` либо ненулевым `storage_fee`.

### 43.2 Реализация классификатора

```java
public final class OperationClassifier {

    private static final Set<String> SALE_NAMES = Set.of(...);
    private static final Set<String> RETURN_NAMES = Set.of(...);

    public ClassificationResult classify(WbFinanceRowDto row) {
        String operation = normalize(row.supplierOperName());
        String document = normalize(row.docTypeName());

        if (RETURN_NAMES.contains(operation) || document.contains("возврат")) {
            return classified(OperationKind.RETURN);
        }
        if (SALE_NAMES.contains(operation) || document.contains("продаж")) {
            return classified(OperationKind.SALE);
        }
        if (operation.equals("хранение") || nonZero(row.storageFee())) {
            return classified(OperationKind.STORAGE);
        }
        if (containsAny(operation, "логист", "перевоз", "пвз", "склад")) {
            return classified(OperationKind.LOGISTICS);
        }
        if (nonZero(row.acquiringFee())) {
            return classified(OperationKind.ACQUIRING);
        }
        if (nonZero(row.acceptance())) {
            return classified(OperationKind.ACCEPTANCE);
        }
        if (nonZero(row.penalty())) {
            return classified(OperationKind.PENALTY);
        }
        if (nonZero(row.deduction()) || nonZero(row.additionalPayment())) {
            return classified(OperationKind.DEDUCTION);
        }
        return unrecognized();
    }
}
```

Классификатор обязан сохранять исходную строку даже при `UNRECOGNIZED`.

### 43.3 Таблица классификации как тестовый ресурс

Следует создать файл:

```text
src/test/resources/wb-operation-classification.csv
```

Колонки:

```text
supplier_oper_name,doc_type_name,expected_kind
```

Каждая обнаруженная на реальных данных новая операция добавляется в этот файл и покрывается параметризованным JUnit-тестом.

## 44. Бизнес-дата

Порядок выбора даты сохраняется из текущего кода:

```text
rr_dt → sale_dt → order_dt → create_dt
```

```java
public LocalDate resolve(WbFinanceRowDto row) {
    return Stream.of(row.rrDt(), row.saleDt(), row.orderDt(), row.createDt())
        .filter(Objects::nonNull)
        .map(value -> value.atZoneSameInstant(REPORT_ZONE).toLocalDate())
        .findFirst()
        .orElseThrow(() -> new MissingBusinessDateException(...));
}
```

Часовой пояс задаётся конфигурацией. Рекомендуемое значение `Europe/Moscow`, если API и отчёт пользователя интерпретируются по московскому времени. При отсутствии подтверждения часового пояса в исходных данных он должен оставаться конфигурационным параметром.

## 45. Хэш операции

### 45.1 Назначение

`row_hash` обеспечивает повторяемость загрузки одного периода и исключает дублирование одинаковой исходной строки.

### 45.2 Состав строки для хэша

```text
business_date
rr_dt
sale_dt
order_dt
nm_id
srid
supplier_oper_name
doc_type_name
quantity
retail_amount
retail_price_withdisc_rub
ppvz_for_pay
ppvz_reward
acquiring_fee
delivery_rub
rebill_logistic_cost
storage_fee
penalty
deduction
acceptance
additional_payment
```

Каждое значение нормализуется:

- `null` превращается в пустую строку;
- текст обрезается по краям;
- дата переводится в ISO-8601;
- `BigDecimal` приводится через `stripTrailingZeros().toPlainString()`;
- поля объединяются символом `|`;
- используется SHA-256.

`raw_payload` в хэш не включается.

## 46. Расчёт товарных показателей

### 46.1 Продажи

Для операции продажи:

```text
sales_quantity += abs(quantity)
sales_amount += resolveRevenue(row)
```

`resolveRevenue` повторяет текущий приоритет:

```text
retail_amount, если > 0
иначе retail_price_withdisc_rub, если > 0
иначе 0
```

### 46.2 Возвраты

Для операции возврата:

```text
return_quantity += abs(quantity)
return_amount += resolveRevenue(row)
```

Производные показатели:

```text
net_quantity = sales_quantity - return_quantity
net_revenue_amount = sales_amount - return_amount
```

Возвраты хранятся положительной абсолютной суммой в `return_amount`, а вычитаются только при расчёте `net_revenue_amount`.

### 46.3 Комиссия

Текущая формула строки:

```text
commission = abs(retail) - abs(ppvz_for_pay) - abs(acquiring_fee)
```

Для продажи результат добавляется со знаком плюс. Для возврата вычитается.

```java
BigDecimal base = abs(retail).subtract(abs(forPay)).subtract(abs(acquiring));
BigDecimal commission = base.max(ZERO);
return isReturn ? commission.negate() : commission;
```

Эквайринг не входит в итоговую комиссию и дополнительно накапливается в общей строке дня.

### 46.4 Прямая логистика

```text
logistics_core = abs(delivery_rub) + abs(rebill_logistic_cost)
```

Вознаграждение ПВЗ добавляется в логистический пул только для операций, классифицированных как относящиеся к логистическому контуру. Бездумное `abs(ppvz_reward)` для любой строки запрещено.

## 47. Двухуровневое равное распределение логистического пула

### 47.1 Группа

Ключ группы:

```java
record LogisticsGroupKey(LocalDate businessDate, String srid) {}
```

Для пустого `srid` нельзя объединять все строки дня в одну группу. Каждая строка без `srid` получает технический уникальный ключ `NO_SRID:<rawOperationId>`. После этого её пул может перейти на дневной уровень.

### 47.2 Уникальные товары группы

```java
Set<Long> productIds = group.operations().stream()
    .map(RawFinancialOperation::getNmId)
    .filter(Objects::nonNull)
    .filter(id -> id > 0)
    .collect(toCollection(TreeSet::new));
```

Сортировка по `nm_id` обеспечивает стабильное распределение остатка округления.

### 47.3 Пул группы

```text
group_pool = Σ logistics_core + Σ applicable_pvz_reward
```

### 47.4 Распределение

Если `N > 0`:

```text
base_share = group_pool / N с точностью 2 знака, DOWN
remainder = group_pool - base_share × N
```

Первые `N - 1` товаров получают `base_share`. Последний получает `base_share + remainder`.

Если товаров нет, `group_pool` прибавляется к `unassignedDayPool`.

### 47.5 Дневной уровень

После обработки всех групп:

- если в дне есть товары, `unassignedDayPool` равными долями распределяется между всеми уникальными товарами дня;
- если товаров нет, `unassignedDayPool` добавляется в `additional_deductions_amount` общей строки дня.

### 47.6 Инварианты распределения

JUnit-тесты обязаны проверять:

```text
сумма распределённых долей = исходный пул
один товар получает весь пул
три товара делят 100 как 33,33 + 33,33 + 33,34
повторный расчёт даёт тот же результат
число исходных строк товара не меняет его долю
```

## 48. Общая строка дня

Следующие поля накапливаются без распределения:

```text
acquiring_amount = Σ abs(acquiring_fee)
storage_amount = Σ abs(storage_fee)
acceptance_amount = Σ abs(acceptance)
penalty_amount = Σ abs(penalty)
additional_deductions_amount =
    Σ abs(deduction)
    + Σ abs(additional_payment)
    + нераспределённый логистический пул дня без товаров
```

До окончательной проверки семантики `additional_payment` на реальных ответах API оно должно сохраняться отдельно в RAW. Во втором слое оно включается в дополнительные удержания только согласно зафиксированному правилу классификатора.

## 49. Себестоимость

### 49.1 Поиск стоимости

Для товара и даты выбирается запись:

```sql
SELECT cost_amount
FROM product_costs
WHERE user_id = :userId
  AND nm_id = :nmId
  AND valid_from <= :businessDate
ORDER BY valid_from DESC
LIMIT 1;
```

### 49.2 Формула

```text
cost_amount = net_quantity × cost_per_unit
```

Если `net_quantity` отрицательное, себестоимость также отрицательная и увеличивает прибыль за счёт возврата стоимости товара.

### 49.3 Отсутствующая стоимость

Зафиксированное безопасное поведение:

- `cost_amount = 0`;
- `has_cost = false`;
- прибыль рассчитывается технически, но интерфейс и Excel помечают результат как неполный;
- карточка количества товаров без себестоимости может выводиться как дополнительный индикатор;
- отсутствующая стоимость не должна молча считаться корректным нулём.

### 49.4 Изменение стоимости задним числом

После создания или изменения записи `product_costs` пересчитываются дневные строки данного товара:

```text
от valid_from включительно
до даты следующего изменения стоимости не включительно
или до текущей последней покрытой даты
```

## 50. Налог, прибыль и маржинальность

### 50.1 Налоговая база

Если сохраняется текущая упрощённая формула, налог считается от положительной чистой выручки:

```text
tax_amount = max(net_revenue_amount, 0) × tax_percent / 100
```

Это бизнес-допущение проекта, а не универсальная налоговая модель. Оно должно быть указано в README и интерфейсе настроек.

### 50.2 Прибыль товара

```text
product_profit_amount =
    net_revenue_amount
    - commission_amount
    - logistics_amount
    - cost_amount
    - tax_amount
```

### 50.3 Общие затраты дня

```text
day_common_expenses =
    acquiring_amount
    + storage_amount
    + acceptance_amount
    + penalty_amount
    + additional_deductions_amount
```

### 50.4 Итоговая прибыль периода

```text
total_profit =
    Σ product_profit_amount
    - Σ day_common_expenses
```

### 50.5 Маржинальность

```text
margin_percent = total_profit / net_revenue × 100
```

При нулевой чистой выручке возвращается `null`, а не `0` и не исключение деления.

## 51. Транзакционный алгоритм пересчёта дня

```java
@Service
@RequiredArgsConstructor
public class DailyFinanceRecalculationService {

    @Transactional
    public void recalculate(long userId, LocalDate businessDate) {
        List<RawFinancialOperation> raw = rawRepository
            .findAllForUpdateByUserIdAndBusinessDate(userId, businessDate);

        DailyCalculationResult result = calculator.calculate(userId, businessDate, raw);

        dailyRepository.deleteByUserIdAndBusinessDate(userId, businessDate);
        dailyRepository.saveAll(result.productEntries());
        dailyRepository.save(result.daySummary());
    }
}
```

### 51.1 Почему день удаляется и создаётся заново

- расчёт идемпотентен;
- исключается накопление повторных долей;
- повторная загрузка исправляет изменившиеся операции;
- не требуется сложная синхронизация каждого поля;
- транзакция короткая и охватывает один день.

### 51.2 Блокировка

Для параллельного пересчёта одной даты используется один из вариантов:

- PostgreSQL advisory lock;
- запрет пересекающихся активных `sync_job` пользователя.

Для MVP достаточно запрета второго активного задания пользователя и последовательного пересчёта дат внутри одного задания.

## 52. Полный алгоритм синхронизации

### 52.1 HTTP-этап

`POST /api/v1/sync-jobs`:

1. Проверяет период.
2. Проверяет отсутствие активного задания пользователя.
3. Создаёт `sync_job` со статусом `CREATED`.
4. Передаёт идентификатор в выделенный executor.
5. Возвращает HTTP 202 и DTO задания.

### 52.2 Фоновый этап

```text
CREATED
→ RUNNING
→ запрос к API по страницам
→ сохранение RAW
→ RAW_SAVED
→ определение affectedDates
→ последовательный пересчёт affectedDates
→ DAILY_RECALCULATED
→ создание общих строк дня для покрытия периода
→ COMPLETED
```

При ошибке:

```text
status = FAILED
error_code = стабильный код
error_message = безопасное сообщение
finished_at = now
```

### 52.3 Внешний API и транзакции

Сетевой запрос выполняется вне транзакции. Каждая пачка RAW сохраняется отдельной короткой транзакцией. Пересчёт каждого дня выполняется отдельной транзакцией.

### 52.4 Retry

Повторяются только временные ошибки:

- HTTP 429;
- HTTP 502;
- HTTP 503;
- HTTP 504;
- timeout соединения.

Не повторяются:

- HTTP 401;
- HTTP 403;
- ошибки валидации ответа;
- неверный период.

Рекомендуемая задержка:

```text
1-я повторная попытка: 1 секунда
2-я: 2 секунды
3-я: 4 секунды
```

При наличии `Retry-After` используется значение сервера.

## 53. REST API и точные DTO

### 53.1 Запуск синхронизации

```http
POST /api/v1/sync-jobs
Content-Type: application/json
```

```json
{
  "dateFrom": "2026-06-01",
  "dateTo": "2026-06-30"
}
```

Ответ 202:

```json
{
  "id": 42,
  "status": "CREATED",
  "dateFrom": "2026-06-01",
  "dateTo": "2026-06-30",
  "createdAt": "2026-06-19T11:00:00Z"
}
```

### 53.2 Проверка покрытия

```http
GET /api/v1/coverage/check?dateFrom=2026-06-01&dateTo=2026-06-30
```

Покрытие вычисляется по `daily_finance_entries`: дата считается покрытой, если для пользователя существует общая строка дня `nm_id IS NULL`. После успешной загрузки периода такая строка создаётся для каждой даты периода, включая дни без операций.

```json
{
  "complete": false,
  "coveredDays": 28,
  "requiredDays": 30,
  "missingDates": ["2026-06-05", "2026-06-17"]
}
```

### 53.3 Сводка

```http
GET /api/v1/finance/report/summary?dateFrom=...&dateTo=...
```

```json
{
  "netRevenue": 538000.00,
  "wildberriesExpenses": 113400.00,
  "costAmount": 214000.00,
  "totalProfit": 164320.00,
  "marginPercent": 30.54,
  "productsWithoutCost": 3
}
```

`wildberriesExpenses` включает комиссию, логистику, эквайринг, хранение, приёмку, штрафы и дополнительные удержания, но не себестоимость и налог.

### 53.4 Динамика

```json
[
  {
    "date": "2026-06-01",
    "netRevenue": 18500.00,
    "productProfit": 6300.00,
    "totalProfit": 5900.00
  }
]
```

### 53.5 Структура затрат

```json
[
  {"code":"COMMISSION","label":"Комиссия маркетплейса","amount":81000.00,"sharePercent":22.20},
  {"code":"LOGISTICS","label":"Логистика","amount":29000.00,"sharePercent":7.95},
  {"code":"COST","label":"Себестоимость","amount":214000.00,"sharePercent":58.63}
]
```

Нулевые категории не возвращаются.

### 53.6 Рейтинг товаров

```http
GET /api/v1/finance/report/top-products?dateFrom=...&dateTo=...&limit=5
GET /api/v1/finance/report/loss-products?dateFrom=...&dateTo=...&limit=5
```

```json
[
  {
    "nmId": 12345678,
    "productName": "Товар",
    "netRevenue": 85000.00,
    "profit": 24000.00,
    "marginPercent": 28.24
  }
]
```

## 54. SQL-запросы отчёта

### 54.1 Сводка

Сводка строится двумя условными агрегатами из одной таблицы. Товарные поля берутся из строк `nm_id IS NOT NULL`, общие расходы из строки `nm_id IS NULL`.

```sql
SELECT
    COALESCE(SUM(net_revenue_amount) FILTER (WHERE nm_id IS NOT NULL), 0) AS net_revenue,
    COALESCE(SUM(commission_amount + logistics_amount)
        FILTER (WHERE nm_id IS NOT NULL), 0)
    + COALESCE(SUM(acquiring_amount + storage_amount + acceptance_amount
        + penalty_amount + additional_deductions_amount)
        FILTER (WHERE nm_id IS NULL), 0) AS wb_expenses,
    COALESCE(SUM(cost_amount) FILTER (WHERE nm_id IS NOT NULL), 0) AS cost_amount,
    COALESCE(SUM(product_profit_amount) FILTER (WHERE nm_id IS NOT NULL), 0)
    - COALESCE(SUM(acquiring_amount + storage_amount + acceptance_amount
        + penalty_amount + additional_deductions_amount)
        FILTER (WHERE nm_id IS NULL), 0) AS total_profit,
    COUNT(DISTINCT nm_id) FILTER (WHERE nm_id IS NOT NULL AND has_cost = false)
        AS products_without_cost
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo;
```

### 54.2 Динамика

```sql
SELECT
    business_date,
    SUM(net_revenue_amount) FILTER (WHERE nm_id IS NOT NULL) AS net_revenue,
    SUM(product_profit_amount) FILTER (WHERE nm_id IS NOT NULL) AS product_profit,
    SUM(product_profit_amount) FILTER (WHERE nm_id IS NOT NULL)
    - SUM(acquiring_amount + storage_amount + acceptance_amount
        + penalty_amount + additional_deductions_amount)
        FILTER (WHERE nm_id IS NULL) AS total_profit
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo
GROUP BY business_date
ORDER BY business_date;
```

Каждая дата покрытого периода должна присутствовать. Если операций нет, сохраняется общая нулевая строка дня `nm_id IS NULL`, чтобы временной ряд не имел разрывов и покрытие периода можно было определить по `daily_finance_entries`.

### 54.3 Структура затрат

Запрос может вернуть одну строку со столбцами, после чего Java формирует список ненулевых категорий.

```sql
SELECT
    SUM(commission_amount) FILTER (WHERE nm_id IS NOT NULL) AS commission,
    SUM(logistics_amount) FILTER (WHERE nm_id IS NOT NULL) AS logistics,
    SUM(cost_amount) FILTER (WHERE nm_id IS NOT NULL) AS cost,
    SUM(tax_amount) FILTER (WHERE nm_id IS NOT NULL) AS tax,
    SUM(acquiring_amount) FILTER (WHERE nm_id IS NULL) AS acquiring,
    SUM(storage_amount) FILTER (WHERE nm_id IS NULL) AS storage,
    SUM(acceptance_amount) FILTER (WHERE nm_id IS NULL) AS acceptance,
    SUM(penalty_amount) FILTER (WHERE nm_id IS NULL) AS penalty,
    SUM(additional_deductions_amount) FILTER (WHERE nm_id IS NULL) AS deductions
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo;
```

### 54.4 Топ товаров

```sql
SELECT
    nm_id,
    MAX(product_name) AS product_name,
    SUM(net_revenue_amount) AS net_revenue,
    SUM(product_profit_amount) AS profit,
    CASE
        WHEN SUM(net_revenue_amount) = 0 THEN NULL
        ELSE ROUND(SUM(product_profit_amount) / SUM(net_revenue_amount) * 100, 2)
    END AS margin_percent
FROM daily_finance_entries
WHERE user_id = :userId
  AND business_date BETWEEN :dateFrom AND :dateTo
  AND nm_id IS NOT NULL
GROUP BY nm_id
ORDER BY profit DESC
LIMIT :limit;
```

## 55. Thymeleaf-страница финансового анализа

### 55.1 Порядок блоков

```text
Заголовок
Блок загрузки API
Статус последней загрузки
Календарь покрытия
Выбор периода отчёта
Сообщение о покрытии
Кнопка скачивания Excel
Карточки
Линейный график
Круговая диаграмма + денежные значения справа
Топ-5 прибыльных товаров + убыточные товары
```

### 55.2 Состояния страницы

- Нет токена: блок загрузки заблокирован, ссылка ведёт в аккаунт.
- Идёт загрузка: кнопка загрузки заблокирована, отображается статус.
- Покрытие неполное: Excel и аналитика скрыты, показаны пропущенные даты.
- Покрытие полное: Excel активен, аналитика загружается.
- Нет операций в покрытом периоде: отображаются нулевые карточки и пустые рейтинги.
- Есть неизвестные операции: предупреждение с количеством строк, но RAW не удаляется.

### 55.3 JavaScript-файлы

```text
finance-page.js
coverage-calendar.js
finance-dynamics-chart.js
cost-structure-chart.js
```

Каждый файл отвечает только за свой блок.

## 56. Круговая диаграмма структуры затрат

Используется тип `doughnut` или `pie`. Справа создаётся собственная легенда.

Каждая строка легенды содержит:

```text
цветовой маркер
название категории
сумма, форматированная Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' })
доля, два знака после запятой
```

Сумма долей может отличаться от 100,00 на 0,01 из-за округления отображения. Backend рассчитывает долю от полной суммы всех возвращённых категорий.

## 57. Excel-файл

### 57.1 Имя

```text
financial-report_2026-06-01_2026-06-30.xlsx
```

### 57.2 Лист `Финансовый отчёт`

Порядок колонок:

```text
A  Артикул WB
B  Наименование
C  Продано, шт.
D  Возвращено, шт.
E  Чистое количество, шт.
F  Продажи, ₽
G  Возвраты, ₽
H  Чистая выручка, ₽
I  Комиссия, ₽
J  Логистика, ₽
K  Себестоимость, ₽
L  Налог, ₽
M  Прибыль, ₽
N  Маржинальность, %
O  Себестоимость указана
```

После товарных строк:

```text
Итого по товарам
Эквайринг
Хранение
Приёмка
Штрафы
Дополнительные удержания
Итоговая прибыль
```

### 57.3 Лист `Динамика по дням`

```text
Дата
Чистая выручка
Комиссия
Логистика
Себестоимость
Налог
Прибыль товаров
Эквайринг
Хранение
Приёмка
Штрафы
Дополнительные удержания
Итоговая прибыль
```

### 57.4 Лист `ABC-анализ`

```text
Артикул WB
Наименование
Чистая выручка
Доля в общей выручке, %
Накопленная доля, %
Категория
```

Правило:

```text
A: cumulativeShare <= 80%
B: 80% < cumulativeShare <= 95%
C: cumulativeShare > 95%
```

Если строка пересекает границу, категория определяется по накопленной доле после добавления строки.

### 57.5 Оформление

- заголовки полужирные;
- первая строка закреплена;
- включён автофильтр;
- денежный формат `# ##0.00 ₽`;
- процент `0.00%` или числовое значение с форматом `0.00` в зависимости от записи;
- отрицательная прибыль может выделяться условным стилем;
- строка итогов полужирная;
- ширина колонок ограничивается, а не вычисляется бесконечным `autoSizeColumn` на огромных файлах.

## 58. Сессионная безопасность

### 58.1 Доступ

```text
/auth/**, /css/**, /js/**, /images/** — permitAll
/swagger-ui/**, /v3/api-docs/** — local или authenticated
/actuator/health — permitAll
остальные — authenticated
```

### 58.2 CSRF

CSRF включён для HTML-форм. Для `fetch` запросов токен передаётся из meta-тегов Thymeleaf.

### 58.3 Владение данными

Идентификатор пользователя не принимается из запроса. Он извлекается из `Authentication`. Любой repository query содержит `user_id`.

## 59. Ошибки и HTTP-коды

```text
400 INVALID_PERIOD
400 VALIDATION_ERROR
401 AUTHENTICATION_REQUIRED
403 ACCESS_DENIED
404 RESOURCE_NOT_FOUND
409 ACTIVE_SYNC_EXISTS
409 COST_DATE_CONFLICT
422 PERIOD_NOT_COVERED
429 WB_RATE_LIMIT
502 WB_INVALID_RESPONSE
503 WB_UNAVAILABLE
500 INTERNAL_ERROR
```

Формат:

```json
{
  "code": "PERIOD_NOT_COVERED",
  "message": "Для выбранного периода отсутствуют данные за 2 даты",
  "timestamp": "2026-06-19T12:30:00Z",
  "path": "/api/v1/finance/report/summary",
  "details": {
    "missingDates": ["2026-06-05", "2026-06-17"]
  }
}
```

## 60. Полная матрица тестов

### 60.1 Классификатор

- каждая строка продажи;
- каждая строка возврата;
- `doc_type_name` как запасной признак;
- хранение;
- логистика;
- неизвестная операция;
- регистр и пробелы.

### 60.2 Дата

- `rr_dt` имеет приоритет;
- затем `sale_dt`;
- затем `order_dt`;
- затем `create_dt`;
- отсутствуют все даты.

### 60.3 Хэш

- одинаковые данные дают одинаковый SHA-256;
- `10.0` и `10.00` дают одинаковое нормализованное значение;
- изменение значимого поля меняет хэш;
- порядок полей JSON не влияет.

### 60.4 Логистика

- один товар;
- два товара;
- три товара и остаток 0,01;
- один товар в нескольких RAW-строках;
- группа без товара, день с товарами;
- день без товаров;
- пустой `srid` не объединяет независимые строки;
- сумма долей равна пулу.

### 60.5 Себестоимость

- первая стоимость;
- изменение стоимости;
- стоимость в будущем не применяется;
- возврат уменьшает себестоимость;
- стоимость отсутствует;
- заднее изменение вызывает пересчёт диапазона.

### 60.6 Прибыль

- обычная продажа;
- возврат;
- отрицательная комиссия корректировки;
- нулевая выручка;
- общие расходы уменьшают итоговую прибыль;
- отсутствующая себестоимость помечается.

### 60.7 Integration

- Flyway на PostgreSQL;
- частичные уникальные индексы;
- уникальность RAW-хэша;
- пересчёт дня заменяет старые строки;
- SQL сводки;
- SQL временного ряда;
- SQL рейтинга;
- изоляция пользователей.

### 60.8 Web

- неавторизованный пользователь перенаправляется на login;
- CSRF защищает POST;
- пользователь не видит чужую себестоимость;
- Excel недоступен при неполном покрытии;
- активная синхронизация блокирует вторую;
- графики возвращают только данные текущего пользователя.

## 61. Проверка Docker-конфигурации

Полная конфигурация Docker определена в разделе 32. Перед началом прикладной разработки необходимо проверить:

1. `docker compose up --build` поднимает PostgreSQL и приложение;
2. приложение ожидает успешный healthcheck PostgreSQL;
3. Flyway создаёт схему на пустой базе;
4. том `postgres_data` сохраняет данные после `docker compose down`;
5. профиль `docker` не содержит секретов в репозитории;
6. приложение можно запускать из IDE при отдельно поднятом контейнере PostgreSQL;
7. GitHub Actions успешно собирает Docker-образ после `mvn verify`.

Docker считается единственным дополнительным инфраструктурным компонентом проекта. Kafka и другие брокеры не предусматриваются ни в обязательной версии, ни в скрытых заделах архитектуры.

## 62. Пошаговый порядок написания кода с ИИ

Каждый этап завершается компиляцией и тестами. Нельзя просить ИИ одновременно сгенерировать весь проект.

### Этап 1

Создать Spring Boot-проект, `pom.xml`, `Dockerfile`, `compose.yaml`, `.dockerignore`, `.env.example`, профили `local` и `docker`, затем добавить пустой smoke test.

### Этап 2

Создать Flyway V1–V6 и integration test, который запускает PostgreSQL Testcontainer и проверяет наличие таблиц и индексов.

### Этап 3

Создать account-модуль, регистрацию, login, session security и страницу аккаунта.

### Этап 4

Создать модуль себестоимости, CRUD, историю и `CostResolver` с тестами.

### Этап 5

Создать WB DTO, RestClient, properties, обработку 401, 429, 5xx и WireMock/MockWebServer tests.

### Этап 6

Создать RAW Entity, хэш, дату, классификатор и пакетную вставку.

### Этап 7

Создать `DailyCalculationContext`, накопители товаров и общую строку дня.

### Этап 8

Реализовать двухуровневое равное распределение логистики и покрыть unit-тестами.

### Этап 9

Добавить себестоимость, налог, прибыль и транзакционный пересчёт дня.

### Этап 10

Реализовать `SyncOrchestrator`, executor, журнал и покрытие.

### Этап 11

Реализовать JdbcClient-запросы аналитики и REST DTO.

### Этап 12

Создать Thymeleaf-страницу, календарь, формы и Chart.js.

### Этап 13

Создать Apache POI экспорт и ABC-анализ.

### Этап 14

Добавить полные integration/web tests, GitHub Actions, README и архитектурные диаграммы.

## 63. Шаблон запроса к ИИ для каждого класса

```text
Реализуй класс <полное имя> строго по разделу <номер> технической спецификации.
Не создавай дополнительные таблицы, endpoints и поля.
Используй Java 21, Spring Boot, BigDecimal и существующие enum.
Сначала перечисли контракт класса, затем выведи код класса и unit-тесты.
Не меняй публичные DTO без отдельного согласования.
После кода укажи потенциальные ошибки компиляции и необходимые зависимости.
```
## 64. Проверка готовности перед демонстрацией

- `mvn clean verify` проходит локально;
- CI проходит на GitHub;
- `docker compose up --build` поднимает приложение с пустой базой;
- Flyway применяет все миграции;
- регистрация и вход работают;
- токен WB не выводится в лог;
- повторная загрузка не создаёт RAW-дубли;
- пересчёт одного дня идемпотентен;
- суммы распределённой логистики сохраняются без потерь округления;
- календарь правильно показывает покрытие;
- Excel недоступен при пропусках;
- карточки, график и структура затрат совпадают с Excel;
- общая логистика интерфейса равна сумме товарной логистики;
- дополнительные удержания включают только утверждённые компоненты;
- ABC-анализ строится по чистой выручке;
- данные двух пользователей изолированы;
- README содержит запуск, архитектуру, формулы и ограничения.

## 65. Ограничения, которые необходимо явно указать

- Финансовая модель ориентирована на Wildberries.
- Налог рассчитывается по пользовательской процентной ставке и упрощённой формуле проекта.
- Логистика без прямой товарной связи распределяется равными долями, что является принятым расчётным правилом.
- Если за день нет товаров, нераспределённый логистический пул включается в дополнительные удержания.
- Отсутствующая себестоимость делает прибыль неполной.
- Неизвестные операции не включаются автоматически в неопределённую категорию.
- Исторические изменения API могут потребовать обновления классификатора.
- Приложение не заменяет бухгалтерский или налоговый учёт.
