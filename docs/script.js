document.addEventListener('DOMContentLoaded', () => {
    initClock();
    initAtmosphereSpotlight();
    initThemeGallery();
    initHeroSimulation();
    renderMiniMacroGrid();
    renderMicroNoTomorrow();
    renderMicroVsYesterday();
    renderMicroThisYear();
    renderMicroHealth();
    updateTodayRotationBadge();
});

// ============================================================================
// Perceptual Oklch Color Math & Procedural Atmosphere Theme
// Matches OklchColor.kt and ProceduralThemeGenerator.kt
// ============================================================================

function linearToSrgb(value) {
    const clamped = Math.max(0.0, Math.min(1.0, value));
    if (clamped <= 0.0031308) {
        return 12.92 * clamped;
    }
    return 1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055;
}

function oklchToRgb(lightness, chroma, hueDegrees) {
    const l = Math.max(0.0, Math.min(1.0, lightness));
    const c = Math.max(0.0, chroma);
    const hRad = (((hueDegrees % 360) + 360) % 360) * (Math.PI / 180.0);

    const a = c * Math.cos(hRad);
    const b = c * Math.sin(hRad);

    const lLms = Math.pow(l + 0.3963377774 * a + 0.2158037573 * b, 3);
    const mLms = Math.pow(l - 0.1055613458 * a - 0.0638541728 * b, 3);
    const sLms = Math.pow(l - 0.0894841775 * a - 1.2914855480 * b, 3);

    const rLin = +4.0767416621 * lLms - 3.3077115913 * mLms + 0.2309699292 * sLms;
    const gLin = -1.2684380046 * lLms + 2.6097574011 * mLms - 0.3413193965 * sLms;
    const bLin = -0.0041960863 * lLms - 0.7034186147 * mLms + 1.7076147010 * sLms;

    const r = Math.max(0, Math.min(255, Math.floor(linearToSrgb(rLin) * 255.0 + 0.5)));
    const g = Math.max(0, Math.min(255, Math.floor(linearToSrgb(gLin) * 255.0 + 0.5)));
    const bl = Math.max(0, Math.min(255, Math.floor(linearToSrgb(bLin) * 255.0 + 0.5)));

    return { r, g, b: bl };
}

