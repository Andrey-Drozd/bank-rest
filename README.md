# Bank REST

`Spring Boot` сервис на `Java 17` для управления банковскими картами, пользователями и переводами между своими картами.

## Что реализовано

- JWT-аутентификация и авторизация с ролями `ADMIN` и `USER`
- регистрация, логин и профиль текущего пользователя
- CRUD-операции по картам для администратора
- просмотр своих карт, баланса и запрос на блокировку карты для пользователя
- переводы между своими картами
- административное управление пользователями
- пагинация, фильтрация и валидация входных данных
- единый формат ошибок
- Liquibase-миграции
- Docker Compose для PostgreSQL и приложения
- тесты ключевой бизнес-логики

## Стек

- `Java 17`
- `Spring Boot 3`
- `Spring Security + JWT`
- `Spring Data JPA`
- `PostgreSQL`
- `Liquibase`
- `Springdoc OpenAPI / Swagger UI`
- `JUnit 5`
- `Mockito`
- `H2` для тестового профиля

## Структура

- `controller` — REST-контроллеры
- `service` — бизнес-логика
- `repository` — доступ к БД через Spring Data JPA
- `entity` — JPA-сущности
- `dto` — контракты запросов и ответов
- `security` — JWT, фильтр и `UserDetailsService`
- `exception` — пользовательские исключения и глобальный обработчик ошибок
- `util` — шифрование, HMAC и маскирование номера карты
- `src/main/resources/db/migration` — миграции Liquibase
- `docs/openapi.yaml` — OpenAPI-контракт

## Архитектура

- `AuthController` / `AuthService` — регистрация, логин и профиль текущего пользователя
- `CardController` / `CardService` — управление картами, балансом и заявками на блокировку
- `TransferController` / `TransferService` — переводы между своими картами
- `UserController` / `UserService` — административное управление пользователями
- `repository` слой — persistence через Spring Data JPA

## Безопасность

- номер карты хранится в БД в зашифрованном виде
- для поиска и проверки уникальности используется HMAC-хеш номера карты
- наружу номер карты отдаётся только в маске вида `**** **** **** 1234`
- доступ к API ограничен ролями `ADMIN` и `USER`
- приложение работает stateless через JWT
- для карты используется optimistic locking (`@Version`)
- для переводов используется pessimistic lock на обе карты в фиксированном порядке

## Основные эндпоинты

Аутентификация:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`

Карты:

- `POST /api/cards` — создать карту (`ADMIN`)
- `GET /api/cards` — список всех карт (`ADMIN`)
- `GET /api/cards/{cardId}` — получить карту (`ADMIN`)
- `PATCH /api/cards/{cardId}/activate` — активировать карту (`ADMIN`)
- `PATCH /api/cards/{cardId}/block` — заблокировать карту (`ADMIN`)
- `DELETE /api/cards/{cardId}` — удалить карту (`ADMIN`)
- `GET /api/cards/me` — мои карты с поиском по masked-номеру и пагинацией
- `GET /api/cards/me/{cardId}` — моя карта
- `GET /api/cards/me/{cardId}/balance` — баланс моей карты
- `POST /api/cards/me/{cardId}/block-request` — запрос на блокировку моей карты

Переводы:

- `POST /api/transfers/me` — перевод между своими картами
- `GET /api/transfers/me` — история переводов текущего пользователя

Пользователи:

- `GET /api/users` — список пользователей (`ADMIN`)
- `GET /api/users/{userId}` — получить пользователя (`ADMIN`)
- `PATCH /api/users/{userId}` — обновить пользователя (`ADMIN`)
- `DELETE /api/users/{userId}` — логическое удаление пользователя (`ADMIN`)

Полный контракт API описан в [docs/openapi.yaml](docs/openapi.yaml).

## Профили

- `local` — профиль по умолчанию для ручной разработки
- `prod` — профиль для production-like запуска без Swagger UI и demo data
- `test` — H2 + Liquibase для автоматических тестов

## Конфигурация

Шаблон переменных окружения:

- `.env.example`

Основные параметры:

- `SERVER_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SECURITY_JWT_SECRET`
- `SECURITY_JWT_LIFETIME`
- `CARD_ENCRYPTION_SECRET`
- `CARDS_EXPIRATION_SYNC_CRON`

Важно:

- в `local` есть значения по умолчанию для быстрого запуска
- в `prod` `SECURITY_JWT_SECRET` и `CARD_ENCRYPTION_SECRET` нужно задать явно

## Быстрый запуск

Есть два варианта запуска.

### Вариант 1. Полностью через Docker

1. Поднять PostgreSQL и приложение:

```bash
docker compose up -d --build
```

2. Проверить health:

```bash
curl http://localhost:8080/api/actuator/health
```

3. Для `local`-режима Swagger UI будет доступен по адресу:

```text
http://localhost:8080/api/swagger-ui.html
```

4. OpenAPI JSON:

```text
http://localhost:8080/api/v3/api-docs
```

Логи приложения в Docker:

```bash
docker logs -f bankcards-app
```

По умолчанию `docker compose` запускает приложение в `local`-профиле. Если нужен production-like режим, задайте `SPRING_PROFILES_ACTIVE=prod` и реальные секреты в `.env`.

### Вариант 2. PostgreSQL в Docker + приложение локально

1. Поднять только PostgreSQL:

```bash
docker compose up -d postgres
```

2. При необходимости скопировать `.env.example` в `.env` и скорректировать параметры.

3. Запустить приложение:

```bash
mvn spring-boot:run
```

4. Проверить health:

```bash
curl http://localhost:8080/api/actuator/health
```

5. Swagger UI:

```text
http://localhost:8080/api/swagger-ui.html
```

## Тестовые данные для local/test

Liquibase добавляет demo-пользователей только в профилях `local` и `test`:

- `admin@bankcards.local` / `password2`
- `user@bankcards.local` / `password3`

Их можно использовать для ручной проверки ролей, карт и переводов.

## Тесты

Локальный запуск тестов:

```bash
mvn test
```

Сейчас тестами покрыты:

- поднятие Spring-контекста
- шифрование, хеширование и маскирование номера карты
- основные сценарии `card-service`
- основные сценарии `transfer-service`
- основные сценарии `user-service`
- основные сценарии `auth-service`

## База данных и миграции

Миграции лежат в:

```text
src/main/resources/db/migration
```

Основные таблицы:

- `users`
- `roles`
- `user_roles`
- `cards`
- `transfers`

Liquibase управляет схемой, а `spring.jpa.hibernate.ddl-auto=validate` проверяет соответствие entity и БД.

## Что важно по бизнес-логике

- пользователь может переводить деньги только между своими картами
- карта с истёкшим сроком не может участвовать в изменениях и переводах
- удаление пользователя реализовано как логическое удаление
- администратор не может управлять собственным аккаунтом через административные эндпоинты
- удаление карты запрещено, если по ней уже есть связанные операции

## Возможные улучшения

- health-check, завязанный не только на Spring, но и на доступность БД и планировщика
- интеграционный запуск в CI с PostgreSQL через Testcontainers
- контейнерный production-профиль с отдельной конфигурацией логирования
- rate limiting и audit logging для чувствительных операций
- отдельные integration tests для security и controller-слоя
