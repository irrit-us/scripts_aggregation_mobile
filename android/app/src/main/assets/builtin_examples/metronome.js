// Example 18: Metronome
// A full practice metronome: BPM range 30-250 (grave to prestissimo) with a
// slider plus minus/plus step buttons, tap tempo (average of the last 4
// taps, reset after a 2 second gap), selectable time signature of 2, 3, 4
// or 6 beats per bar, audible clicks via Sound.playTone (1568 Hz accent,
// 1046 Hz regular), vibration feedback, a row of beat dots, and a big
// colored flash indicator showing the current beat. Timing is
// drift-corrected: every beat is scheduled with setTimeout against an
// absolute target time instead of setInterval, so the tempo never drifts
// and mid-play changes stay smooth.
// Permissions: VIBRATE (grant when prompted).
UI.setTitle("Metronome");


let COLOR_ACCENT = "#FF5722";
let COLOR_BEAT = "#2196F3";
let COLOR_IDLE = "#B0BEC5";
let COLOR_TEXT = "#FFFFFF";

let MIN_BPM = 30;
let MAX_BPM = 250;
let TAP_RESET_MS = 2000;
let TAP_COUNT = 4;
let ACCENT_HZ = 1568;
let BEAT_HZ = 1046;
let CLICK_MS = 50;

let bpm = 120;
let beatsPerBar = 4;
let currentBeat = 0;
let running = false;
let timerId = 0;
let nextBeatAt = 0;
let tapTimes = [];
let dotLabels = [];

// Big BPM readout
let bpmLabel = new Label(bpm + " BPM");
bpmLabel.setTextSize(28);
bpmLabel.setBold(true);
bpmLabel.setTextAlign("center");
UI.addView(bpmLabel);

// Big colored flash indicator, also shows the beat number within the bar
let beatLabel = new Label("Ready");
beatLabel.setTextSize(32);
beatLabel.setTextAlign("center");
beatLabel.setBold(true);
beatLabel.setBackgroundColor(COLOR_IDLE);
beatLabel.setCornerRadius(8);
beatLabel.setPadding(16, 24, 16, 24);
UI.addView(beatLabel);

// Row of beat dots: one small Label per beat in the current bar
let dotsRow = new Layout("horizontal");
dotsRow.setGravity("center_vertical");
UI.addView(dotsRow);

// BPM row: minus glyph, coarse slider, plus glyph
let bpmRow = new Layout("horizontal");
bpmRow.setGravity("center_vertical");
UI.addView(bpmRow);

let minusBtn = new Label("-");
minusBtn.setTextSize(28);
minusBtn.setBold(true);
minusBtn.setPadding(20, 0, 20, 0);
minusBtn.setOnTap(function() {
    setBpm(bpm - 1);
});
bpmRow.addView(minusBtn);

let bpmSlider = new Slider();
bpmSlider.setMin(MIN_BPM);
bpmSlider.setMax(MAX_BPM);
bpmSlider.setValue(bpm);
bpmSlider.setOnChange(function(value) {
    setBpm(value);
});
bpmRow.addView(bpmSlider);
bpmSlider.setWeight(1);

let plusBtn = new Label("+");
plusBtn.setTextSize(28);
plusBtn.setBold(true);
plusBtn.setPadding(20, 0, 20, 0);
plusBtn.setOnTap(function() {
    setBpm(bpm + 1);
});
bpmRow.addView(plusBtn);

// Time signature row: label plus spinner for beats per bar
let sigRow = new Layout("horizontal");
sigRow.setGravity("center_vertical");
UI.addView(sigRow);

let sigLabel = new Label("Beats per bar:");
sigLabel.setTextSize(16);
sigLabel.setPadding(0, 0, 12, 0);
sigRow.addView(sigLabel);

let sigSpinner = new Spinner();
sigSpinner.setItems(["2", "3", "4", "6"]);
sigSpinner.setSelection(2);
sigSpinner.setOnChange(function(index, label) {
    beatsPerBar = parseInt(label);
    console.log("Time signature changed to " + beatsPerBar + " beats per bar");
    rebuildDots();
    if (running) {
        // Restart the bar at beat 1 without stopping the beat stream
        currentBeat = 0;
        nextBeatAt = Device.getTime();
        onBeat();
    }
});
sigRow.addView(sigSpinner);
sigSpinner.setWeight(1);

