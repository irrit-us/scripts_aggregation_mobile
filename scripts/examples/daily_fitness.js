// Example 11: Daily Fitness Checklist
// Shows only TODAY's part of a 14-day training cycle, as a checklist.
//
// Plan config format (FITNESS_PLAN_MD, set in Settings after first run):
//   - two schedule sections ("### Week 1" / "### Week 2", the Chinese
//     week names also match) with one line per day:
//     "- [ ] <DayName>: <ModuleA>+<ModuleB>" where DayName is Mon..Sun
//     or the Chinese weekday names
//   - one "# <ModuleName>" section per module holding its "- [ ]" items
// Each day the schedule of the current weekday expands into its module
// checklists. Week 1 and week 2 alternate by ISO week parity, giving a
// 14-day cycle. Days whose schedule mentions strength training also
// prepend the warm-up module.
//
// Done state is stored per date in script storage (fitness_state_<date>.json)
// so a new day always starts unchecked.
// Permissions: READ_STORAGE, WRITE_STORAGE, CONFIG

Config.schema(JSON.stringify([
    { key: "FITNESS_PLAN_MD", label: "Plan (14-day markdown checklist)", type: "multiline" }
]));

let DEFAULT_PLAN = [
    "### Week 1",
    "- [ ] Mon:Strength A",
    "- [ ] Tue:Core A",
    "- [ ] Wed:Strength B",
    "- [ ] Thu:Core A",
    "- [ ] Fri:Strength A",
    "- [ ] Sat:Core B",
    "- [ ] Sun:Recovery",
    "### Week 2",
    "- [ ] Mon:Strength B",
    "- [ ] Tue:Core A",
    "- [ ] Wed:Strength A",
    "- [ ] Thu:Core B",
    "- [ ] Fri:Strength B",
    "- [ ] Sat:Core A",
    "- [ ] Sun:Recovery",
    "# Strength A",
    "- [ ] Goblet squat: 3 sets x 10",
    "- [ ] Push-ups: 3 sets x 8",
    "# Strength B",
    "- [ ] Reverse lunge: 3 sets x 8 per side",
    "- [ ] Row: 3 sets x 12 per side",
    "# Core A",
    "- [ ] Dead bug: 2 sets x 8 per side",
    "- [ ] Plank: 2 sets x 30 s",
    "# Core B",
    "- [ ] Side plank: 2 sets x 20 s per side",
    "- [ ] Bird dog: 2 sets x 8 per side",
    "# Recovery",
    "- [ ] Easy breathing: 5 slow breaths",
    "- [ ] Light stretch: 5 minutes"
].join("\n");

let DAY_NAMES_CN = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];
let DAY_NAMES_EN = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function dayIndexFromName(name) {
    for (let i = 0; i < 7; i++) {
        if (name === DAY_NAMES_CN[i] || name === DAY_NAMES_EN[i]) return i + 1;
    }
    return 0;
}

// Parse the plan into { weeks: {1|2: {day: text}}, modules: {name: [items]} }
function parsePlan(md) {
    let weeks = { 1: {}, 2: {} };
    let modules = {};
    let section = null;      // "week1" | "week2" | "module"
    let currentModule = null;
    let lines = md.replace(/\r/g, "").split("\n");
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();
        if (line === "") continue;
        if (line.indexOf("###") === 0) {
            if (line.indexOf("第二周") >= 0 || /week\s*2/i.test(line)) { section = "week2"; }
            else if (line.indexOf("第一周") >= 0 || /week\s*1/i.test(line)) { section = "week1"; }
            continue;
        }
        if (line.charAt(0) === "#") {
            section = "module";
            currentModule = line.replace(/^#+\s*/, "").trim();
            if (!modules[currentModule]) modules[currentModule] = [];
            continue;
        }
        if (line.indexOf("- [") !== 0) continue;
        let text = line.replace(/^- \[[ xX]\]\s*/, "").trim().replace(/\*\*/g, "");
        if (section === "week1" || section === "week2") {
            let m = text.match(/^(周[一二三四五六日]|Mon|Tue|Wed|Thu|Fri|Sat|Sun)[：:]\s*(.+)$/);
            if (m) {
                let day = dayIndexFromName(m[1]);
                if (day > 0) weeks[section === "week1" ? 1 : 2][day] = m[2].trim();
            }
        } else if (section === "module" && currentModule) {
            modules[currentModule].push(text);
        }
    }
    return { weeks: weeks, modules: modules };
}

function isoWeekNumber(d) {
    let t = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    let day = (t.getDay() + 6) % 7; // Mon=0
    t.setDate(t.getDate() - day + 3);
    let firstThursday = new Date(t.getFullYear(), 0, 4);
    let fd = (firstThursday.getDay() + 6) % 7;
    firstThursday.setDate(firstThursday.getDate() - fd + 3);
    return 1 + Math.round((t - firstThursday) / (7 * 86400000));
}

