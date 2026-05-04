# StockScout — Android Take-Home

A small Android inventory app that demonstrates item-alias resolution, barcode
scanning, offline persistence, and resilient background sync.

---

## How to Build & Run

### Prerequisites
| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or later |
| JDK | 17 |
| Android Gradle Plugin | 8.x |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |

### Steps
1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/StockScout.git
   cd StockScout
   ```
2. Open the project in Android Studio (`File → Open`).
3. Let Gradle sync finish.
4. Select a device or emulator (API 24+) and press **Run ▶**.

### Run Unit Tests
```bash
./gradlew test
```
The alias-resolution and offline-queue test suites are in:
- `app/src/test/.../AliasResolutionTest.java`
- `app/src/test/.../OfflineQueueTest.java`

---

## Mock REST Endpoints

Both endpoints are hosted on [MockAPI.io](https://mockapi.io).

| Purpose | Method | URL |
|---------|--------|-----|
| Fetch items | `GET` | `https://67f0e4f02a80b06b88999999.mockapi.io/api/v1/items` |
| Record pick | `POST` | `https://67f0e4f02a80b06b88999999.mockapi.io/api/v1/picks` |

### Item schema returned by `GET /items`
```json
{
  "itemCode":      "WGT-A",
  "name":          "Wireless Headphones",
  "unitOfMeasure": "each",
  "quantity":      25,
  "aliases":       ["123456789012", "5901234123457", "WH-1000XM4"]
}
```

### Pick payload sent to `POST /picks`
```json
{
  "itemCode":    "WGT-A",
  "newQuantity": 24,
  "timestamp":   1714900000000
}
```

> **Fallback:** If MockAPI is unreachable, the app shows cached data from the
> last successful fetch (Room database).

---

## Libraries Chosen & Why

| Library | Why |
|---------|-----|
| **Retrofit 2** | Type-safe HTTP client; minimal boilerplate for REST calls; widely adopted in Android codebases. |
| **OkHttp** | Sits under Retrofit; provides connection pooling, logging interceptor, and automatic retry on connection failure. |
| **Gson** | Simple JSON ↔ Java model mapping; sufficient for the flat data model here; already familiar to most Java devs. |
| **Room** | Google's SQLite ORM; annotation-based DAOs, compile-time SQL validation, and first-class support for `List<String>` via `@TypeConverters`. Offline persistence without writing raw SQL. |
| **WorkManager** | Guaranteed background execution for pending-pick sync; respects `NetworkType.CONNECTED` constraint so it only fires when online; survives app restarts and device reboots. Chosen over bare `Service` because it handles all the scheduling edge cases automatically. |
| **ML Kit Barcode Scanning** | On-device, no API key, supports UPC-A, EAN-13, Code-128, GS1 composite codes, and QR. Works offline. Integrates cleanly with CameraX for live preview. Chosen over ZXing because it's actively maintained by Google and provides better accuracy on modern devices. |
| **CameraX** | Jetpack camera library; handles lifecycle, permission, and resolution automatically. Required by ML Kit's `ImageAnalysis` use case. |
| **Robolectric** | Lets unit tests run on JVM (no emulator needed) while still having access to Android framework classes used in the GS1 parser and alias resolution logic. |

---

## Architecture Overview

```
MainActivity  ──search/scan──►  ItemRepository  ──►  ApiService (Retrofit)
     │                               │                    │
     │                               ▼                    │
     │                          AppDatabase (Room)        │
     │                          ┌─────────────┐           │
     │                          │  items      │◄──────────┘ (cached on fetch)
     │                          │  picks      │
     │                          └─────────────┘
     │                               │
     ▼                               ▼
ItemDetailActivity  ──pick──►  SyncManager  ──►  POST /picks
                                    │
                                    ▼
                              WorkManager (periodic, network-gated)
```

### Alias Resolution Logic (`ItemRepository.resolveItem`)
1. Check if input is a GS1 composite string (has parenthesised Application
   Identifiers like `(01)…`).  If so, extract the GTIN-14 and use *that* for
   lookup.
2. Try exact match on `itemCode`.
3. Try match on any entry in the item's `aliases` list.

Plain 12/13-digit UPC-A / EAN-13 barcodes are treated as regular aliases and
matched in step 3 — they are **not** confused with GS1 strings.

### Offline Pick Flow
1. User taps **Pick Item** → quantity decremented in Room immediately.
2. A `Pick` record (synced = false) is inserted into the `picks` table.
3. `SyncManager` attempts an immediate POST if network is available.
4. If the POST fails (offline, server error), `retryCount` is incremented.
   The pick stays in the queue.
5. When connectivity is restored, `ConnectivityManager.NetworkCallback`
   fires `syncPendingPicks()` automatically.
6. `WorkManager` also fires `SyncWorker` every 15 minutes (minimum interval)
   as a safety net, requiring `NetworkType.CONNECTED`.
7. After `MAX_RETRIES` (3) consecutive failures, the pick is marked as
   permanently failed so it no longer blocks the queue; a log entry is written
   for diagnostics.

---

## Tradeoffs & Known Limitations

| Area | Decision | Tradeoff |
|------|----------|----------|
| **Sync polling** | `ItemDetailActivity` polls `getPendingSyncCount()` every 3 s via `Handler.postDelayed` | Simple to implement; slightly wasteful on battery. A `LiveData` / `Flow` observer from Room would be cleaner but adds complexity beyond the scope. |
| **GS1 AI parentheses** | Only the `(NN)value` form is recognised as GS1 | Some scanners emit FNC1 (`\x1D`) delimited strings without parentheses. A production parser would handle both forms. |
| **No ViewModel / MVVM** | Activities talk directly to repository | Keeps the codebase flat and readable for a take-home; not suitable for production rotation-survival. |
| **MockAPI.io** | Free tier; shared resource | Rate limits apply. If the endpoint returns 429, the app falls back to cached data transparently. |
| **Retry sentinel (`retryCount = -1`)** | Permanently-failed picks are marked `synced=true` with retryCount=-1 to remove them from the pending queue | A separate `failed` boolean column would be cleaner but requires a schema migration. |