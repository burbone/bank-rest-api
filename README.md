# Bank Cards

REST API для управления банковскими картами с JWT аутентификацией.

## Запуск проекта

### Требования
- Docker Desktop
- Java 17+
- Maven 3.6+

### Шаги запуска

1. Клонировать репозиторий:
```bash
git clone https://github.com/burbone/bank-rest-api
cd bank-rest-api
```

2. Запустить базу данных и приложение:
```bash
docker-compose up -d
```

### Альтернативный запуск (без Docker для приложения)

1. Запустить только базу данных:
```bash
docker-compose up -d postgres
```

2. Собрать и запустить приложение:
```bash
mvn clean install
mvn spring-boot:run
```

## Документация API

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI спецификация: `docs/openapi.yaml`

## Тестирование

Запуск тестов:
```bash
mvn test
```