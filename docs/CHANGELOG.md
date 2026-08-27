# Changelog

All notable changes to ScriptHost will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- "Built-in Examples" import: the add-script dialog offers a picker over the
  18 examples bundled in `assets/builtin_examples/` (synced copies of
  `scripts/examples/`); selecting one installs it immediately. Installing
  `agent_conversation.js` also toasts a hint to configure the API keys
- Settings "Agent" section (between App and the generic config keys):
  labeled "API Base URL" (prefilled with the stored value or
  `https://api.openai.com/v1`) and masked "API Key" inputs with a Save
  button, persisted to ConfigStore under `AGENT_API_URL` / `OPENAI_API_KEY`
- `setStrikeThrough(enabled)` text method on all text-capable components
  (Button, Label, Switch, CheckBox, TextField)
- New example `metronome.js` (BPM slider, accented beats via vibration);
  `agent_conversation.js` reworked into a chat-style UI with per-exchange
  status (model, HTTP outcome, elapsed ms); `todo_list.js` and
  `daily_fitness.js` now use gray + strikethrough for completed items
- On-device API coverage script `tests/api_coverage.js` printing
  `APITEST|PASS|name` / `APITEST|FAIL|name|reason` markers
- Read-only `Device` system information for scripts: `Device.getTime()`
  (epoch ms), `Device.getTimeZone()`, `Device.getDeviceName()`,
  `Device.getMemoryInfo()` (`{ totalMB, availableMB, lowMemory }`),
  `Device.getStorageInfo()` (`{ totalMB, freeMB, usedMB }`), and
  `Device.getSystemInfo()` (`{ androidVersion, sdkVersion, abi,
  supportedAbis }`); no permissions required
- One-shot timed notifications: `Scheduler.scheduleIn(id, delayMs, title,
  message)` and `Scheduler.scheduleAt(id, epochMs, title, message)` backed by
  WorkManager one-time work (inexact, minute-scale precision);
  `Scheduler.cancel(id)` now cancels one-shot schedules too
- Markdown file association: opening a `.md` / `.markdown` file (content or
  file scheme, `text/markdown` or `text/plain`) offers SAM's new
  `MarkdownViewerActivity`, which renders the file with the built-in
  MarkdownRenderer in a sub-screen (X / rightward swipe to close)
- Settings "App" section: debug-mode toggle (script `console.*` messages
  mirrored to Logcat tag `ScriptConsole` when on) and light/dark appearance
  (Follow system / Light / Dark) via `AppCompatDelegate`; both persisted in
  the new `AppSettings` (SharedPreferences `app_settings`, keys `debug_mode` /
  `night_mode`), separate from the script-readable `ConfigStore`
- Fira Sans (SIL OFL 1.1) bundled as the app's default typeface
  (`res/font/fira_sans*.ttf` + `res/font/fira_sans.xml` family, applied via
  the app theme; license text in `assets/OFL-FiraSans.txt`)
- "script aggregation mobile" subtitle under the SAM title in the main
  header and the drawer header
- `Markdown` script view: lightweight built-in Markdown renderer (headings,
  bold/italic/strikethrough, inline and fenced code, lists, blockquotes,
  clickable links, horizontal rules) with `new Markdown(text)` and
  `md.setMarkdown(text)`; no external dependencies

### Changed
- All app UI text is now English; user-facing strings in the main screens
  were moved to `strings.xml`
- Main header bar is taller (~72dp) with exactly one left button at a time:
  ☰ when idle, ✕ at the same spot while a script runs; glyphs unchanged.
  Idle content shows a "Welcome" / "Click Script on left to focus" state
- Drawer bottom text buttons replaced by a compact icon row ("+" add script,
  gear settings) directly below the script list; the drawer header stays
  fixed while the list scrolls
- Script runtime: the console section and Stop Script button are only
  visible in debug mode; the script page host is wrapped in a ScrollView so
  long script content scrolls
- System status bar now matches the theme background with light/dark icon
  contrast via `WindowInsetsControllerCompat` (follows the theme switch)
- Example scripts: `todo_list.js` tasks are per-item CheckBoxes (tap to mark
  done), `daily_fitness.js` gained a "Done today" CheckBox, and
  `scheduled_notify.js` makes the system-notification nature of `Notify.post`
  explicit in its message text
