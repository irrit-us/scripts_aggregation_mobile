// Example 11: Daily Fitness Checklist
// Shows only TODAY's part of a configurable training cycle, as a checklist.
//
// Plan config format (FITNESS_PLAN, a small YAML subset, set in Settings
// after the first run):
//
//   cycle_days: 14            # any cycle length; 7 = weekly plan
//   cycle_start: 2026-08-24   # optional anchor date; first run otherwise
//   schedule:
//     - day: 1
//       modules: [Warm-up, Strength A, Core A]
//     - day: 2
//       modules: [Core B]
//   modules:
//     Strength A:
//       - action: Goblet squat: 3 sets x 10
//         notes:
//           - keep the dumbbells at your sides
//       - action: Push-ups: 4 sets x 5
//
// Each cycle day expands its modules: "action" lines become CheckBoxes,
// "notes" lines become compact gray annotations under their action (no
// checkbox). Done state is stored per date in script storage
// (fitness_state_<date>.json), so a new day always starts unchecked.
// Permissions: READ_STORAGE, WRITE_STORAGE, CONFIG
UI.setTitle("Daily Fitness");


Config.schema(JSON.stringify([
    { key: "FITNESS_PLAN", label: "Plan (YAML, cycle schedule + modules)", type: "multiline" }
]));

let DEFAULT_PLAN = [
    "cycle_days: 7",
    "schedule:",
    "  - day: 1",
    "    modules: [Strength A]",
    "  - day: 2",
    "    modules: [Core A]",
    "  - day: 3",
    "    modules: [Strength B]",
    "  - day: 4",
    "    modules: [Core A]",
    "  - day: 5",
    "    modules: [Strength A]",
    "  - day: 6",
    "    modules: [Core B]",
    "  - day: 7",
    "    modules: [Recovery]",
    "modules:",
    "  Strength A:",
    "    - action: Goblet squat: 3 sets x 10",
    "    - action: Push-ups: 3 sets x 8",
    "  Strength B:",
    "    - action: Reverse lunge: 3 sets x 8 per side",
    "    - action: Row: 3 sets x 12 per side",
    "  Core A:",
    "    - action: Dead bug: 2 sets x 8 per side",
    "      notes:",
    "        - keep your lower back near the floor",
    "    - action: Plank: 2 sets x 30 s",
    "  Core B:",
    "    - action: Side plank: 2 sets x 20 s per side",
    "    - action: Bird dog: 2 sets x 8 per side",
    "  Recovery:",
    "    - action: Easy breathing: 5 slow breaths",
    "    - action: Light stretch: 5 minutes"
].join("\n");

