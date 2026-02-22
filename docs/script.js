document.addEventListener('DOMContentLoaded', () => {
    renderMacroGrid();
    renderMiniMacroGrid();
    renderMicroNoTomorrow();
    renderMicroVsYesterday();
});

function renderMacroGrid() {
    const gridContainer = document.getElementById('life-grid');
    if (!gridContainer) return;

    // Simulate standard human lifespan ~80-90 years
    const totalYears = 90;
    const yearsLived = 26; // Target demographic

    // To mimic the vertical phone layout (9:16 aspect ratio),
    // we want more rows than columns.
    // E.g., 7 columns is a common "week-like" width or just a good narrow number.
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
        // Make them slightly smaller/adjusted for mini view if needed via CSS

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

    // Order: Past (top/left) vs Present (bottom/right) or stacked
    vsContainer.appendChild(pastShape);
    vsContainer.appendChild(presentShape);

    container.appendChild(vsContainer);
}
