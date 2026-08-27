# Script Design Guidelines

Conventions every SAM script (bundled example or your own) should follow.
They exist because each one was learned from a real bug or a bad user
experience. `test_integration.py` enforces the checkable ones — run it
before shipping a script.

## Configuration

- **Declare configuration, never build a settings UI.** Scripts must not
  render their own settings screens or fake preference panels. Declare
  configurable fields at startup with `Config.schema(...)`; they appear in
  the app's Settings under the script's title after the first run.
- **Keep values free-form.** Any field may hold arbitrarily long text.
  Use `"multiline"` for long structured content (plans, lists, templates);
  the Settings UI shows it as a compact preview and edits it in a
  full-screen editor with a top-right Save button.
- **Structured plans use the documented YAML subset** (see `daily_fitness.js`:
  `cycle_days` / `cycle_start` scalars, a `schedule:` list of
  `- day: N` + `modules: [...]` entries, and `modules:` with
  `- action:` items and their `notes:` annotations). Parse config
  defensively: unknown or malformed input must never throw.
- **List items are plain text.** Never emit markdown or other markup
  (`**bold**`, backticks, `#`) into list/checklist rows — the list widgets
  render text literally. Strip markup when importing external content.
- **Distinguish actions from annotations.** Checkable items get
  checkboxes; explanatory notes do not. Render notes in a compact,
  subdued style (smaller, gray, indented) with no checkbox.

## Presentation

- **Set a display title.** Call `UI.setTitle("...")` once at startup with a
  proper English title ("Daily Fitness", not "daily_fitness"). When unset,
  the top bar falls back to a prettified script name. Never add a big
  in-page title Label — the top bar already shows the title. Section
  labels for content structure are fine.
- **Restore colors, don't hardcode them.** To undo a custom text color
  call `setTextColor("")` — it restores the theme default captured when
  the view was created. Never "restore" with a fixed color like `#212121`;
  it is wrong in dark mode. Custom foregrounds/backgrounds should stay
  readable in both light and dark themes.
- **Long content must scroll.** If a script can produce more than one
  screen of content, put it in a scrollable structure and verify with 30+
  items.
- **Feedback on actions.** Use `showToast` for quick confirmations and a
  status Label for ongoing state (e.g. "2 of 7 done today").

## Robustness

- **Never crash on bad input.** Guard parsing, network calls, and empty
  user input; show errors as inline messages or toasts, not exceptions.
- **Declare permissions** in the header comment (`// Permissions: ...`)
  and only what the script actually uses. Imported scripts get their
  permissions inferred from API usage — keep usage explicit.
- **Persist state deliberately.** Date- or session-scoped state goes in
  script storage with a key that encodes the scope (e.g.
  `fitness_state_<yyyy-mm-dd>.json`), so it resets when the scope rolls
  over. Positional indexes into user-editable content are fragile —
  prefer stable identifiers when the content can be edited mid-scope.
- **Examples stay ASCII** in `scripts/examples/` (the test runner reads
  them cross-platform). Escape non-ASCII literals as `\uXXXX` when a
  parser keyword genuinely needs another language, and keep comments in
  English.

## Lifecycle

- **Pages, not modals.** Multi-step flows use `UI.pushPage()` /
  `UI.popPage()`; the system chrome supplies close affordances (✕,
  right-fling, Back). Scripts must not implement their own close buttons
  or navigation bars.
- **Clean up recurring resources.** Always `clearInterval` before
  re-creating a timer, `Sensor.stop()` when done, and avoid stacking
  listeners on re-entry.