- Cleartext HTTP is now permitted (`android:usesCleartextTraffic="true"`)
  because scripts commonly target local-network devices that only serve
  plain HTTP; scripts should still prefer HTTPS for internet hosts

### Fixed
- `setAlpha`, `setCornerRadius`, and `setTextSize` rejected fractional JS
  numbers (`argument has type float, got java.lang.Double`); the bridge now
  takes `Double` and converts at the use site
- `showToast(message, <non-string>)` threw an empty error; any second
  argument is now tolerated, with only the string `"long"` selecting
  `LENGTH_LONG`
- Missing Gradle wrapper; `./gradlew` now works as documented
- Build configuration for AGP 8.2: enabled `android.useAndroidX` and removed
  legacy repository declarations that conflicted with the settings plugin
  management (previously blocked all builds)
- J2V8 API mismatches: `executeVoidFunction` replaced with the actual
  `V8Function.call` API, and heap-statistics monitoring removed (not exposed
  by J2V8 6.2.1)
- Embedded platform public key was not a valid RSA key; replaced with a real
  generated RSA-2048 key
- Launcher icons referenced by the manifest but never shipped (build failure)
- Missing `VIBRATE` permission while `Device.vibrate()` requires it
- Signature verification was silently skipped for unsigned scripts when
  verification was enabled; it is now enforced
- Timer IDs always returned `0` and `clearTimeout`/`clearInterval` were missing
- UI bridge touched views from background threads; view creation and mutation
  now happen on the main thread
- V8 callback/object access is marshaled to the main thread; J2V8 enforces
  thread affinity and previously threw on foreign-thread access
- Sensor callbacks fired on background threads; events are now posted to the
  main thread, and `unregister()` stops sensor listeners
- Nullable runtime references could crash bridges; guarded with `?: return`
  and `?: null` fallbacks
- `Network.get`/`Network.post` treated only HTTP 200 as success; non-2xx
  responses are now surfaced as errors
- Stale claims about a 50MB memory limit removed (heap statistics are not
  exposed by J2V8 6.2.1; only the 30-second execution timeout is enforced)
- Storage path traversal now rejected: filenames are confined to the app's
  private `filesDir` via canonical-path checking (`../` escapes and absolute
  paths outside `filesDir` fail closed with `null`/`false`)

### Changed
- System ActionBar removed (`NoActionBar` theme); MainActivity now shows a
  single slim in-app header bar — a custom ☰ hamburger button (replacing
  `ActionBarDrawerToggle`) plus the title. The title shows
  "SAM / script aggregation mobile" when idle and swaps to ✕ + the running
  script's name while a script runs (no more stacked bars); the fragment
  header merged into the activity-level header via a new
  `Host.onScriptSessionStarted` callback. Edge-swipe drawer opening kept
- Launcher icon replaced: white background with a compact black "SAM"
  wordmark; `scripts/generate_launcher_icons.py` now draws 5x7 bitmap glyphs
  and also generates adaptive-icon foreground PNGs
  (`@mipmap/ic_launcher_foreground` over a white
  `@color/ic_launcher_background`)
- Example scripts no longer implement their own settings UIs:
  `sub_screens.js` drops the Settings pseudo-page (Notes instead), and
  `ui_controls.js` uses neutral demo labels instead of a fake settings form
- App renamed to **SAM** (scripts_aggregation_mobile); package stays
  `com.scripthost`
- Main UI rebuilt around a `DrawerLayout`: a Discord-style left drawer
  (~280dp) holds the "SAM" header, the "脚本列表" script list, and
  bottom-pinned "添加脚本" / "设置" buttons; the drawer starts open on cold
  start, dims the content while open, and reopens via edge swipe or the
  hamburger icon
- The script runtime is embedded in MainActivity's content area as
  `ScriptRuntimeFragment`; `ScriptRuntimeActivity` is now a thin standalone
  host for the same fragment. Tapping a script runs it in place; Stop Script
  returns to the empty state
- Settings is a full-screen sub-screen with a top-left X and rightward-swipe
  to close, and gains a per-installed-script section showing declared and
  granted permissions with per-permission revoke
