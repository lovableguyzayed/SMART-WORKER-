# Smart Worker — Workforce Management (Native Android)

Enterprise workforce management for construction & field teams: workers,
attendance (manual / bulk / employee-ID / QR), payroll with configurable pay
policies, transactions, sites/projects/tasks/departments, closures, leave
ledger, reports, CSV/PDF export and optional cloud replication.

Native Kotlin + Jetpack Compose port of the SmartWorker Flask application —
same business rules, offline-first architecture.

## Build & Run

**Requirements:** Android Studio (latest stable), JDK 17+. Everything else
(Gradle 9.1, SDK components) is fetched automatically.

```bash
./gradlew assembleDebug     # debug APK — works on a fresh clone, no setup
./gradlew assembleRelease   # release APK — falls back to debug signing if no upload key
./gradlew test              # JVM unit tests (payroll engine)
```

APKs land in `app/build/outputs/apk/`.

- **Debug signing:** uses the standard Android debug keystore automatically.
- **Release signing (Play Store):** provide `my-upload-key.jks` in the repo root
  (or `KEYSTORE_PATH`) plus `STORE_PASSWORD` and `KEY_PASSWORD` env vars.
  Without them, `assembleRelease` still succeeds using debug signing.

## First Login (seeded demo data)

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Attendance user | `suresh` | `suresh123` |

The database seeds 8 demo workers, sites, projects, departments and 14 days of
attendance on first launch so every screen is populated.

## Cloud sync (optional)

The app is fully offline-first — Room is the source of truth and no
configuration is needed. To enable automatic background replication to
Supabase, copy `.env.example` to `.env` and set:

```
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon key>
```

APKs built with these values replicate core tables (workers, attendance,
transactions, sites, projects, assignments) to `sw_*` tables via PostgREST —
every 6 hours in the background (WorkManager) and on demand from
**More → Cloud Backup**. Unconfigured builds skip sync silently.

## Architecture

```
ui/ (Compose screens + ViewModels)  →  data/repo/ (workflow rules)
        →  domain/PayrollCalculator (pure-Kotlin pay engine, unit-tested)
        →  data/db/ (Room: 15 entities)  →  data/sync/ (Supabase replication)
```

MVVM + repository pattern, manual DI (`AppContainer`), Material 3, single
activity, role-based permissions (admin/manager vs scoped attendance users).