function rgbToHex({ r, g, b }) {
    const toHex = (n) => n.toString(16).padStart(2, '0').toUpperCase();
    return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function rgbToRgba({ r, g, b }, alpha) {
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function getLocalEpochDay(date = new Date()) {
    return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / (1000 * 60 * 60 * 24));
}

function generateAtmosphereTheme(date = new Date()) {
    const GOLDEN_ANGLE = 137.507764;
    const epochDay = getLocalEpochDay(date);
    const baseHue = ((Math.trunc(epochDay * GOLDEN_ANGLE) % 360) + 360) % 360;
    const offset = ((epochDay % 2) + 2) % 2 === 0 ? 150 : 180;
    const accentHue = (baseHue + offset) % 360;

    const lightBgRgb = oklchToRgb(0.965, 0.012, baseHue);
    const darkBgRgb = oklchToRgb(0.095, 0.018, baseHue);
    const pastRgb = oklchToRgb(0.58, 0.055, baseHue);
    const lightCurrentRgb = oklchToRgb(0.62, 0.16, accentHue);
    const darkCurrentRgb = oklchToRgb(0.72, 0.16, accentHue);

    const getBaseDescriptor = (h) => {
        if (h <= 35) return 'Terracotta';
        if (h <= 70) return 'Dune';
        if (h <= 125) return 'Moss';
        if (h <= 175) return 'Celadon';
        if (h <= 230) return 'Glacial';
        if (h <= 280) return 'Basalt';
        if (h <= 325) return 'Twilight';
        return 'Cinnabar';
    };

    const getAccentDescriptor = (h) => {
        if (h <= 45) return 'Ember';
        if (h <= 90) return 'Ochre';
        if (h <= 150) return 'Verdant';
        if (h <= 210) return 'Cyan';
        if (h <= 270) return 'Zenith';
        if (h <= 320) return 'Amethyst';
        return 'Coral';
    };

    return {
        id: 'atmosphere',
        name: 'Atmosphere',
        mood: `${getBaseDescriptor(baseHue)} & ${getAccentDescriptor(accentHue)}`,
        category: 'Procedural',
        isRotatable: false,
        light: {
            bg: rgbToHex(lightBgRgb),
            past: rgbToHex(pastRgb),
            current: rgbToHex(lightCurrentRgb),
            future: rgbToRgba(pastRgb, 0.30),
            futureBorder: rgbToRgba(pastRgb, 0.45),
            text: '#1F2022'
        },
        dark: {
            bg: rgbToHex(darkBgRgb),
            past: rgbToHex(pastRgb),
            current: rgbToHex(darkCurrentRgb),
            future: rgbToRgba(pastRgb, 0.30),
            futureBorder: rgbToRgba(pastRgb, 0.45),
            text: '#FAF8F5'
        }
    };
}

// ============================================================================
// Curated Paired Themes (14 Curated + 4 Health Connect)
// Matches CuratedThemes.kt
// ============================================================================

function makeFuture(hex) {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return {
        future: `rgba(${r}, ${g}, ${b}, 0.30)`,
        futureBorder: `rgba(${r}, ${g}, ${b}, 0.45)`
    };
}

function createTheme(id, name, category, isRotatable, lightColors, darkColors) {
    const lightFuture = makeFuture(lightColors.past);
    const darkFuture = makeFuture(darkColors.past);
    return {
        id,
        name,
        category,
        isRotatable,
        light: {
            bg: lightColors.bg,
            past: lightColors.past,
            current: lightColors.current,
            future: lightFuture.future,
            futureBorder: lightFuture.futureBorder,
            text: '#1F2022'
        },
        dark: {
            bg: darkColors.bg,
            past: darkColors.past,
            current: darkColors.current,
            future: darkFuture.future,
            futureBorder: darkFuture.futureBorder,
            text: '#FAF8F5'
        }
    };
}

const ATMOSPHERE_TODAY = generateAtmosphereTheme();

const THEMES = {
    sage_garden: createTheme(
        'sage_garden', 'Iconic', 'Curated', true,
        { bg: '#EAE7DC', past: '#8E8D8A', current: '#E85A4F' },
        { bg: '#191816', past: '#8E8D8A', current: '#E85A4F' }
    ),
    nordic_minimal: createTheme(
        'nordic_minimal', 'Nordic Minimal', 'Curated', true,
        { bg: '#F8F9FA', past: '#7D9D9C', current: '#E76F51' },
        { bg: '#131A19', past: '#6F9998', current: '#E76F51' }
    ),
    warm_sand: createTheme(
        'warm_sand', 'Warm Sand', 'Curated', true,
        { bg: '#FAF7F2', past: '#C9ADA7', current: '#9A8873' },
        { bg: '#171311', past: '#A89690', current: '#D4B896' }
    ),
    glacial_peak: createTheme(
        'glacial_peak', 'Glacial Peak', 'Curated', true,
        { bg: '#F5F8FA', past: '#7D94B0', current: '#E57373' },
        { bg: '#0B1120', past: '#5B9AA0', current: '#FFA07A' }
    ),
    rose_quartz: createTheme(
        'rose_quartz', 'Rose Quartz', 'Curated', true,
        { bg: '#FFF9FA', past: '#C5A3AB', current: '#6D9DC5' },
        { bg: '#1B1725', past: '#A78BAA', current: '#F2C14E' }
    ),
    monochrome_zen: createTheme(
        'monochrome_zen', 'Monochrome Zen', 'Curated', true,
        { bg: '#FAFAFA', past: '#666666', current: '#000000' },
        { bg: '#121212', past: '#888888', current: '#FFFFFF' }
    ),
    sumi_cinnabar: createTheme(
        'sumi_cinnabar', 'Sumi & Cinnabar', 'Curated', true,
        { bg: '#F7F4EE', past: '#7A7875', current: '#D84A38' },
        { bg: '#141312', past: '#8A8884', current: '#E05644' }
    ),
    boreal_forest: createTheme(
        'boreal_forest', 'Boreal Forest', 'Curated', true,
        { bg: '#F4F7F4', past: '#6B8775', current: '#E08C38' },
        { bg: '#0E1712', past: '#668571', current: '#F0A04B' }
    ),
    sahara_dunes: createTheme(
        'sahara_dunes', 'Sahara Dunes', 'Curated', true,
        { bg: '#FAF6F0', past: '#B5997E', current: '#2A9D8F' },
        { bg: '#191410', past: '#9C8167', current: '#38B2A3' }
    ),
    kyoto_celadon: createTheme(
        'kyoto_celadon', 'Kyoto Celadon', 'Curated', true,
        { bg: '#F3F7F5', past: '#78998C', current: '#E26D5C' },
        { bg: '#101815', past: '#68887C', current: '#EB7A6A' }
    ),
    alpenglow: createTheme(
        'alpenglow', 'Alpenglow', 'Curated', true,
        { bg: '#F7F5F9', past: '#8F8299', current: '#DE7E73' },
        { bg: '#15111B', past: '#877A93', current: '#E88C82' }
    ),
    basalt_ochre: createTheme(
        'basalt_ochre', 'Basalt & Ochre', 'Curated', true,
        { bg: '#F5F5F4', past: '#757471', current: '#D4973B' },
        { bg: '#141413', past: '#8A8985', current: '#E5A84C' }
    ),
    pacific_drift: createTheme(
        'pacific_drift', 'Pacific Drift', 'Curated', true,
        { bg: '#F3F7F8', past: '#6E8B99', current: '#3EA892' },
        { bg: '#0D161A', past: '#5E7E8E', current: '#4EBEA7' }
    ),
    terracotta_courtyard: createTheme(
        'terracotta_courtyard', 'Terracotta Courtyard', 'Curated', true,
        { bg: '#FAF5F2', past: '#B57E70', current: '#3D78B2' },
        { bg: '#1A1210', past: '#A06A5D', current: '#5290CE' }
    ),
    health_steps: createTheme(
        'health_steps', 'Steps Green', 'Health Connect', false,
        { bg: '#F1F8E9', past: '#AED581', current: '#558B2F' },
        { bg: '#152011', past: '#558B2F', current: '#8BC34A' }
    ),
    health_calories: createTheme(
        'health_calories', 'Vitality Orange', 'Health Connect', false,
        { bg: '#FFF3E0', past: '#FFB74D', current: '#E65100' },
        { bg: '#22140A', past: '#E65100', current: '#FF9800' }
    ),
    health_distance: createTheme(
        'health_distance', 'Distance Purple', 'Health Connect', false,
        { bg: '#F3E5F5', past: '#BA68C8', current: '#6A1B9A' },
        { bg: '#1C0F24', past: '#8E24AA', current: '#BA68C8' }
    ),
    health_sleep: createTheme(
        'health_sleep', 'Deep Sleep Blue', 'Health Connect', false,
        { bg: '#E3F2FD', past: '#64B5F6', current: '#1565C0' },
        { bg: '#0A1424', past: '#1976D2', current: '#64B5F6' }
    )
};

// ============================================================================
// 12 Unit Shapes SVG Path Generator (Matches UnitShapeGeometry.kt)
// ============================================================================

function fmt(n) {
    return Number(n.toFixed(2));
}

function buildPolylinePath(points) {
    if (!points.length) return '';
    let d = `M ${fmt(points[0][0])} ${fmt(points[0][1])}`;
    for (let i = 1; i < points.length; i++) {
        d += ` L ${fmt(points[i][0])} ${fmt(points[i][1])}`;
    }
    return `${d} Z`;
}

function buildRoundedPolygonPath(vertices, cornerFraction) {
    const count = vertices.length;
    if (count < 3) return '';
    let d = '';
    for (let i = 0; i < count; i++) {
        const prev = vertices[(i - 1 + count) % count];
        const curr = vertices[i];
        const next = vertices[(i + 1) % count];

        const dxPrev = prev[0] - curr[0];
        const dyPrev = prev[1] - curr[1];
        const lenPrev = Math.hypot(dxPrev, dyPrev);

        const dxNext = next[0] - curr[0];
        const dyNext = next[1] - curr[1];
        const lenNext = Math.hypot(dxNext, dyNext);

        const trim = Math.min(lenPrev, lenNext) * cornerFraction;
        const startX = curr[0] + (dxPrev / lenPrev) * trim;
        const startY = curr[1] + (dyPrev / lenPrev) * trim;
        const endX = curr[0] + (dxNext / lenNext) * trim;
        const endY = curr[1] + (dyNext / lenNext) * trim;

        d += i === 0
            ? `M ${fmt(startX)} ${fmt(startY)}`
            : ` L ${fmt(startX)} ${fmt(startY)}`;
        d += ` Q ${fmt(curr[0])} ${fmt(curr[1])} ${fmt(endX)} ${fmt(endY)}`;
    }
    return `${d} Z`;
}

function buildRoundedRectPath(x, y, w, h, tl, tr, br, bl) {
    return [
        `M ${fmt(x + tl)} ${fmt(y)}`,
        `L ${fmt(x + w - tr)} ${fmt(y)}`,
        `Q ${fmt(x + w)} ${fmt(y)} ${fmt(x + w)} ${fmt(y + tr)}`,
        `L ${fmt(x + w)} ${fmt(y + h - br)}`,
        `Q ${fmt(x + w)} ${fmt(y + h)} ${fmt(x + w - br)} ${fmt(y + h)}`,
        `L ${fmt(x + bl)} ${fmt(y + h)}`,
        `Q ${fmt(x)} ${fmt(y + h)} ${fmt(x)} ${fmt(y + h - bl)}`,
        `L ${fmt(x)} ${fmt(y + tl)}`,
        `Q ${fmt(x)} ${fmt(y)} ${fmt(x + tl)} ${fmt(y)}`,
        'Z'
    ].join(' ');
}

function buildUnitShapePath(shapeId) {
    const x = 5;
    const y = 5;
    const size = 90;
    const half = size / 2;
    const cx = x + half;
    const cy = y + half;
    const TWO_PI = Math.PI * 2;
    const HALF_PI = Math.PI / 2;

    switch (shapeId) {
        case 'circle':
            return `M ${fmt(cx)} ${fmt(y)} A ${fmt(half)} ${fmt(half)} 0 1 1 ${fmt(cx)} ${fmt(y + size)} A ${fmt(half)} ${fmt(half)} 0 1 1 ${fmt(cx)} ${fmt(y)} Z`;

        case 'rounded_square': {
            const r = size * 0.16;
            return buildRoundedRectPath(x, y, size, size, r, r, r, r);
        }

        case 'squircle': {
            const samples = 64;
            const power = 2.0 / 3.2;
            const pts = [];
            for (let i = 0; i < samples; i++) {
                const angle = (TWO_PI * i) / samples;
                const cosA = Math.cos(angle);
                const sinA = Math.sin(angle);
                pts.push([
                    cx + Math.sign(cosA) * Math.pow(Math.abs(cosA), power) * half,
                    cy + Math.sign(sinA) * Math.pow(Math.abs(sinA), power) * half
                ]);
            }
            return buildPolylinePath(pts);
        }

        case 'square':
            return `M ${x} ${y} H ${x + size} V ${y + size} H ${x} Z`;

        case 'rhombus':
            return buildRoundedPolygonPath([
                [cx, y],
                [x + size, cy],
                [cx, y + size],
                [x, cy]
            ], 0.12);

        case 'hexagon': {
            const verts = [];
            for (let i = 0; i < 6; i++) {
                const angle = -HALF_PI + (TWO_PI * i) / 6;
                verts.push([cx + Math.cos(angle) * half, cy + Math.sin(angle) * half]);
            }
            return buildRoundedPolygonPath(verts, 0.18);
        }

        case 'octagon': {
            const cut = size * 0.28;
            const right = x + size;
            const bottom = y + size;
            return buildRoundedPolygonPath([
                [x + cut, y],
                [right - cut, y],
                [right, y + cut],
                [right, bottom - cut],
                [right - cut, bottom],
                [x + cut, bottom],
                [x, bottom - cut],
                [x, y + cut]
            ], 0.10);
        }

        case 'pill': {
            const pillW = size * 0.56;
            const left = cx - pillW / 2;
            const r = pillW / 2;
            return buildRoundedRectPath(left, y, pillW, size, r, r, r, r);
        }

        case 'clover': {
            const samples = 72;
            const raw = [];
            let maxExtent = 1e-6;
            for (let i = 0; i < samples; i++) {
                const angle = (TWO_PI * i) / samples;
                const r = 0.80 - 0.20 * Math.cos(4 * angle);
                const rx = r * Math.cos(angle);
                const ry = r * Math.sin(angle);
                maxExtent = Math.max(maxExtent, Math.abs(rx), Math.abs(ry));
                raw.push([rx, ry]);
            }
            const scale = half / maxExtent;
            return buildPolylinePath(raw.map(([rx, ry]) => [cx + rx * scale, cy + ry * scale]));
        }

        case 'scallop': {
            const samples = 96;
            const pts = [];
            for (let i = 0; i < samples; i++) {
                const angle = (TWO_PI * i) / samples;
                const r = (0.91 + 0.09 * Math.cos(10 * angle)) * half;
                pts.push([cx + r * Math.cos(angle), cy + r * Math.sin(angle)]);
            }
            return buildPolylinePath(pts);
        }

        case 'leaf': {
            const big = size * 0.48;
            const small = size * 0.10;
            return buildRoundedRectPath(x, y, size, size, big, small, big, small);
        }

        case 'spark': {
            const verts = [];
            const inner = half * 0.44;
            for (let i = 0; i < 8; i++) {
                const angle = -HALF_PI + (TWO_PI * i) / 8;
                const r = i % 2 === 0 ? half : inner;
                verts.push([cx + Math.cos(angle) * r, cy + Math.sin(angle) * r]);
            }
            return buildRoundedPolygonPath(verts, 0.28);
        }

        default: {
            const r = size * 0.16;
            return buildRoundedRectPath(x, y, size, size, r, r, r, r);
        }
    }
}

const SHAPES = [
    { id: 'rounded_square', label: 'Rounded' },
    { id: 'circle', label: 'Circle' },
    { id: 'squircle', label: 'Squircle' },
    { id: 'square', label: 'Square' },
    { id: 'rhombus', label: 'Rhombus' },
    { id: 'hexagon', label: 'Hexagon' },
    { id: 'octagon', label: 'Octagon' },
    { id: 'pill', label: 'Capsule' },
    { id: 'clover', label: 'Clover' },
    { id: 'scallop', label: 'Scallop' },
    { id: 'leaf', label: 'Leaf' },
    { id: 'spark', label: 'Spark' }
].map(shape => ({
    ...shape,
    path: buildUnitShapePath(shape.id)
}));

let simState = {
    themeId: 'sage_garden',
    isDark: false,
    shapeIndex: 0,
    isDailyRotation: false
};

function getThemeById(themeId) {
    if (themeId === 'atmosphere') return ATMOSPHERE_TODAY;
    return THEMES[themeId] || THEMES.sage_garden;
}

function getRotatedThemeId(date = new Date()) {
    const rotatableKeys = Object.keys(THEMES).filter(k => THEMES[k].isRotatable);
    const epochDay = getLocalEpochDay(date);
    const dayIndex = ((epochDay % rotatableKeys.length) + rotatableKeys.length) % rotatableKeys.length;
    return rotatableKeys[dayIndex];
}

function initClock() {
    const clockEl = document.getElementById('sim-clock');
    if (!clockEl) return;
    const updateTime = () => {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        clockEl.textContent = `${hours}:${minutes}`;
    };
    updateTime();
    setInterval(updateTime, 30000);
}

function initHeroSimulation() {
    const gridContainer = document.getElementById('life-grid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';
    const totalYears = 90;
    const yearsLived = 26;
    const cols = 7;

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;

    const svgNS = 'http://www.w3.org/2000/svg';
    const initialPath = SHAPES[simState.shapeIndex].path;

    for (let i = 0; i < totalYears; i++) {
        const cell = document.createElement('div');
        cell.classList.add('year-dot', 'svg-cell');

        if (i < yearsLived) {
            cell.classList.add('past');
        } else if (i === yearsLived) {
            cell.classList.add('current');
            cell.title = 'Current Year';
        } else {
            cell.classList.add('future');
        }

        const svg = document.createElementNS(svgNS, 'svg');
        svg.setAttribute('viewBox', '0 0 100 100');
        svg.classList.add('shape-svg');

        const pathEl = document.createElementNS(svgNS, 'path');
        pathEl.setAttribute('d', initialPath);
        pathEl.setAttribute('stroke-width', '7');
        svg.appendChild(pathEl);

        cell.appendChild(svg);
        gridContainer.appendChild(cell);
    }

    applyHeroTheme();

    // Theme Pills Click Handler
    const themePillButtons = document.querySelectorAll('#hero-theme-pills .theme-pill');
    themePillButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const themeId = btn.getAttribute('data-theme');
            setHeroTheme(themeId, false);
        });
    });

    // Dark Mode Toggle
    const darkToggleBtn = document.getElementById('toggle-dark-mode');
    if (darkToggleBtn) {
        darkToggleBtn.addEventListener('click', () => {
            simState.isDark = !simState.isDark;
            updateDarkModeButton();
            applyHeroTheme();
        });
    }

    // Daily Rotation Toggle
    const rotationToggleBtn = document.getElementById('toggle-daily-rotation');
    if (rotationToggleBtn) {
        rotationToggleBtn.addEventListener('click', () => {
            simState.isDailyRotation = !simState.isDailyRotation;
            updateRotationButton();
            if (simState.isDailyRotation) {
                const todayThemeId = getRotatedThemeId();
                setHeroTheme(todayThemeId, true);
            }
        });
    }

    // Shape Cycle Toggle (All 12 Shapes)
    const shapeToggleBtn = document.getElementById('toggle-shape');
    if (shapeToggleBtn) {
        shapeToggleBtn.addEventListener('click', () => {
            simState.shapeIndex = (simState.shapeIndex + 1) % SHAPES.length;
            updateShapePaths();
            const shapeLabel = document.getElementById('shape-label');
            if (shapeLabel) {
                shapeLabel.textContent = SHAPES[simState.shapeIndex].label;
            }
        });
    }
}