- Script sub-pages (`UI.pushPage`) and the script editor follow the same
  sub-screen chrome (X at top-left, rightward swipe to close/pop)
- `ScriptManager` now takes an injectable storage directory (plus a `Context`
  convenience constructor), making it testable on the JVM
- `SignatureVerifier` accepts an injectable public key so sign/verify
  round-trips are consistent
- Unit tests moved into `android/app/src/test/` where Gradle actually runs
  them, replacing placeholder assertions with real behavioral tests
- `run_all_tests.py` no longer hard-codes paths from another machine
- `JavaScriptEngine.execute()` now runs on the main thread to satisfy J2V8
  runtime thread affinity
- All Development Roadmap phases (1-7) in README.md completed and ticked
- Documentation synced with the new features (README, docs/API.md,
  docs/EXAMPLES.md, docs/SECURITY.md, docs/QUICKSTART.md, docs/STATUS.md,
  docs/PROJECT_SUMMARY.md)
- Documentation reorganized under `docs/` per standard conventions;
  `README.md` and `LICENSE` remain at the repository root
- Bridges now enforce per-script permissions: every capability check requires
  the permission to be declared by the running script AND granted at the
  system level (`PermissionManager.hasScriptPermission`)
- `ConfigStore` values are encrypted at rest (AES-256-GCM with a per-value IV,
  key managed by AndroidKeyStore); legacy plaintext values migrate on next save
- `printStackTrace` replaced by an injectable `Logger`
  (`AndroidLogger` / `ConsoleLogger`)
- README: development roadmap and cross-platform claims removed; project
  structure and technology stack synced with the actual repository

### Removed
- Script category classification: the `ScriptCategory` enum, `Script.category`,
  `ScriptManager.getScriptsByCategory`, the category spinner/filter in the main
  UI and the category selector in the script editor. Legacy metadata/export
  files that still carry a `category` key are parsed tolerantly (key ignored)
- Main-screen search box and the old top-level Settings button (superseded by
  the drawer)
- Development-process documents (roadmap/status artifacts): `docs/STATUS.md`,
  `docs/TEST_REPORT.md`, `docs/FINAL_SUMMARY.md`, `docs/PROJECT_SUMMARY.md`,
  `docs/deep-research-report.md`
- Unused `androidx.security:security-crypto` dependency

### Added
- Real script file import: "添加脚本" → "从文件导入" opens the SAF document
  picker, copies the chosen `.js`/JSON package into app storage, and installs
  it via `ScriptManager.installScriptFromFile`; the existing `.js` VIEW
  intent-filter imports through the same flow
- Script export now writes the script source to the app-specific external
  directory (`exports/`) and shows the path
- `PermissionManager.revokePermission(scriptId, permission)` for
  single-permission revoke
- Expanded native UI bridge with new components: `CheckBox`, `Spinner`,
  `ProgressBar`, and `Layout` (vertical/horizontal container)
- Common configuration methods on every component: `setVisible`, `setEnabled`,
  `setPadding`, `setMargin`, `setWidth`, `setHeight`, `setAlpha`,
  `setBackgroundColor`, `setCornerRadius`, and `getViewId` (dimensions support
  `-1` = match parent and `-2` = wrap content)
- Text configuration methods on text-capable components: `setTextSize`,
  `setTextColor`, `setBold`, `setItalic`, `setTextAlign`, and `setAllCaps`
- Enhanced component APIs: `TextField.setHint`/`setHintTextColor`/
  `setInputType`/`setMaxLength`, `ListView.setSelection`,
  `ImageView.setImageBase64`/`setScaleType`, `Switch.setText`/`setChecked`/
  `getChecked`, `Slider.getValue`/`setMax`/`setMin`,
  `ScrollView.addView`/`removeView`/`setFillViewport`
- `UI.addView` reparents views, enabling container nesting; new
  `UI.setBackgroundColor` for the root container
- Dialog helpers: `showConfirm`, `showPrompt`, `showListPicker`, and a
  two-argument `showToast(message, "long")` form