// --- Restricted YAML subset parser (see format above) ---
function parseYamlPlan(text) {
    let plan = { cycleDays: 14, cycleStart: null, schedule: {}, modules: {} };
    let lines = text.replace(/\r/g, "").split("\n");
    let section = null, currentModule = null, currentItem = null, currentEntry = null, inNotes = false;

    function unquote(s) { return s.trim().replace(/^['"]|['"]$/g, ""); }

    for (let i = 0; i < lines.length; i++) {
        let raw = lines[i];
        let line = raw.trim();
        if (line === "" || line.charAt(0) === "#") continue;

        if (line === "schedule:") { section = "schedule"; currentModule = null; currentEntry = null; continue; }
        if (line === "modules:") { section = "modules"; currentModule = null; continue; }

        let scalar = line.match(/^([A-Za-z_]+):\s*(.+)$/);
        if (scalar && (scalar[1] === "cycle_days" || scalar[1] === "cycle_start")) {
            if (scalar[1] === "cycle_days") plan.cycleDays = parseInt(scalar[2]) || 14;
            else plan.cycleStart = unquote(scalar[2]);
            continue;
        }

        if (section === "schedule") {
            let dayMatch = line.match(/^-\s*day:\s*(\d+)\s*$/);
            if (dayMatch) { currentEntry = parseInt(dayMatch[1]); plan.schedule[currentEntry] = []; inNotes = false; continue; }
            let inlineModules = line.match(/^modules:\s*\[(.*)\]\s*$/);
            if (inlineModules && currentEntry !== null) {
                plan.schedule[currentEntry] = inlineModules[1].split(",")
                    .map(unquote).filter(function(s) { return s !== ""; });
                continue;
            }
            let plain = line.match(/^-\s+(.+)$/);
            if (plain && currentEntry !== null) { plan.schedule[currentEntry].push(unquote(plain[1])); continue; }
        } else if (section === "modules") {
            // "notes:" must be checked before the module-key rule — it also
            // ends with ":" and would otherwise become a bogus module.
            if (line === "notes:") { inNotes = true; continue; }
            if (line.charAt(0) !== "-" && line.slice(-1) === ":") {
                currentModule = unquote(line.slice(0, -1));
                if (!plan.modules[currentModule]) plan.modules[currentModule] = [];
                currentItem = null; inNotes = false;
                continue;
            }
            let actionMatch = line.match(/^-\s*action:\s*(.+)$/);
            if (actionMatch && currentModule) {
                currentItem = { action: unquote(actionMatch[1]), notes: [] };
                plan.modules[currentModule].push(currentItem);
                inNotes = false;
                continue;
            }
            let plain = line.match(/^-\s+(.+)$/);
            if (plain && currentModule) {
                if (inNotes && currentItem) {
                    currentItem.notes.push(unquote(plain[1]));
                } else {
                    // Plain item in a simple module: treat as an action
                    currentItem = { action: unquote(plain[1]), notes: [] };
                    plan.modules[currentModule].push(currentItem);
                }
                continue;
            }
        }
    }
    return plan;
}

function dateKey(d) {
    let m = "" + (d.getMonth() + 1);
    let day = "" + d.getDate();
    return d.getFullYear() + "-" + (m.length < 2 ? "0" : "") + m + "-" + (day.length < 2 ? "0" : "") + day;
}

// --- Resolve today inside the cycle ---
let now = new Date();
let plan = parseYamlPlan(Config.get("FITNESS_PLAN") || DEFAULT_PLAN);

let anchor = plan.cycleStart;
if (!anchor) {
    // Persist the first-run date as the cycle anchor so the cycle is stable
    try { anchor = Storage.readFile("fitness_anchor.txt"); } catch (e) { anchor = null; }
    if (!anchor) {
        anchor = dateKey(now);
        Storage.writeFile("fitness_anchor.txt", anchor);
    }
}
let anchorDate = new Date(anchor + "T00:00:00");
let todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
let diffDays = Math.max(0, Math.floor((todayMidnight - anchorDate) / 86400000));
let dayOfCycle = (diffDays % plan.cycleDays) + 1;

let moduleNames = plan.schedule[dayOfCycle] || [];

// Module lookup: exact, then prefix, then substring
function findModule(name) {
    if (plan.modules[name]) return name;
    for (let moduleName in plan.modules) {
        if (moduleName.indexOf(name) === 0) return moduleName;
    }
    for (let moduleName in plan.modules) {
        if (moduleName.indexOf(name) >= 0) return moduleName;
    }
    return null;
}

// --- State (per date) ---
let stateFile = "fitness_state_" + dateKey(now) + ".json";
let doneIndexes = [];
try {
    let raw = Storage.readFile(stateFile);
    if (raw) doneIndexes = JSON.parse(raw) || [];
} catch (e) { doneIndexes = []; }
if (!Array.isArray(doneIndexes)) doneIndexes = [];

function saveState() { Storage.writeFile(stateFile, JSON.stringify(doneIndexes)); }

// --- Render: today only ---
let DAY_NAMES = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
let weekday = DAY_NAMES[(now.getDay() + 6) % 7];

let header = new Label("Day " + dayOfCycle + " of " + plan.cycleDays + " · " + weekday +
    (moduleNames.length > 0 ? " · " + moduleNames.join(" + ") : " · Rest day"));
header.setTextSize(18);
header.setBold(true);
UI.addView(header);

let statusLabel = new Label("");
statusLabel.setTextSize(13);
statusLabel.setTextColor("#888888");
UI.addView(statusLabel);

let actionCount = 0;

function refreshStatus() {
    statusLabel.setText(doneIndexes.length + " of " + actionCount + " done today");
}

for (let m = 0; m < moduleNames.length; m++) {
    let found = findModule(moduleNames[m]);
    if (!found) continue;

    let sectionLabel = new Label(found);
    sectionLabel.setTextSize(15);
    sectionLabel.setBold(true);
    sectionLabel.setTextColor("#007AFF");
    UI.addView(sectionLabel);

    let moduleItems = plan.modules[found];
    for (let k = 0; k < moduleItems.length; k++) {
        (function(idx, item) {
            let check = new CheckBox(item.action);
            if (doneIndexes.indexOf(idx) >= 0) {
                check.setChecked(true);
                check.setStrikeThrough(true);
                check.setTextColor("#888888");
            }
            check.setOnChange(function(checked) {
                check.setStrikeThrough(checked);
                check.setTextColor(checked ? "#888888" : "");
                let at = doneIndexes.indexOf(idx);
                if (checked && at < 0) doneIndexes.push(idx);
                if (!checked && at >= 0) doneIndexes.splice(at, 1);
                saveState();
                refreshStatus();
            });
            UI.addView(check);

            // Compact annotation lines under the action: no checkbox
            for (let n = 0; n < item.notes.length; n++) {
                let note = new Label(item.notes[n]);
                note.setTextSize(12);
                note.setTextColor("#888888");
                note.setMargin(36, 0, 0, 2);
                UI.addView(note);
            }
        })(actionCount, moduleItems[k]);
        actionCount++;
    }
}

if (actionCount === 0) {
    let rest = new Label("Rest day - nothing scheduled. The plan can be configured in Settings.");
    rest.setTextColor("#888888");
    UI.addView(rest);
}

refreshStatus();
console.log("Daily checklist: day " + dayOfCycle + "/" + plan.cycleDays +
    ", " + actionCount + " actions, " + doneIndexes.length + " done");