function setHeroTheme(themeId, fromRotation = false) {
    if (themeId !== 'atmosphere' && !THEMES[themeId]) return;
    simState.themeId = themeId;
    if (!fromRotation && simState.isDailyRotation) {
        simState.isDailyRotation = false;
        updateRotationButton();
    }
    updateThemePillsUI();
    applyHeroTheme();
}

function updateThemePillsUI() {
    const themePillButtons = document.querySelectorAll('#hero-theme-pills .theme-pill');
    themePillButtons.forEach(btn => {
        if (btn.getAttribute('data-theme') === simState.themeId) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
}

function updateDarkModeButton() {
    const darkToggleBtn = document.getElementById('toggle-dark-mode');
    const darkIcon = document.getElementById('dark-mode-icon');
    const darkLabel = document.getElementById('dark-mode-label');
    if (!darkToggleBtn) return;

    if (simState.isDark) {
        darkToggleBtn.classList.add('active');
        if (darkIcon) darkIcon.className = 'fa-solid fa-sun';
        if (darkLabel) darkLabel.textContent = 'Light Mode';
    } else {
        darkToggleBtn.classList.remove('active');
        if (darkIcon) darkIcon.className = 'fa-solid fa-moon';
        if (darkLabel) darkLabel.textContent = 'Dark Mode';
    }
}

function updateRotationButton() {
    const rotationToggleBtn = document.getElementById('toggle-daily-rotation');
    const rotationLabel = document.getElementById('rotation-label');
    if (!rotationToggleBtn) return;

    if (simState.isDailyRotation) {
        rotationToggleBtn.classList.add('active');
        if (rotationLabel) rotationLabel.textContent = 'Rotation: On';
    } else {
        rotationToggleBtn.classList.remove('active');
        if (rotationLabel) rotationLabel.textContent = 'Rotation: Off';
    }
}

function updateShapePaths() {
    const newPath = SHAPES[simState.shapeIndex].path;
    const paths = document.querySelectorAll('#life-grid .shape-svg path');
    paths.forEach(pathEl => {
        pathEl.setAttribute('d', newPath);
    });
}

function applyHeroTheme() {
    const simBox = document.getElementById('macro-simulation');
    if (!simBox) return;

    const theme = getThemeById(simState.themeId);
    const variant = simState.isDark ? theme.dark : theme.light;

    simBox.style.backgroundColor = variant.bg;
    simBox.style.borderColor = simState.isDark ? '#333' : '#1a1a1a';

    const header = simBox.querySelector('.sim-phone-header');
    if (header) {
        header.style.color = variant.text;
    }

    const caption = simBox.querySelector('.caption');
    if (caption) {
        caption.style.color = simState.isDark ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.55)';
    }

    const notch = simBox.querySelector('.sim-notch');
    if (notch) {
        notch.style.backgroundColor = simState.isDark ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)';
    }

    const pastPaths = simBox.querySelectorAll('.year-dot.past .shape-svg path');
    pastPaths.forEach(p => {
        p.setAttribute('fill', variant.past);
        p.setAttribute('stroke', variant.past);
    });

    const currentPaths = simBox.querySelectorAll('.year-dot.current .shape-svg path');
    currentPaths.forEach(p => {
        p.setAttribute('fill', variant.current);
        p.setAttribute('stroke', variant.current);
    });

    const futurePaths = simBox.querySelectorAll('.year-dot.future .shape-svg path');
    futurePaths.forEach(p => {
        p.setAttribute('fill', 'none');
        p.setAttribute('stroke', variant.futureBorder);
    });
}

