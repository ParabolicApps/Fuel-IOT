Here are the logical flow Mermaid diagrams for the core functions in this project, organized by file.
1. MainActivity.java
A. One-Time Demo Data Initialization
This logic ensures the database is populated for first-time users.
Kotlin
```mermaid
graph TD
    A[App Start] --> B{Demo Data Initialized?}
    B -- Yes --> C[Skip Init]
    B -- No --> D{Real Mode ON?}
    D -- Yes --> E[Skip Init]
    D -- No --> F[Loop 30 Days]
    F --> G[Generate Random IN/OUT Logs]
    G --> H[db.addLogs]
    H --> I{Loop Finished?}
    I -- No --> F
    I -- Yes --> J[Set Flag: demo_data_initialized]
```
B. Scoped Storage DB Export
Handles exporting the database to the Downloads folder on Android 10+.
Kotlin
```mermaid
graph TD
    A[Click Export] --> B{DB File Exists?}
    B -- No --> C[Show Error Toast]
    B -- Yes --> D{Android 10+?}
    D -- Yes --> E[Use MediaStore Downloads URI]
    D -- No --> F[Use Legacy Public Directory]
    E --> G[Open Output Stream]
    F --> G
    G --> H[Copy Bytes from data.db]
    H --> I[Show Success Toast]
```
2. Frag1FragmentActivity.java (Home)
A. Real-time IOT Data Processing
Processes incoming fuel levels and detects refueling events.
Kotlin
```mermaid
graph TD
    A[Receive IOT Data String] --> B[Parse Double Value]
    B --> C{Value > Current + 1.0?}
    C -- Yes --> D[Start Refill Animation]
    D --> E[Update status: Fueling...]
    C -- No --> F{Is Refilling?}
    F -- Yes --> G[Update Fueling Counter]
    G --> H[Restart 30s Timeout]
    F -- No --> I[Update status: Engine ON]
    I --> J[Update UI Metrics & WaveView]
```
B. Refill Termination & Summary
Detects when fueling stops and presents the results.
Kotlin
```mermaid
graph TD
    A[30s Inactivity Detected] --> B[Set isRefilling = false]
    B --> C[Stop Nozzle Bip Animation]
    C --> D[Reset status: Engine ON]
    D --> E[Calculate total volume added]
    E --> F[Show MaterialAlertDialog Summary]
```
3. Frag2FragmentActivity.java (Analytics)
A. Interactive Tooltip Logic
Calculates coordinates and styles the tooltip dynamically on point selection.
Java
```mermaid
graph TD
    A[Point Selected] --> B[Fetch X/Y Screen Raw Coords]
    B --> C[Update Tooltip Text: Date & Value]
    C --> D[Check Theme: Set Border Color]
    D --> E[Set Tooltip Visibility: VISIBLE]
    E --> F[Animate Box Position above point]
    F --> G[Draw Vertical Selection Line to Axis]
```
3. SQLiteHandler.java
A. Log Insertion & Intelligent Aggregation
Aggregates daily totals while supporting historical date insertion.
Kotlin
```mermaid
graph TD
    A[addLogs Request] --> B[Insert into TABLE_LOG]
    B --> C{Row exists in TABLE_TOTAL for Date?}
    C -- Yes --> D[Calculate Delta from last log]
    D --> E[Update TABLE_TOTAL: add delta]
    C -- No --> F[Calculate Delta from last log]
    F --> G[Insert new row into TABLE_TOTAL]
```
3. HttpBackgroundWorker.java
A. Background Data Fetching & Notification
Manages the hardware connection and triggers system-level alerts.
Kotlin
```mermaid
graph TD
    A[doInBackground] --> B[Fetch Data from Server URL]
    B --> C[Split IN/OUT Data]
    C --> D{Fuel Level <= Threshold?}
    D -- Yes --> E[Trigger Notification: Low Fuel]
    D -- No --> F{Is new data?}
    F -- Yes --> G[Trigger Notification: Refill Detected]
    G --> H[Broadcast Data to UI]
```
3. SettingsFragment.java
A. Dynamic Theme Switching
Instantly applies theme changes and resets navigation for visual confirmation.
Kotlin
```mermaid
graph TD
    A[Toggle Dark Mode Switch] --> B{Switch State?}
    B -- ON --> C[AppCompatDelegate: NIGHT_YES]
    B -- OFF --> D[AppCompatDelegate: NIGHT_NO]
    C --> E[MainActivity: Select Home Tab]
    D --> E[MainActivity: Select Home Tab]
    E --> F[Fragment Transaction: Alpha Transition]
```