function dateKey(d) {
    let m = "" + (d.getMonth() + 1);
    let day = "" + d.getDate();
    return d.getFullYear() + "-" + (m.length < 2 ? "0" : "") + m + "-" + (day.length < 2 ? "0" : "") + day;
}

// ---- Resolve today ----
let now = new Date();
let todayDay = now.getDay() === 0 ? 7 : now.getDay(); // Mon=1..Sun=7
let cycleWeek = (isoWeekNumber(now) % 2 === 0) ? 1 : 2;

let plan = parsePlan(Config.get("FITNESS_PLAN_MD") || DEFAULT_PLAN);
let scheduleText = plan.weeks[cycleWeek][todayDay] ||
    plan.weeks[cycleWeek === 1 ? 2 : 1][todayDay] || "Rest day";

// Expand the schedule into module checklists (warmup first on strength days)
let todaysModules = [];
if (scheduleText !== "Rest day") {
    if (scheduleText.indexOf("力量训练") >= 0 || /strength/i.test(scheduleText)) {
        for (let name in plan.modules) {
            if (name.indexOf("热身") >= 0 || /warm/i.test(name)) {
                todaysModules.push(name);
                break;
            }
        }
    }
    let parts = scheduleText.split(/[＋+、,]/);
    for (let i = 0; i < parts.length; i++) {
        let name = parts[i].trim();
        if (name === "") continue;
        // Schedule names are prefixes of module headings: "核心模块一" should
        // match a module titled "核心模块一：抗伸展". Exact match first, then
        // prefix, then substring as a last resort.
        let found = null;
        if (plan.modules[name]) found = name;
        if (!found) {
            for (let moduleName in plan.modules) {
                if (moduleName.indexOf(name) === 0) { found = moduleName; break; }
            }
        }
        if (!found) {
            for (let moduleName in plan.modules) {
                if (moduleName.indexOf(name) >= 0) { found = moduleName; break; }
            }
        }
        if (found) todaysModules.push(found);
    }
}

// ---- State (per date) ----
let stateFile = "fitness_state_" + dateKey(now) + ".json";
let doneIndexes = [];
try {
    let raw = Storage.readFile(stateFile);
    if (raw) doneIndexes = JSON.parse(raw) || [];
} catch (e) { doneIndexes = []; }
if (!Array.isArray(doneIndexes)) doneIndexes = [];

function saveState() {
    Storage.writeFile(stateFile, JSON.stringify(doneIndexes));
}

// ---- Render: today only ----
let weekNames = ["", "Week 1", "Week 2"];
let header = new Label(weekNames[cycleWeek] + " · " + DAY_NAMES_EN[todayDay - 1] + " · " + scheduleText);
header.setTextSize(18);
header.setBold(true);
UI.addView(header);

let statusLabel = new Label("");
statusLabel.setTextSize(13);
statusLabel.setTextColor("#888888");
UI.addView(statusLabel);

let items = [];

function refreshStatus() {
    statusLabel.setText(doneIndexes.length + " of " + items.length + " done today");
}

let flatIndex = 0;
for (let m = 0; m < todaysModules.length; m++) {
    let moduleName = todaysModules[m];
    let sectionLabel = new Label(moduleName);
    sectionLabel.setTextSize(15);
    sectionLabel.setBold(true);
    sectionLabel.setTextColor("#007AFF");
    UI.addView(sectionLabel);

    let moduleItems = plan.modules[moduleName];
    for (let k = 0; k < moduleItems.length; k++) {
        (function(idx, text) {
            let check = new CheckBox(text);
            if (doneIndexes.indexOf(idx) >= 0) {
                check.setChecked(true);
                check.setStrikeThrough(true);
                check.setTextColor("#888888");
            }
            check.setOnChange(function(checked) {
                check.setStrikeThrough(checked);
                check.setTextColor(checked ? "#888888" : "#212121");
                let at = doneIndexes.indexOf(idx);
                if (checked && at < 0) doneIndexes.push(idx);
                if (!checked && at >= 0) doneIndexes.splice(at, 1);
                saveState();
                refreshStatus();
            });
            items.push(check);
            UI.addView(check);
        })(flatIndex, moduleItems[k]);
        flatIndex++;
    }
}

if (items.length === 0) {
    let rest = new Label("Rest day - nothing scheduled. The plan can be configured in Settings.");
    rest.setTextColor("#888888");
    UI.addView(rest);
}

refreshStatus();
console.log("Daily checklist: " + weekNames[cycleWeek] + " " + DAY_NAMES_EN[todayDay - 1] +
    ", " + items.length + " items, " + doneIndexes.length + " done");
