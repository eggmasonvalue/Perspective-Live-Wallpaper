document.addEventListener('DOMContentLoaded', () => {
    renderMacroGrid();
    renderMicroNoTomorrow();
    renderMicroVsYesterday();
    renderMiniMacroGrid();
});

function renderMacroGrid() {
    const gridContainer = document.getElementById('life-grid');
    if (!gridContainer) return;

    const totalYears = 80;
    const yearsLived = 26; // Simulate a 26-year-old

    // Create 80 dots
    for (let i = 0; i < totalYears; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot');

        if (i < yearsLived) {
            dot.classList.add('past');
        } else if (i === yearsLived) {
            dot.classList.add('current');
            // Add a subtle title for hover
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

    // Smaller grid for feature preview
    const totalYears = 40;
    const yearsLived = 15;

    for (let i = 0; i < totalYears; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot');
        // Make them smaller via inline style or CSS class specific to mini
        dot.style.width = '12px';
        dot.style.height = '12px';

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
    shape.style.margin = '0 auto';

    container.appendChild(shape);
}

function renderMicroVsYesterday() {
    const container = document.getElementById('micro-vs-yesterday');
    if (!container) return;

    const vsContainer = document.createElement('div');
    vsContainer.classList.add('vs-container');

    // Past Shape (Static)
    const pastShape = document.createElement('div');
    pastShape.classList.add('shape-past');
    pastShape.title = "Yesterday";

    // Present Shape (Pulsing)
    const presentShape = document.createElement('div');
    presentShape.classList.add('shape-present');
    presentShape.title = "Today";

    vsContainer.appendChild(pastShape);
    vsContainer.appendChild(presentShape);

    container.appendChild(vsContainer);
}
