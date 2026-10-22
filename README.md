# Kotlin Automation (KA)

[![CI](https://github.com/tawsor/KA/actions/workflows/ci.yml/badge.svg)](https://github.com/tawsor/KA/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4%2B-blue.svg)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-orange.svg)](https://openjdk.org)

> Масштабируемый, модульный фреймворк и CLI-движок для сквозной автоматизации DevOps, задач оркестрации, интеграций и API-пайплайнов на Kotlin и корутинах.

---

## 🏗 Архитектура модулей

| Модуль | Описание | Основные возможности |
|---|---|---|
| [`ka-core`](ka-core) | Ядро фреймворка | Декларативный type-safe DSL, контекст пайплайнов, параллельные шаги, повторы (retries), хуки (`onSuccess`/`onFailure`), маскирование секретов |
| [`ka-tasks`](ka-tasks) | DevOps & системные задачи | Кроссплатформенный шелл (`sh`), управление процессами, Git-хелперы, файловые операции, поддержка `.env` файлов |
| [`ka-http`](ka-http) | Сетевые интеграции | Асинхронный Ktor Client, JSON-сериализация, вебхуки, поллинг (`pollUntil`), уведомления в Telegram и Discord |
| [`ka-cli`](ka-cli) | Консольный интерфейс | Точка входа для запуска пайплайнов из терминала, сборка единого исполняемого Fat JAR |
| [`ka-example`](ka-example) | Рецепты и живые примеры | Готовый пример полного пайплайна с демонстрацией всех возможностей |

---

## 🚀 Быстрый старт с Kotlin DSL

```kotlin
import kotlinx.coroutines.runBlocking
import tawsor.ka.core.pipeline
import tawsor.ka.tasks.sh
import tawsor.ka.tasks.gitBranch
import tawsor.ka.tasks.loadDotenv
import tawsor.ka.http.httpGet
import tawsor.ka.http.telegramNotify
import kotlin.time.Duration.Companion.milliseconds

fun main() = runBlocking {
    val myPipeline = pipeline("CI & Deployment Orchestration") {

        step("1. Подготовка и окружение") {
            loadDotenv() // Автоматически загружает переменные из .env
            val branch = gitBranch()
            logInfo("Текущая ветка: $branch")
            set("branch", branch)
        }

        parallel("2. Параллельные проверки") {
            step("Сборка через Gradle", retries = 1, retryDelay = 500.milliseconds) {
                sh("./gradlew check")
            }

            step("Healthcheck внешнего сервиса") {
                val status = httpGet("https://api.github.com/zen")
                logInfo("Статус сервиса: $status")
            }
        }

        onSuccess { ctx ->
            ctx.logSuccess("Пайплайн успешно завершён!")
            // ctx.telegramNotify(botToken = "...", chatId = "...", message = "✅ Релиз готов!")
        }

        onFailure { ctx, error ->
            ctx.logError("Пайплайн упал: ${error.message}")
            // ctx.telegramNotify(botToken = "...", chatId = "...", message = "❌ Ошибка сборки!")
        }
    }

    myPipeline.execute()
}
```

---

## 🛠 Сборка и запуск

### Требования
- **JDK 21** (LTS)
- **Gradle 9+** (через включенный `gradlew`)

### Команды

```bash
# Собрать все модули и прогнать тесты
./gradlew build

# Собрать автономный Fat JAR для CLI
./gradlew :ka-cli:fatJar

# Запустить демонстрационный сценарий
./gradlew :ka-example:run
```

---

## 📦 CI/CD и релизы (GitHub Actions)

- **Непрерывная интеграция (CI)**:
  - Автоматически проверяет PR и push в `master` / `main`.
  - Валидирует контрольные суммы Gradle Wrapper.
  - Сохраняет HTML-отчёты о тестах и сборки JAR.
- **Публикация пакетов (Releases)**:
  - При пуше тега версии (`git tag v0.1.0 && git push origin v0.1.0`):
    - Публикует артефакты `ka-core`, `ka-tasks`, `ka-http` в **GitHub Packages** (`maven.pkg.github.com`).
    - Создает **GitHub Release** с прикреплением исполняемого `ka-cli-all.jar`.
- **Dependabot**:
  - Еженедельный автоматический поиск обновлений зависимостей и версий GitHub Actions.

---

## 📄 Лицензия

Проект распространяется под открытой лицензией [MIT](LICENSE).
