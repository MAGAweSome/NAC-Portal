# ParishSync — Implementation & Deployment Roadmap

## 1. Architecture Overview
**ParishSync** is built with a native Kotlin + Jetpack Compose **Offline-First MVVM** architecture:
- **UI Layer (`ui/screens/`, `Navigation.kt`)**: Material 3 Compose screens with Role-Based Access Control (RBAC) conditional rendering and dynamic bottom navigation routing tailored to `Member`, `Minister / Rector`, and `Administrator` roles.
- **ViewModel Layer (`ui/ParishViewModel.kt`)**: Reactive `StateFlow<ParishUiState>` combining active user session, congregation filter, Room database streams, and Supabase sync status.
- **Repository Layer (`data/repository/ParishRepository.kt`)**: `OfflineFirstParishRepository` treats **Room SQLite (`ParishDatabase`)** as the single source of truth for zero-latency offline UI rendering, while synchronizing mutations and auth sessions with **Supabase (`SupabaseClientService`)**.
- **Background Processing (`workers/`)**:
  - `PdfScheduleParseWorker` & `PdfScheduleParser`: Background `WorkManager` job that extracts service dates and ministry assignments from uploaded schedule PDFs, matches names against the congregation roster in Room, and highlights the local user's assignments.
  - `ScheduleSyncWorker` & `ParishNotificationHelper`: Background schedule sync and local Android notification triggers for upcoming volunteer commitments and urgent broadcasts.

---

## 2. Phase 1: Local Testing with Room SQLite (Offline-First Mode)
By default, **ParishSync** boots in **Offline-First Room SQLite Testing Mode** with zero external cloud dependencies required.

### Step-by-Step Local Testing Instructions
1. **Build & Run Locally**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```
2. **Automatic Database Seeding**:
   - On first launch, `ParishDatabase` (`parish_sync_offline.db`) automatically runs `ParishSeedData` via `ensureSeedDataPopulated()`.
   - Pre-populates 3 congregations (**St. Jude's Episcopal Parish**, **Grace Lutheran Community**, **All Saints Cathedral**), a 9-person parish directory, weekly announcements, interactive Sunday service leaflets, volunteer rosters across all 6 ministry duties (`Altar Setup`, `Ushering`, `AV / Sound Booth`, `Sunday School`, `Coffee Hour`, `Sanctuary Cleaning`), choir SATB hymn lists, prayer requests, ride-sharing posts, and Discipleship Small Groups (DSGs).
3. **Testing Onboarding & Role-Based Access Control (RBAC)**:
   - On the **Onboarding Screen**, use the **Quick Role Presets** (`Member (Hannah)`, `Minister / Rector`, or `Administrator`) or enter any email/password, choose a congregation and role, and tap **Enter**.
   - At any time inside the app, tap the **Role Switcher Chip** in the top app bar to switch between `Member`, `Minister / Rector`, and `Administrator` or switch congregations to verify dynamic Bottom Navigation Bar routing and conditional UI controls.
4. **Testing Smart PDF Schedule Indexing Locally**:
   - Switch to **Minister / Rector** or **Administrator** role -> open the **Ministry** tab -> **Smart PDF Indexer**.
   - Tap **"Pick PDF File"** to select a PDF from device storage, OR tap **"Index Advent PDF"** to run `PdfScheduleParseWorker` via `WorkManager` against the built-in multi-Sunday Advent rota stream.
   - Navigate back to the **Roster** tab to see newly indexed dates (`2026-11-22`, `2026-11-29`), matched congregation members, and `"YOUR ASSIGNMENT"` badges highlighted for the active user.

---

## 3. Phase 2: Connecting Live Supabase Cloud Credentials
When moving from local SQLite testing to a live multi-user parish deployment, connect a Supabase project:

### Step 1: Provision Supabase PostgreSQL Tables & RLS Policies
1. Create a new project at [https://supabase.com](https://supabase.com).
2. Open the **SQL Editor** in your Supabase Dashboard.
3. Copy and execute the contents of [`supabase/supabase_schema.sql`](supabase/supabase_schema.sql).
   - Creates `user_profiles`, `announcements`, `volunteer_assignments`, `hymn_selections`, `prayer_requests`, `ride_shares`, and `small_groups`.
   - Enables **Row Level Security (RLS)** on every table and creates the `public.current_user_role()` security-definer helper.
   - Enforces database-level RBAC policies:
     - `MEMBER`: Read access to bulletins, rosters, hymns, and public prayer chain; insert access for prayer intentions and ride shares.
     - `MINISTER`: Elevated write access to announcements, volunteer schedules, DSG pastoral logs, and full read/triage access to confidential clergy-only prayer/visit requests.
     - `ADMINISTRATOR`: Full CRUD across all tables plus exclusive permission to modify `user_profiles.role`.
   - Enables **Supabase Realtime** publications on `announcements`, `volunteer_assignments`, `prayer_requests`, and `ride_shares`.

### Step 2: Configure Supabase Auth
1. In the Supabase Dashboard, navigate to **Authentication -> Providers -> Email**.
2. Enable **Email/Password** provider (disable email confirmation during staging if desired for instant onboarding).

### Step 3: Connect Credentials in ParishSync
You can supply your Supabase project credentials in either of two ways:
- **Option A — In-App Admin Console (Zero Rebuild)**:
  1. Sign in with the **Administrator** role.
  2. Open the **Admin** tab (`Broadcast & RBAC`) and scroll to **Backend Sync: Room SQLite & Live Supabase**.
  3. Enter your **Supabase Project URL** (`https://<project-ref>.supabase.co`) and **Supabase Anon Key**, then tap **Save & Sync**.
- **Option B — Compile-Time Default in `SupabaseConfig`**:
  Update the default `supabaseUrl` and `supabaseAnonKey` in [`SupabaseClientService.kt`](app/src/main/kotlin/com/example/parishsync/data/remote/SupabaseClientService.kt).
