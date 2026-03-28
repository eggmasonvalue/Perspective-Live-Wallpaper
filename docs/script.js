document.addEventListener('DOMContentLoaded', () => {
    renderMacroGrid();
    renderMiniMacroGrid();
    renderMicroNoTomorrow();
    renderMicroVsYesterday();
    renderMicroCustom();
    renderMicroHealth();
});

function renderMacroGrid() {
    const gridContainer = document.getElementById('life-grid');
    if (!gridContainer) return;

    // Simulate standard human lifespan ~80-90 years
    const totalYears = 90;
    const yearsLived = 26; // Target demographic

    // To mimic the vertical phone layout (9:16 aspect ratio),
    // we want more rows than columns.
    const cols = 7;

    // Update CSS grid columns dynamically
    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;

    // Create dots
    for (let i = 0; i < totalYears; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot');

        if (i < yearsLived) {
            dot.classList.add('past');
        } else if (i === yearsLived) {
            dot.classList.add('current');
            dot.title = "Current Year";
        } else {
            dot.classList.add('future');
        }

        gridContainer.appendChild(dot);
    }
}

function renderMiniMacroGrid() {
    const gridContainer = document.getElementById('mini-macro-grid');
    if (!gridContainer) return;

    // A smaller subset for the feature preview card
    const totalYears = 60;
    const yearsLived = 20;
    const cols = 6;

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;

    for (let i = 0; i < totalYears; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot');

        if (i < yearsLived) {
            dot.classList.add('past');
        } else if (i === yearsLived) {
            dot.classList.add('current');
        } else {
            dot.classList.add('future');
        }
        gridContainer.appendChild(dot);
    }
}

function renderMicroNoTomorrow() {
    const container = document.getElementById('micro-no-tomorrow');
    if (!container) return;

    // Create a large pulsing circle
    const shape = document.createElement('div');
    shape.classList.add('shape-no-tomorrow');

    // Center it
    shape.style.margin = 'auto'; // Flexbox centering handles this too

    container.appendChild(shape);
}

function renderMicroVsYesterday() {
    const container = document.getElementById('micro-vs-yesterday');
    if (!container) return;

    const vsContainer = document.createElement('div');
    vsContainer.classList.add('vs-container');

    // Past Shape (Static - Gray)
    const pastShape = document.createElement('div');
    pastShape.classList.add('shape-past');
    pastShape.title = "Yesterday";

    // Present Shape (Pulsing - Red)
    const presentShape = document.createElement('div');
    presentShape.classList.add('shape-present');
    presentShape.title = "Today";

    // Order: Past (top) vs Present (bottom)
    vsContainer.appendChild(pastShape);
    vsContainer.appendChild(presentShape);

    container.appendChild(vsContainer);
}

function renderMicroCustom() {
    const gridContainer = document.getElementById('custom-event-grid');
    if (!gridContainer) return;

    // Simulate a 30-day countdown
    const totalDays = 30;
    const daysPassed = 12;
    const cols = 5; // Use 5 cols for weeks (mon-fri) or similar feel

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;

    for (let i = 0; i < totalDays; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot'); // Reuse macro dot styles for consistency

        if (i < daysPassed) {
            dot.classList.add('past');
        } else if (i === daysPassed) {
            dot.classList.add('current');
            dot.title = "Today";
        } else {
            dot.classList.add('future');
        }
        gridContainer.appendChild(dot);
    }
}

function renderMicroHealth() {
    const gridContainer = document.getElementById('health-event-grid');
    if (!gridContainer) return;

    // Simulate Health Connect Data
    const totalDays = 30; // One month view
    const daysPassed = 24; // Current day is the 25th
    const cols = 5; // 5 columns

    // Simulate varying opacities for past days (based on goal progress)
    // and labels
    const data = [
        { val: "10.8k", opacity: 0.9 },
        { val: "16.9k", opacity: 1.0 },
        { val: "13.8k", opacity: 0.95 },
        { val: "14.3k", opacity: 0.98 },
        { val: "2.2k",  opacity: 0.2 },
        { val: "3.4k",  opacity: 0.3 },
        { val: "2.4k",  opacity: 0.25 },
        { val: "4.0k",  opacity: 0.4 },
        { val: "12.8k", opacity: 0.9 },
        { val: "19.8k", opacity: 1.0 },
        { val: "13.2k", opacity: 0.95 },
        { val: "8.3k",  opacity: 0.8 },
        { val: "9.5k",  opacity: 0.95 },
        { val: "8.9k",  opacity: 0.85 },
        { val: "13.2k", opacity: 0.98 },
        { val: "20.5k", opacity: 1.0 },
        { val: "6.1k",  opacity: 0.6 },
        { val: "13.4k", opacity: 0.95 },
        { val: "1.2k",  opacity: 0.1 },
        { val: "1.5k",  opacity: 0.15 },
        { val: "10.3k", opacity: 0.9 },
        { val: "8.9k",  opacity: 0.85 },
        { val: "10.4k", opacity: 0.92 },
        { val: "10.4k", opacity: 0.92 },
    ];

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;
    gridContainer.classList.add('rhombus-grid'); // Special class for rhombus health style

    for (let i = 0; i < totalDays; i++) {
        const dot = document.createElement('div');
        dot.classList.add('health-shape');

        if (i < daysPassed) {
            dot.classList.add('past');
            // Apply variable opacity
            const opacity = data[i] ? data[i].opacity : 0.5;
            dot.style.opacity = opacity;

            // Add text overlay
            const label = document.createElement('span');
            label.textContent = data[i] ? data[i].val : "";
            dot.appendChild(label);

        } else if (i === daysPassed) {
            dot.classList.add('current');
            dot.title = "Today";
            dot.style.opacity = 1.0; // Pulse animation handles full opacity, but base is 1

            // Current day label
            const label = document.createElement('span');
            label.textContent = "14"; // Day of month or current metric
            dot.appendChild(label);

        } else {
            dot.classList.add('future');
        }
        gridContainer.appendChild(dot);
    }
}
