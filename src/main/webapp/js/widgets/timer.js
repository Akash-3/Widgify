// Timer Widget Renderer
function renderTimerWidget(container, widget) {
    let secondsLeft = 25 * 60;
    let isRunning = false;
    let intervalId = null;

    container.innerHTML = `
        <div class="timer-display">
            <div class="timer-countdown" id="timer-val-${widget.id}">25:00</div>
            <div class="timer-controls">
                <button class="timer-btn primary" id="timer-toggle-${widget.id}">START</button>
                <button class="timer-btn" id="timer-reset-${widget.id}">RESET</button>
            </div>
            <div class="timer-presets">
                <span class="preset-chip" id="preset-25-${widget.id}">25m</span>
                <span class="preset-chip" id="preset-5-${widget.id}">5m</span>
                <span class="preset-chip" id="preset-50-${widget.id}">50m</span>
            </div>
        </div>
    `;

    const valEl = document.getElementById(`timer-val-${widget.id}`);
    const toggleBtn = document.getElementById(`timer-toggle-${widget.id}`);
    const resetBtn = document.getElementById(`timer-reset-${widget.id}`);

    function updateDisplay() {
        const mins = Math.floor(secondsLeft / 60);
        const secs = secondsLeft % 60;
        valEl.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    }

    function setPreset(mins) {
        secondsLeft = mins * 60;
        if (isRunning) {
            clearInterval(intervalId);
            isRunning = false;
            toggleBtn.textContent = 'START';
        }
        updateDisplay();
    }

    toggleBtn.addEventListener('click', () => {
        if (!isRunning) {
            isRunning = true;
            toggleBtn.textContent = 'PAUSE';
            intervalId = setInterval(() => {
                if (secondsLeft > 0) {
                    secondsLeft--;
                    updateDisplay();
                } else {
                    clearInterval(intervalId);
                    isRunning = false;
                    toggleBtn.textContent = 'START';
                }
            }, 1000);
        } else {
            clearInterval(intervalId);
            isRunning = false;
            toggleBtn.textContent = 'START';
        }
    });

    resetBtn.addEventListener('click', () => {
        setPreset(25);
    });

    document.getElementById(`preset-25-${widget.id}`).addEventListener('click', () => setPreset(25));
    document.getElementById(`preset-5-${widget.id}`).addEventListener('click', () => setPreset(5));
    document.getElementById(`preset-50-${widget.id}`).addEventListener('click', () => setPreset(50));

    updateDisplay();
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('timer', renderTimerWidget);
}
