// Weather Widget Renderer (Honest API Integration Status)
function renderWeatherWidget(container, widget) {
    let config = { location: 'Location Not Set' };
    try {
        if (widget.config) config = typeof widget.config === 'string' ? JSON.parse(widget.config) : widget.config;
    } catch(e) {}

    const locationName = config.location && config.location !== 'Location Not Set' ? config.location : 'Weather Service';

    container.innerHTML = `
        <div class="weather-nothing-card">
            <div class="weather-main-section">
                <div class="weather-huge-temp" style="font-size:24px; color:var(--text-muted);">--°C</div>
                <div class="weather-meta">
                    <div class="weather-loc-name">${escapeHtml(locationName)}</div>
                    <div class="weather-condition-text" style="color:var(--text-muted);">API Not Connected</div>
                </div>
            </div>
            <div class="weather-details-grid" style="border-top:1px solid var(--border); padding-top:6px; margin-top:8px;">
                <div class="weather-detail-item">
                    <span class="detail-label">STATUS</span>
                    <span class="detail-value" style="color:var(--text-muted);">Live API Integration in Phase 5</span>
                </div>
            </div>
        </div>
    `;

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('weather', renderWeatherWidget);
}