function initAtmosphereSpotlight() {
    const moodEl = document.getElementById('atmosphere-mood-name');
    const swatchesEl = document.getElementById('atmosphere-swatches');
    const bannerEl = document.getElementById('atmosphere-banner');

    if (moodEl) {
        moodEl.textContent = ATMOSPHERE_TODAY.mood;
    }

    if (swatchesEl) {
        swatchesEl.innerHTML = `
            <div class="atmo-swatch-pair" title="Light Variant">
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.light.bg};"></span>
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.light.past};"></span>
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.light.current};"></span>
            </div>
            <div class="atmo-swatch-pair atmo-dark" title="Dark Variant">
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.dark.bg};"></span>
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.dark.past};"></span>
                <span class="swatch-dot" style="background: ${ATMOSPHERE_TODAY.dark.current};"></span>
            </div>
        `;
    }

    if (bannerEl) {
        bannerEl.addEventListener('click', () => {
            setHeroTheme('atmosphere', false);
            const heroSection = document.getElementById('hero');
            if (heroSection) {
                heroSection.scrollIntoView({ behavior: 'smooth' });
            }
        });
    }
}

function initThemeGallery() {
    const galleryContainer = document.getElementById('theme-gallery');
    if (!galleryContainer) return;

    galleryContainer.innerHTML = '';

    Object.values(THEMES).forEach(theme => {
        const card = document.createElement('div');
        card.classList.add('theme-card');
        card.setAttribute('data-theme', theme.id);

        const badgeClass = theme.isRotatable ? 'theme-badge rotatable' : 'theme-badge';
        const badgeText = theme.isRotatable ? 'Rotatable' : theme.category;

        card.innerHTML = `
            <div class="theme-card-header">
                <span class="theme-card-title">${theme.name}</span>
                <span class="${badgeClass}">${badgeText}</span>
            </div>
            <div class="theme-variants">
                <div class="variant-row light-variant">
                    <span class="variant-label"><i class="fa-solid fa-sun"></i> Light</span>
                    <div class="swatch-group">
                        <span class="swatch-dot" style="background: ${theme.light.bg};" title="Background: ${theme.light.bg}"></span>
                        <span class="swatch-dot" style="background: ${theme.light.past};" title="Past: ${theme.light.past}"></span>
                        <span class="swatch-dot" style="background: ${theme.light.current};" title="Current: ${theme.light.current}"></span>
                        <span class="swatch-dot" style="background: ${theme.light.futureBorder};" title="Future Outline"></span>
                    </div>
                </div>
                <div class="variant-row dark-variant">
                    <span class="variant-label"><i class="fa-solid fa-moon"></i> Dark</span>
                    <div class="swatch-group">
                        <span class="swatch-dot" style="background: ${theme.dark.bg};" title="Background: ${theme.dark.bg}"></span>
                        <span class="swatch-dot" style="background: ${theme.dark.past};" title="Past: ${theme.dark.past}"></span>
                        <span class="swatch-dot" style="background: ${theme.dark.current};" title="Current: ${theme.dark.current}"></span>
                        <span class="swatch-dot" style="background: ${theme.dark.futureBorder};" title="Future Outline"></span>
                    </div>
                </div>
            </div>
        `;

        card.addEventListener('click', () => {
            setHeroTheme(theme.id, false);
            const heroSection = document.getElementById('hero');
            if (heroSection) {
                heroSection.scrollIntoView({ behavior: 'smooth' });
            }
        });

        galleryContainer.appendChild(card);
    });
}

