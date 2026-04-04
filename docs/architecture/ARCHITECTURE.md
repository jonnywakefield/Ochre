# Ochre Architecture Guide

Built with native Android, Kotlin, and Jetpack Compose following **Clean Architecture** with **MVVM**.

## Core Philosophy
1. **Separation of Concerns** — UI doesn't know where data comes from. The database doesn't know how data is displayed.
2. **Scalability** — New features are new use cases + new ViewModels. Core logic is never rewritten.
3. **Unidirectional Data Flow** — State flows down from ViewModel to UI. Events flow up from UI to ViewModel.

## Module Breakdown (`app/src/main/java/com/ochre`)

### `app`
Application entry point. Contains `OchreApp`, `MainActivity`, and the manual DI container (`AppContainer` / `DefaultAppContainer`). `MainActivity` sets up `OchreTheme` and `AppNavGraph`.

### `core`
Shared utilities and base classes (formatting helpers, haptic feedback wrappers). *Not yet populated.*

### `domain`
The innermost layer — **no Android dependencies**.
- `model/` — `DogEvent`, `EventType`
- `repository/` — `EventRepository` interface
- `usecase/` — `LogEventUseCase`, `UpdateEventUseCase`, `DeleteEventUseCase`, `GetAllEventsUseCase`, `GetLastEventUseCase`, `GetLastEventPerTypeUseCase`

### `data`
Implements `domain` interfaces using Room.
- `local/` — `AppDatabase`, `EventDao`, `EventEntity` (+ mapper extensions)
- `repository/` — `EventRepositoryImpl`

### `presentation`
UI layer, Jetpack Compose.
- `common/` — `OchreTheme` (colours, typography), `EventSheet` (shared bottom sheet composable)
- `navigation/` — `Screen`, `AppNavGraph`, `OchreNavBar`
- `home/` — `HomeScreen`, `HomeViewModel` (dashboard with last-event-per-type + quick-log)
- `history/` — `HistoryScreen`, `HistoryViewModel` (full log, filter, edit, delete, log past)
- `settings/` — `SettingsScreen` (placeholder)

## Design Language
- Background: `#000000`
- Accent (interactive): `#E4A853`
- Text primary: `#F0EDE8`
- Text secondary: `#6B6B6B`
- Destructive: `#CF6679`
- No cards, no borders, no shadows — typography and spacing only.
