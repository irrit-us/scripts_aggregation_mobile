// Plan parser unit tests (node tests/plan_parser.test.js)
// Exercises the restricted YAML subset parser from daily_fitness.js against
// the documented config format. Run: node tests/plan_parser.test.js
// The parser source is injected into the marker block by the test suite.

const NL = String.fromCharCode(10);

/*PARSER_SOURCE*/

let failures = 0;
function check(name, cond) {
    if (cond) { console.log("PARSERTEST|PASS|" + name); }
    else { failures++; console.log("PARSERTEST|FAIL|" + name); }
}

// 1. Documented sample: scalars, schedule, actions and notes
let yaml = [
    "cycle_days: 14",
    "cycle_start: 2026-08-24",
    "schedule:",
    "  - day: 1",
    "    modules: [Warm-up, Strength A, Core A]",
    "  - day: 2",
    "    modules: [Core B]",
    "modules:",
    "  Core A:",
    "    - action: Dead bug: 2 sets x 8",
    "      notes:",
    "        - keep your lower back near the floor",
    "        - shorten the range if the back lifts",
    "    - action: Plank: 2 sets x 30 s",
    "      notes:",
    "        - squeeze abs and glutes",
    "  Core B:",
    "    - action: Side plank: 2 sets x 20 s"
].join(NL);
let plan = parseYamlPlan(yaml);
check("cycleDays parsed", plan.cycleDays === 14);
check("cycleStart parsed", plan.cycleStart === "2026-08-24");
check("schedule day1 modules", JSON.stringify(plan.schedule[1]) === JSON.stringify(["Warm-up", "Strength A", "Core A"]));
check("schedule day2 modules", JSON.stringify(plan.schedule[2]) === JSON.stringify(["Core B"]));
check("core A has 2 actions", plan.modules["Core A"].length === 2);
check("first action text", plan.modules["Core A"][0].action === "Dead bug: 2 sets x 8");
check("notes attached to action", plan.modules["Core A"][0].notes.length === 2);
check("notes is not a module", plan.modules["notes"] === undefined);
check("second action has 1 note", plan.modules["Core A"][1].notes.length === 1);
check("module without notes parses", plan.modules["Core B"].length === 1 && plan.modules["Core B"][0].notes.length === 0);

// 2. Plain items (no action: prefix) become actions
let simple = parseYamlPlan(["modules:", "  Simple:", "    - just a plain item", "    - action: explicit one"].join(NL));
check("plain item becomes action", simple.modules["Simple"][0].action === "just a plain item");
check("explicit action after plain", simple.modules["Simple"][1].action === "explicit one");

// 3. Nested module list lines under "- day:" without inline list
let nested = parseYamlPlan(["schedule:", "  - day: 3", "    - Alpha", "    - Beta", "modules:", "  Alpha:", "    - action: a", "  Beta:", "    - action: b"].join(NL));
check("nested schedule lines", JSON.stringify(nested.schedule[3]) === JSON.stringify(["Alpha", "Beta"]));

// 4. Quoted values are unquoted
let quoted = parseYamlPlan(["modules:", '  "Quoted Mod":', '    - action: "do it"'].join(NL));
check("quoted module name", quoted.modules["Quoted Mod"] !== undefined);
check("quoted action text", quoted.modules["Quoted Mod"][0].action === "do it");

// 5. Malformed input never throws and yields defaults
let threw = false, garbage = null;
try { garbage = parseYamlPlan(["not yaml at all", "::::", "  - ["].join(NL)); } catch (e) { threw = true; }
check("malformed input does not throw", !threw);
check("malformed yields default cycleDays", garbage.cycleDays === 14);
check("malformed yields empty modules", Object.keys(garbage.modules).length === 0);

// 6. Comments and blank lines are skipped
let commented = parseYamlPlan(["# a comment", "", "cycle_days: 7", "modules:", "  A:", "    - action: x"].join(NL));
check("comments skipped", commented.cycleDays === 7 && commented.modules["A"].length === 1);

console.log("PARSERTEST|DONE|" + (failures === 0 ? "all passed" : failures + " failed"));
process.exit(failures === 0 ? 0 : 1);
