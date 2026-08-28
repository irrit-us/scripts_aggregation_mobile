// Example 20: Sound Demo
// A musical note pad: one compact row of note buttons C4 through C5 that
// play 300 ms tones via Sound.playTone(frequencyHz, durationMs), plus a
// frequency slider (200-2000 Hz), a duration slider (50-1000 ms) and a
// volume slider (0-100 percent) feeding a play button that uses
// Sound.playTone(frequencyHz, durationMs, volume) with volume 0.0-1.0.
// Permissions: none (audio playback requires no permissions)

UI.setTitle("Sound Demo");

// Note table: name and frequency in Hz, played for 300 ms per tap
let NOTE_MS = 300;
let notes = [
    { name: "C4", freq: 262 },
    { name: "D4", freq: 294 },
    { name: "E4", freq: 330 },
    { name: "F4", freq: 349 },
    { name: "G4", freq: 392 },
    { name: "A4", freq: 440 },
    { name: "B4", freq: 494 },
    { name: "C5", freq: 523 }
];

let fontSize = 16;

// Compact note pad row: note names as tap glyphs, each weighted to fill
let padRow = new Layout("horizontal");
padRow.setGravity("center_vertical");
padRow.setWidth(-1);
UI.addView(padRow);

for (let i = 0; i < notes.length; i++) {
    let note = notes[i];
    let noteBtn = new Label(note.name);
    noteBtn.setTextSize(fontSize * 1.5);
    noteBtn.setBold(true);
    noteBtn.setTextAlign("center");
    noteBtn.setPadding(4, 12, 4, 12);
    noteBtn.setOnTap(function() {
        Sound.playTone(note.freq, NOTE_MS);
        console.log("Played note " + note.name + " (" + note.freq + " Hz, " + NOTE_MS + " ms)");
    });
    padRow.addView(noteBtn);
    noteBtn.setWeight(1);
}

// Frequency slider with a live Hz label
let freq = 440;
let freqLabel = new Label("Frequency: " + freq + " Hz");
freqLabel.setTextSize(fontSize);
UI.addView(freqLabel);

let freqSlider = new Slider();
freqSlider.setMin(200);
freqSlider.setMax(2000);
freqSlider.setValue(freq);
freqSlider.setWidth(-1);
freqSlider.setOnChange(function(value) {
    freq = value;
    freqLabel.setText("Frequency: " + freq + " Hz");
});
UI.addView(freqSlider);

// Duration slider with a live ms label
let duration = 300;
let durLabel = new Label("Duration: " + duration + " ms");
durLabel.setTextSize(fontSize);
UI.addView(durLabel);

let durSlider = new Slider();
durSlider.setMin(50);
durSlider.setMax(1000);
durSlider.setValue(duration);
durSlider.setWidth(-1);
durSlider.setOnChange(function(value) {
    duration = value;
    durLabel.setText("Duration: " + duration + " ms");
});
UI.addView(durSlider);

// Volume slider (0-100 percent, mapped to 0.0-1.0) with a live label
let volumePct = 80;
let volLabel = new Label("Volume: " + volumePct + "%");
volLabel.setTextSize(fontSize);
UI.addView(volLabel);

let volSlider = new Slider();
volSlider.setMin(0);
volSlider.setMax(100);
volSlider.setValue(volumePct);
volSlider.setWidth(-1);
volSlider.setOnChange(function(value) {
    volumePct = value;
    volLabel.setText("Volume: " + volumePct + "%");
});
UI.addView(volSlider);

// Single primary action: play the custom tone with the three-arg overload
let playBtn = new Button("Play Custom Tone");
playBtn.setBackgroundColor("#2196F3");
playBtn.setTextColor("#FFFFFF");
playBtn.setOnTap(function() {
    let volume = volumePct / 100;
    Sound.playTone(freq, duration, volume);
    console.log("Played custom tone: " + freq + " Hz, " + duration + " ms, volume " + volume);
});
UI.addView(playBtn);

console.log("Sound Demo ready: tap a note or play a custom tone");
