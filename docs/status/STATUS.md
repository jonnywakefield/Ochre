# Ochre Project Status

## What has been done
- [x] Defined initial project requirements and utility-first specification.
- [x] Initialized project directory structure with clean architecture layout.
- [x] Initialize Gradle build files (`build.gradle.kts`, `settings.gradle.kts`).
- [x] Add `AndroidManifest.xml` and `MainActivity.kt`.
- [x] Set up manual Dependency Injection container (`AppContainer`/`DefaultAppContainer`).
- [x] Implement domain models (`DogEvent`, `EventType`).
- [x] Implement repository pattern (`EventRepository` interface + `EventRepositoryImpl`).
- [x] Implement use cases (`LogEventUseCase`, `GetLastEventUseCase`, `UpdateEventUseCase`, `DeleteEventUseCase`, `GetAllEventsUseCase`, `GetLastEventPerTypeUseCase`).
- [x] Implement local database (Room) for event logging (`AppDatabase`, `EventDao`, `EventEntity`).
- [x] Implement Jetpack Navigation Compose with bottom nav bar (Home / History / Settings).
- [x] Centralised theme (`OchreTheme`) with ochre colour scheme — black background, ochre accent, off-white text.
- [x] Home screen — dashboard with last-event-per-type rows and quick-log `+` buttons.
- [x] Shared `EventSheet` bottom sheet — log/edit with date/time pickers, value and note inputs.
- [x] History screen — chronological log grouped by date, filter chips, inline edit/delete, log-past-event flow.
- [x] Settings screen (placeholder).

## What has NOT been done (TODO)
- [ ] Implement Preferences DataStore for persistent state (timers, dog name, units preference).
- [ ] Walk Mode — active timer UI state.
- [ ] Sitter Export utility.
- [ ] Populate `core` module with shared utilities (formatting helpers, haptic feedback wrappers).
- [ ] Edge-case handling: confirm before delete, undo snackbar.
