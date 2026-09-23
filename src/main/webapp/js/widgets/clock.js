// Nothing OS Style Dot-Matrix Digital Clock Renderer
function renderClockWidget(container, widget) {
    container.innerHTML = `
        <div class="clock-display">
            <div class="clock-time-matrix" id="clock-time-${widget.id}">10:24 AM</div>
            <div class="clock-date-str" id="clock-date-${widget.id}">Loading date...</div>
        </div>
    `;

    function updateClock() {
        const timeEl = document.getElementById(`clock-time-${widget.id}`);
        const dateEl = document.getElementById(`clock-date-${widget.id}`);
        
        const now = new Date();
        const hours = now.getHours();
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        const displayHours = hours % 12 || 12;

        const timeStr = `${displayHours}:${minutes} ${ampm}`;
        const dateStr = now.toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });

        if (timeEl) timeEl.textContent = timeStr;
        if (dateEl) dateEl.textContent = dateStr;

        const bannerDate = document.getElementById('banner-date-str');
        if (bannerDate) {
            bannerDate.textContent = dateStr;
        }
    }

    updateClock();
    const timerId = setInterval(updateClock, 1000);
    container.dataset.intervalId = timerId;
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('clock', renderClockWidget);
}