- Example script `ui_controls.js` demonstrating the expanded UI interface
- `test_examples.py` and `validate.py` updated to cover the new UI components
- Initial project structure
- JavaScript engine integration with J2V8
- Native UI bridge layer with components (Button, Label, TextField, ListView, Switch, Slider)
- System bridge for network, storage, and sensors
- Permission management system
- Script signature verification
- Script manager for installation and updates
- Example scripts (Hello World, Counter, Todo List, Network Request, Sensors, Storage)
- Comprehensive API documentation
- Security documentation
- Testing framework with unit and integration tests
- Android app with MainActivity, ScriptEditorActivity, and ScriptRuntimeActivity
- Configuration interface: Settings screen to add, edit, and delete API keys
  and settings
- `ConfigStore` (JSON-backed key/value store in app-private storage)
- `Config` bridge for scripts: `Config.get(key)` and `Config.keys()`, gated
  behind the `CONFIG` permission
- Header overloads for `Network.get(url, headers, callback)` and
  `Network.post(url, headers, body, callback)` for authenticated API calls
- Example scripts: `agent_conversation.js` (wrapped agent conversation) and
  `server_monitor.js` (server health monitoring)
- `ConfigStoreTest` unit tests (8 tests)
- `Chart` UI widget (line/bar, custom colors, axis labels) — dependency-free
  `SimpleChartView`
- `Notify.post(...)` and `Scheduler.scheduleDaily(...)` / `Scheduler.cancel(...)`
  bridges backed by a WorkManager daily worker (POST_NOTIFICATIONS runtime
  permission handled on API 33+)
- `SSH` bridge (`connect` / `exec` / `disconnect`) over JSch, gated by the new
  `SSH` script permission
- Example scripts: `monitor_port_chart.js`, `daily_fitness.js`,
  `stock_trends.js`, `tmux_remote.js`
- Test stack: Robolectric, Truth, mockito-kotlin, WorkManager Test; new suites
  for PermissionManager, NotificationBridge, DailyNotificationWorker,
  SimpleChartView, and AesGcmValueCipher; existing suites modernized
- Sub-screen page stack on the `UI` namespace: `UI.pushPage()` /
  `UI.popPage()` / `UI.pageDepth()`; the device back button pops a page first
  and only closes the script at the root page; single-page scripts are
  unaffected
- Example script `sub_screens.js` (master/detail sub-screen navigation demo)
- New test suites `UIBridgeTest` (4 tests), `SystemBridgeTest` (10 tests),
  `ConfigBridgeTest` (3 tests), and 4 new `ScriptManagerTest` cases (corrupt
  metadata recovery, export unknown id, uninstall unknown id, malformed JSON
  package) — 105 JVM/Robolectric tests total

### Security
- Sandbox isolation for script execution
- Resource limits (30s execution timeout)
- Permission system with dangerous permission flags
- RSA-2048 signature verification
- SHA-256 hash verification for local scripts
- `CONFIG` permission (auto-granted, read-only access to configured API keys
  and settings)
- `docs/SECURITY.md` documents the Config storage model, API-key handling,
  and updated permission/API whitelist
- Per-script permission enforcement: bridges call
  `PermissionManager.hasScriptPermission(scriptId, permission)`, so a
  capability requires both a script declaration and a system-level grant
- Config values encrypted at rest (AES-256-GCM, AndroidKeyStore-managed key);
  `NOTIFICATIONS` maps to the `POST_NOTIFICATIONS` runtime permission on
  API 33+; SSH credentials are supplied by scripts and never persisted

## Version History

### Version Numbering

- **Major version** (X.0.0): Breaking changes, major features
- **Minor version** (0.X.0): New features, backward compatible
- **Patch version** (0.0.X): Bug fixes, security patches

### Release Schedule

- **Major releases**: Annually
- **Minor releases**: Quarterly
- **Patch releases**: As needed for critical bugs/security

### Support Policy

- **Current major version**: Full support
- **Previous major version**: Security updates for 1 year
- **Older versions**: No support

## Migration Guides

### Upgrading from 0.x to 1.0

TBD

## Contributors

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for a list of contributors.

## Links

- [GitHub Repository](https://github.com/scripthost/scripthost)
- [Documentation](https://scripthost.dev/docs)
- [Issue Tracker](https://github.com/scripthost/scripthost/issues)
- [Release Notes](https://github.com/scripthost/scripthost/releases)
