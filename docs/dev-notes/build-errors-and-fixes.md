# Build Errors — Causes and Fixes

A record of build failures encountered during development, what caused them, and how to avoid them.

---

## 2026-04-06 — SettingsScreen.kt: Wrong type for NotificationPrefs parameter

### Errors
```
Argument type mismatch: actual type is 'NotificationPrefs.Prefs', but 'NotificationPrefs' was expected. :75
Unresolved reference 'aloneMaxMinutes'. :83
Unresolved reference 'barStartMinute'. :86
Unresolved reference 'barEndMinute'. :87
```

### Root cause
`NotificationPrefs` is an `object` (singleton) with a nested `data class Prefs` that holds the actual values.
`NotificationPrefs.get(context)` returns a `NotificationPrefs.Prefs`, not a `NotificationPrefs`.

When rewriting `SettingsScreen.kt`, the `GeneralTab` composable was given the parameter type `NotificationPrefs`
(the object) instead of `NotificationPrefs.Prefs` (the data class). This meant the properties
`.aloneMaxMinutes`, `.barStartMinute`, `.barEndMinute` were unresolvable — they live on `Prefs`, not on the object.

```kotlin
// Wrong
private fun GeneralTab(context: Context, prefs: NotificationPrefs) { ... }

// Correct
private fun GeneralTab(context: Context, prefs: NotificationPrefs.Prefs) { ... }
```

### How to avoid
Before passing a value returned by a factory/companion function as a parameter, check the *return type*
of that function, not just the name of the class. Nested data classes inside objects are a common Kotlin pattern —
`Foo.get()` often returns `Foo.Data`, not `Foo`.

Specifically: always read the file for any class being used before writing code that references it.
This error was caused by writing `SettingsScreen.kt` without first reading `NotificationPrefs.kt`.

---

## 2026-04-06 — Architectural lessons from the notification system build

### Context: what was built
- `OchreAlarmScheduler` — schedules exact `AlarmManager` alarms for food/walk/alone alerts
- `AlertReceiver` — `BroadcastReceiver` that fires `IMPORTANCE_HIGH` heads-up notifications
- `BootReceiver` — restores all alarms after device restart
- Smart feed dismissal: `AlertReceiver.handleFoodAlert` checks feed log before firing and skips/stops repeating if meal was already logged

### Key patterns established

**AlarmManager vs WorkManager for time-critical alerts**
WorkManager is for deferrable background work (backups, syncs). For "fire at exactly 18:30" user-facing alerts, use `AlarmManager.setExactAndAllowWhileIdle()`. WorkManager can be delayed by battery optimisation.

**BroadcastReceiver + `goAsync()` for coroutine work**
`BroadcastReceiver.onReceive` runs on the main thread with a 10-second window. Use `goAsync()` + a coroutine scope for any suspend operations (DB reads). Always call `pendingResult.finish()` in a `finally` block.

**PendingIntent request codes must be unique and stable**
Each alarm type needs a fixed, unique request code. If two alarms share a request code, scheduling one replaces the other. Defined in `OchreAlarmScheduler` as private constants.

**AlarmManager alarms are lost on reboot**
`BOOT_COMPLETED` receiver is mandatory for any alarm-based system. Without it, all scheduled alerts vanish after restart.

**`USE_EXACT_ALARM` vs `SCHEDULE_EXACT_ALARM`**
- `SCHEDULE_EXACT_ALARM` (API 31+): user must grant via Settings; revocable. Check `canScheduleExactAlarms()` before use.
- `USE_EXACT_ALARM` (API 33+): granted automatically for alarms/reminder apps; not revocable by user.
Both are declared so the app works across API 31–36.

**Smart cancellation pattern**
When the condition that triggered a scheduled alert resolves (feed logged, walk started, dog returns home), immediately call the scheduler's cancel method. The `AlertReceiver` also checks the condition at fire time as a safety net — two-layer guard against stale alerts.

---
