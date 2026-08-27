// Example 18: Metronome
// A BPM metronome: slider (40-208 BPM) with a live BPM label and a
// start/stop button. Each beat flashes the beat indicator and vibrates;
// the first beat of every 4 is accented (different color, longer vibration).
// The interval is always cleared before it is re-created, so changing the
// BPM or toggling start/stop never stacks multiple timers.
// Permissions: VIBRATE (grant when prompted).

let COLOR_ACCENT = "#FF5722";
let COLOR_BEAT = "#2196F3";
let COLOR_IDLE = "#B0BEC5";

let bpm = 120;
let beatCount = 0;
let timerId = 0;
let running = false;

let title = new Label("Metronome");
title.setTextSize(24);
UI.addView(title);

let bpmLabel = new Label("BPM: " + bpm);
bpmLabel.setTextSize(18);
UI.addView(bpmLabel);

let beatLabel = new Label("Ready");
beatLabel.setTextSize(32);
beatLabel.setTextAlign("center");
beatLabel.setBold(true);
beatLabel.setBackgroundColor(COLOR_IDLE);
beatLabel.setCornerRadius(8);
beatLabel.setPadding(16, 24, 16, 24);
UI.addView(beatLabel);

let bpmSlider = new Slider();
bpmSlider.setMin(40);
bpmSlider.setMax(208);
bpmSlider.setValue(bpm);
bpmSlider.setOnChange(function(value) {
    bpm = value;
    bpmLabel.setText("BPM: " + bpm);
    if (running) {
        startTimer();
    }
});
UI.addView(bpmSlider);

let toggleBtn = new Button("Start");
toggleBtn.setBackgroundColor(COLOR_BEAT);
toggleBtn.setTextColor("#FFFFFF");
toggleBtn.setOnTap(function() {
    if (running) {
        stopMetronome();
    } else {
        startMetronome();
    }
});
UI.addView(toggleBtn);

function intervalMs() {
    return Math.round(60000 / bpm);
}

function startTimer() {
    // Always clear before re-creating: never two timers at once
    stopTimer();
    timerId = setInterval(onBeat, intervalMs());
}

function stopTimer() {
    if (timerId !== 0) {
        clearInterval(timerId);
        timerId = 0;
    }
}

function onBeat() {
    beatCount = beatCount + 1;
    let accent = (beatCount % 4 === 1);
    beatLabel.setBackgroundColor(accent ? COLOR_ACCENT : COLOR_BEAT);
    beatLabel.setText(accent ? "TAP" : "tap");
    Device.vibrate(accent ? 80 : 30);
}

function startMetronome() {
    running = true;
    beatCount = 0;
    toggleBtn.setText("Stop");
    onBeat(); // first beat right away
    startTimer();
    showToast("Metronome started at " + bpm + " BPM");
}

function stopMetronome() {
    running = false;
    stopTimer();
    toggleBtn.setText("Start");
    beatLabel.setText("Ready");
    beatLabel.setBackgroundColor(COLOR_IDLE);
    showToast("Metronome stopped");
}

console.log("Metronome ready at " + bpm + " BPM");
