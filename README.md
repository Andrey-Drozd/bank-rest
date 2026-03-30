# Bank REST

Приложение на `Spring Boot` для управления банковскими картами, пользователями и переводами между своими картами.

В этом файле собрана информация по текущей реализации и запуску проекта.

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
- Docker Compose для PostgreSQL
- тесты ключевой бизнес-логики

## Стек

- `Java 17`
- `Spring Boot 3`
- `Spring Security + JWT`
- `Spring Data JPA`
- `PostgreSQL`
- `Liquibase`
- `Springdoc OpenAPI / Swagger UI`
- `JUnit 5`, `Mockito`, `H2` для тестового профиля

## Архитектура

Структура проекта:

- `controller` — REST-контроллеры
- `service` — бизнес-логика
- `repository` — доступ к БД через Spring Data JPA
- `entity` — JPA-сущности
- `dto` — контракты запросов и ответов
- `security` — JWT, фильтр, `UserDetailsService`
- `exception` — пользовательские исключения и глобальный обработчик ошибок
- `util` — шифрование и маскирование номера карты
- `src/main/resources/db/migration` — файлы миграций Liquibase

## Безопасность

- номер карты хранится в БД в зашифрованном виде
- для поиска и уникальности используется HMAC-хеш номера карты
- наружу номер карты отдаётся только в маске вида `**** **** **** 1234`
- доступ к API ограничен ролями `ADMIN` и `USER`
- приложение работает без серверной сессии, через JWT
- для карты включён optimistic locking (`@Version`)
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
- `prod` — профиль для продакшена без тестовых данных и без Swagger UI
- `test` — H2 + Liquibase для автоматических тестов

## Переменные окружения

Минимальный набор есть в [.env.example](.env.example):

```env
POSTGRES_DB=bankcards
POSTGRES_USER=bankcards
POSTGRES_PASSWORD=bankcards

SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bankcards
SPRING_DATASOURCE_USERNAME=bankcards
SPRING_DATASOURCE_PASSWORD=bankcards

SECURITY_JWT_SECRET=replace-with-long-random-secret
SECURITY_JWT_LIFETIME=3600000
CARD_ENCRYPTION_SECRET=replace-with-long-random-card-secret
CARDS_EXPIRATION_SYNC_CRON=0 0 * * * *
```

Важно:

- в `local` есть значения по умолчанию для быстрого локального запуска
- в `prod` `SECURITY_JWT_SECRET` и `CARD_ENCRYPTION_SECRET` должны быть заданы явно

## Локальный запуск

1. Подними PostgreSQL:

```bash
docker compose up -d
```

2. При необходимости задай переменные окружения из `.env.example`.

3. Запусти приложение:

```bash
mvn spring-boot:run
```

4. Swagger UI будет доступен по адресу:

```text
http://localhost:8080/api/swagger-ui.html
```

5. OpenAPI JSON:

```text
http://localhost:8080/api/v3/api-docs
```

## Тестовые данные для local/test

Liquibase добавляет тестовых пользователей только в профилях `local` и `test`:

- `admin@bankcards.local` / `password2`
- `user@bankcards.local` / `password3`

Их можно использовать для ручной проверки ролей, карт и переводов.

## Тесты

Запуск тестов:

```bash
mvn test
```

Покрыты:

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