function updateTodayRotationBadge() {
    const todayBadgeValue = document.getElementById('today-theme-name');
    if (!todayBadgeValue) return;

    const rotatedThemeId = getRotatedThemeId();
    const rotatedTheme = THEMES[rotatedThemeId];
    if (rotatedTheme) {
        todayBadgeValue.textContent = rotatedTheme.name;
    }
}

function renderMiniMacroGrid() {
    const gridContainer = document.getElementById('mini-macro-grid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';
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

    const existing = container.querySelector('.shape-no-tomorrow');
    if (existing) existing.remove();

    const shape = document.createElement('div');
    shape.classList.add('shape-no-tomorrow');
    shape.style.margin = 'auto';

    container.appendChild(shape);
}

function renderMicroVsYesterday() {
    const container = document.getElementById('micro-vs-yesterday');
    if (!container) return;

    const existing = container.querySelector('.vs-container');
    if (existing) existing.remove();

    const vsContainer = document.createElement('div');
    vsContainer.classList.add('vs-container');

    const pastShape = document.createElement('div');
    pastShape.classList.add('shape-past');
    pastShape.title = 'Yesterday';

    const presentShape = document.createElement('div');
    presentShape.classList.add('shape-present');
    presentShape.title = 'Today';

    vsContainer.appendChild(pastShape);
    vsContainer.appendChild(presentShape);

    container.appendChild(vsContainer);
}

function renderMicroThisYear() {
    const gridContainer = document.getElementById('this-year-grid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';
    const totalWeeks = 52;
    const now = new Date();
    const startOfYear = new Date(now.getFullYear(), 0, 1);
    const dayOfYear = Math.floor((now - startOfYear) / (1000 * 60 * 60 * 24));
    const currentWeekIndex = Math.min(totalWeeks - 1, Math.floor(dayOfYear / 7));
    const cols = 4;

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;

    for (let i = 0; i < totalWeeks; i++) {
        const dot = document.createElement('div');
        dot.classList.add('year-dot');

        if (i < currentWeekIndex) {
            dot.classList.add('past');
        } else if (i === currentWeekIndex) {
            dot.classList.add('current');
            dot.title = `Week ${i + 1} of 52`;
        } else {
            dot.classList.add('future');
        }
        gridContainer.appendChild(dot);
    }
}

function renderMicroHealth() {
    const gridContainer = document.getElementById('health-event-grid');
    if (!gridContainer) return;

    gridContainer.innerHTML = '';
    const totalDays = 25;
    const daysPassed = 19;
    const cols = 5;

    const data = [
        { val: '10.8k', opacity: 0.9 },
        { val: '16.9k', opacity: 1.0 },
        { val: '13.8k', opacity: 0.95 },
        { val: '14.3k', opacity: 0.98 },
        { val: '2.2k',  opacity: 0.2 },
        { val: '3.4k',  opacity: 0.3 },
        { val: '2.4k',  opacity: 0.25 },
        { val: '4.0k',  opacity: 0.4 },
        { val: '12.8k', opacity: 0.9 },
        { val: '19.8k', opacity: 1.0 },
        { val: '13.2k', opacity: 0.95 },
        { val: '8.3k',  opacity: 0.8 },
        { val: '9.5k',  opacity: 0.95 },
        { val: '8.9k',  opacity: 0.85 },
        { val: '13.2k', opacity: 0.98 },
        { val: '20.5k', opacity: 1.0 },
        { val: '6.1k',  opacity: 0.6 },
        { val: '13.4k', opacity: 0.95 },
        { val: '1.2k',  opacity: 0.1 },
    ];

    gridContainer.style.gridTemplateColumns = `repeat(${cols}, 1fr)`;
    gridContainer.classList.add('rhombus-grid');

    for (let i = 0; i < totalDays; i++) {
        const dot = document.createElement('div');
        dot.classList.add('health-shape');

        if (i < daysPassed) {
            dot.classList.add('past');
            const opacity = data[i] ? data[i].opacity : 0.5;
            dot.style.opacity = opacity;

            const label = document.createElement('span');
            label.textContent = data[i] ? data[i].val : '';
            dot.appendChild(label);
        } else if (i === daysPassed) {
            dot.classList.add('current');
            dot.title = 'Today';
            dot.style.opacity = 1.0;

            const label = document.createElement('span');
            label.textContent = '14';
            dot.appendChild(label);
        } else {
            dot.classList.add('future');
        }
        gridContainer.appendChild(dot);
    }
}
