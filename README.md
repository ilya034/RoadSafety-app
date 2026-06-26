# RoadSafety — Безопасная дорога детства

[![Version](https://img.shields.io/badge/version-0.1.5-blue.svg)](https://github.com/ilya034/RoadSafetyApp)

Мобильное приложение для детей и родителей на Kotlin, предназначенное для обеспечения безопасности школьников при передвижении по городу.

## Описание проекта

Персональный навигатор безопасности для школьников и система мониторинга для родителей. Приложение использует систему цветовых зон (зеленая, желтая, красная) для оценки безопасности маршрута и поведения ребенка.

## Технологический стек

* **Язык:** [Kotlin](https://kotlinlang.org/) (2.1.10)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3 Adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive)
* **Архитектура:** MVVM + Clean Architecture principles
* **DI:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Сеть:** [Retrofit](https://square.github.io/retrofit/) + [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* **Карты:** [MapLibre Native](https://maplibre.org/) + [MapLibre Compose](https://github.com/ramani-maps/maplibre-compose)
* **Push-уведомления:** [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
* **Локальное кэширование:** [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) / Preferences

## Технические особенности

### Картография и безопасность
Приложение использует оптимизированный рендеринг зон безопасности на базе **векторных тайлов (MVT)**. 
- **Динамическая стилизация**: Цвета зон (risk level) применяются на лету через Style Expressions.
- **Parental Overrides**: Родители могут переопределять уровень риска для конкретных полигонов, что мгновенно отображается на устройствах детей.
- **Оффлайн-поддержка**: Базовая карта и зоны кэшируются для работы в условиях нестабильного интернета.

### API
Взаимодействие с бэкендом описано в спецификации [RoadSafetyApi.yaml](RoadSafetyApi.yaml).
- Базовый URL (build.gradle.kts): `https://roadsafety.my.to/api/`

## Инструкция по запуску

Для сборки и запуска приложения вам понадобятся:
* **Android Studio**
* **JDK 17**
* Доступ к интернету для загрузки зависимостей и карт.

### Запуска:

1. **Клонирование репозитория:**
   ```bash
   git clone https://github.com/ilya034/RoadSafetyApp.git
   cd RoadSafetyApp
   ```
   
2. **Настройка ключей:**
   * В файле `local.properties` добавьте API ключ для MapTiler:
     `maptiler.key=YOUR_API_KEY`

5. **Запуск:**
   * Выберите модуль `app` в конфигурации запуска.
   * Нажмите кнопку **Run**(Android Studio).
   * или *gradle run*


## Структура проекта
* `app/src/main/java/team/kid/roadsafety/domain` — доменная логика, репозитории (интерфейсы) и модели.
* `app/src/main/java/team/kid/roadsafety/presentation` — UI слои на Compose (Screen, ViewModel, UI State).
* `app/src/main/java/team/kid/roadsafety/data` — реализации репозиториев, API (Retrofit DTO) и локальное хранилище.
* `app/src/main/java/team/kid/roadsafety/infrastructure` — платформенные сервисы (Location, Notifications).

---
Подробное ТЗ находится в файле [TZ.md](TZ.md).