// Button row: tap tempo plus start/stop toggle
let btnRow = new Layout("horizontal");
btnRow.setGravity("center_vertical");
UI.addView(btnRow);

let tapBtn = new Button("TAP");
tapBtn.setOnTap(function() {
    onTapTempo();
});
btnRow.addView(tapBtn);
tapBtn.setWeight(1);

let toggleBtn = new Button("Start");
toggleBtn.setBackgroundColor(COLOR_BEAT);
toggleBtn.setTextColor(COLOR_TEXT);
toggleBtn.setOnTap(function() {
    if (running) {
        stopMetronome();
    } else {
        startMetronome();
    }
});
btnRow.addView(toggleBtn);
toggleBtn.setWeight(1);

function setBpm(value) {
    let next = Math.round(value);
    if (next < MIN_BPM) next = MIN_BPM;
    if (next > MAX_BPM) next = MAX_BPM;
    if (next === bpm) {
        return;
    }
    bpm = next;
    bpmLabel.setText(bpm + " BPM");
    bpmSlider.setValue(bpm);
    console.log("BPM set to " + bpm);
    if (running) {
        // Reschedule at the new tempo without dropping the beat stream
        nextBeatAt = Device.getTime();
        scheduleNext();
    }
}

function onTapTempo() {
    let now = Device.getTime();
    let lastTap = tapTimes.length > 0 ? tapTimes[tapTimes.length - 1] : 0;
    if (now - lastTap > TAP_RESET_MS) {
        tapTimes = []; // gap too long: start a fresh tap history
    }
    tapTimes.push(now);
    if (tapTimes.length > TAP_COUNT) {
        tapTimes.shift();
    }
    if (tapTimes.length >= 2) {
        let span = tapTimes[tapTimes.length - 1] - tapTimes[0];
        let avgInterval = span / (tapTimes.length - 1);
        setBpm(60000 / avgInterval);
        console.log("Tap tempo: " + bpm + " BPM from " + tapTimes.length + " taps");
    } else {
        console.log("Tap tempo: first tap, keep tapping");
    }
}

function rebuildDots() {
    for (let i = 0; i < dotLabels.length; i++) {
        dotsRow.removeView(dotLabels[i]);
    }
    dotLabels = [];
    for (let i = 0; i < beatsPerBar; i++) {
        let dot = new Label("○");
        dot.setTextSize(24);
        dot.setPadding(10, 0, 10, 0);
        dotsRow.addView(dot);
        dotLabels.push(dot);
    }
    updateDots();
}

function updateDots() {
    for (let i = 0; i < dotLabels.length; i++) {
        let played = (i < currentBeat);
        dotLabels[i].setText(played ? "●" : "○");
        dotLabels[i].setTextColor(played && i === 0 ? COLOR_ACCENT : "");
    }
}

function scheduleNext() {
    // Always clear the pending timeout before scheduling a new one
    stopTimer();
    nextBeatAt = nextBeatAt + 60000.0 / bpm;
    let delay = nextBeatAt - Device.getTime();
    timerId = setTimeout(onBeat, Math.max(0, delay));
}

function stopTimer() {
    if (timerId !== 0) {
        clearTimeout(timerId);
        timerId = 0;
    }
}

function onBeat() {
    if (!running) {
        return;
    }
    currentBeat = (currentBeat % beatsPerBar) + 1;
    let accent = (currentBeat === 1);
    Sound.playTone(accent ? ACCENT_HZ : BEAT_HZ, CLICK_MS);
    Device.vibrate(accent ? 80 : 30);
    beatLabel.setBackgroundColor(accent ? COLOR_ACCENT : COLOR_BEAT);
    beatLabel.setText("Beat " + currentBeat + " of " + beatsPerBar);
    updateDots();
    scheduleNext();
}

function startMetronome() {
    running = true;
    currentBeat = 0;
    nextBeatAt = Device.getTime();
    toggleBtn.setText("Stop");
    onBeat(); // play the downbeat immediately, then schedule the rest
    console.log("Metronome started at " + bpm + " BPM, " + beatsPerBar + " beats per bar");
}

function stopMetronome() {
    running = false;
    stopTimer();
    currentBeat = 0;
    toggleBtn.setText("Start");
    beatLabel.setText("Ready");
    beatLabel.setBackgroundColor(COLOR_IDLE);
    updateDots();
    console.log("Metronome stopped");
}

rebuildDots();
console.log("Metronome ready at " + bpm + " BPM");
