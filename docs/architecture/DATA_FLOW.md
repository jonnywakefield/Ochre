# Data Flow Example

To understand how the architecture works, let's trace a single action: **Tapping the `[LOG FEED]` button.**

1.  **UI Event:** The user taps the button in `HudScreen` (Presentation Layer).
2.  **ViewModel Action:** `HudScreen` calls `viewModel.logEvent(EventType.FEED)` (Presentation Layer).
3.  **Use Case Execution:** The ViewModel calls `LogEventUseCase(type = EventType.FEED)` (Domain Layer).
4.  **Repository Call:** `LogEventUseCase` creates a `DogEvent` with the current timestamp and calls `eventRepository.insertEvent(event)` (Domain Layer interface).
5.  **Data Persistence:** `EventRepositoryImpl` maps the `DogEvent` to an `EventEntity` and saves it via `EventDao` to the Room SQLite database (Data Layer).
