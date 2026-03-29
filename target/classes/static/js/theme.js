/* ============================================================
   CEREBRO METRON — THEME TOGGLE SCRIPT
   Include at end of <body> on every page
   ============================================================ */
(function () {
    'use strict';

    const STORAGE_KEY = 'cm-theme';
    const LIGHT_CLASS = 'light-mode';

    /* ── Particle configs ────────────────────────────────────── */
    const DARK_PARTICLES  = ['rgba(80,160,255,',  'rgba(120,200,255,', 'rgba(200,230,255,', 'rgba(30,100,220,'];
    const LIGHT_PARTICLES = ['rgba(80,120,220,',  'rgba(100,150,240,', 'rgba(60,100,200,',  'rgba(40,80,180,'];
    const DARK_CONN       = 'rgba(60,130,255,';
    const LIGHT_CONN      = 'rgba(80,120,220,';

    /* ── State ───────────────────────────────────────────────── */
    let isLight = localStorage.getItem(STORAGE_KEY) === 'light';

    /* ── Apply on load (before paint) ───────────────────────── */
    function applyTheme(light, animate) {
        if (light) {
            document.body.classList.add(LIGHT_CLASS);
        } else {
            document.body.classList.remove(LIGHT_CLASS);
        }

        const btn = document.getElementById('cm-theme-toggle');
        if (btn) {
            btn.setAttribute('title', light ? 'Switch to Dark Mode' : 'Switch to Light Mode');
            btn.innerHTML = light
                ? '<i class="fa fa-moon-o"></i>'
                : '<i class="fa fa-sun-o"></i>';
        }

        /* Re-colour particles if canvas is running */
        if (window.__cmParticles) {
            const colors = light ? LIGHT_PARTICLES : DARK_PARTICLES;
            window.__cmParticles.forEach(p => {
                p.color = colors[Math.floor(Math.random() * colors.length)];
            });
            window.__cmConnColor = light ? LIGHT_CONN : DARK_CONN;
        }
    }

    /* ── Toggle ──────────────────────────────────────────────── */
    function toggle() {
        isLight = !isLight;
        localStorage.setItem(STORAGE_KEY, isLight ? 'light' : 'dark');
        applyTheme(isLight, true);
    }

    /* ── Inject button ───────────────────────────────────────── */
    function injectButton() {
        if (document.getElementById('cm-theme-toggle')) return;
        const btn = document.createElement('button');
        btn.id = 'cm-theme-toggle';
        btn.setAttribute('aria-label', 'Toggle theme');
        btn.addEventListener('click', toggle);
        document.body.appendChild(btn);
    }

    /* ── Patch particle system ───────────────────────────────── */
    /* Pages expose particles array as window.__cmParticles after init */

    /* ── Init ────────────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        injectButton();
        applyTheme(isLight, false);
    });

    /* Apply immediately too (pre-DOM for flicker prevention) */
    applyTheme(isLight, false);

    /* Public API */
    window.cmTheme = { toggle, isLight: () => isLight };
})();