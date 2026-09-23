// Real System Health Monitor Widget Renderer (Nothing OS Style + Live Telemetry API)
function renderSystemWidget(container, widget) {
    const API_BASE = (window.WIDGIFY_CONTEXT || '').replace(/\/$/, '');

    if (container.dataset.intervalId) {
        clearInterval(parseInt(container.dataset.intervalId));
    }

    const r = 24;
    const circ = 2 * Math.PI * r;

    container.innerHTML = `
        <div class="system-widget-wrapper" style="display:flex; flex-direction:column; height:100%; justify-content:space-between;">
            <div class="system-rings-container" id="sys-rings-${widget.id}">
                <div class="ring-gauge-item">
                    <div class="ring-svg-wrapper">
                        <svg width="60" height="60" viewBox="0 0 60 60">
                            <circle cx="30" cy="30" r="${r}" stroke="#1F1F1F" stroke-width="5" fill="none" />
                            <circle id="sys-cpu-circle-${widget.id}" cx="30" cy="30" r="${r}" stroke="#32D583" stroke-width="5" fill="none"
                                    stroke-dasharray="${circ}" stroke-dashoffset="${circ}"
                                    stroke-linecap="round" transform="rotate(-90 30 30)" />
                        </svg>
                        <span class="ring-value-text" id="sys-cpu-text-${widget.id}">--%</span>
                    </div>
                    <span class="ring-label-text">SERVER CPU</span>
                </div>

                <div class="ring-gauge-item">
                    <div class="ring-svg-wrapper">
                        <svg width="60" height="60" viewBox="0 0 60 60">
                            <circle cx="30" cy="30" r="${r}" stroke="#1F1F1F" stroke-width="5" fill="none" />
                            <circle id="sys-ram-circle-${widget.id}" cx="30" cy="30" r="${r}" stroke="#5B8CFF" stroke-width="5" fill="none"
                                    stroke-dasharray="${circ}" stroke-dashoffset="${circ}"
                                    stroke-linecap="round" transform="rotate(-90 30 30)" />
                        </svg>
                        <span class="ring-value-text" id="sys-ram-text-${widget.id}">--%</span>
                    </div>
                    <span class="ring-label-text">SERVER RAM</span>
                </div>

                <div class="ring-gauge-item">
                    <div class="ring-svg-wrapper">
                        <svg width="60" height="60" viewBox="0 0 60 60">
                            <circle cx="30" cy="30" r="${r}" stroke="#1F1F1F" stroke-width="5" fill="none" />
                            <circle id="sys-batt-circle-${widget.id}" cx="30" cy="30" r="${r}" stroke="#B47CFF" stroke-width="5" fill="none"
                                    stroke-dasharray="${circ}" stroke-dashoffset="${circ}"
                                    stroke-linecap="round" transform="rotate(-90 30 30)" />
                        </svg>
                        <span class="ring-value-text" id="sys-batt-text-${widget.id}">--</span>
                    </div>
                    <span class="ring-label-text">SERVER JVM</span>
                </div>
            </div>
            <div id="sys-subtext-${widget.id}" style="font-size:10.5px; color:var(--text-muted); text-align:center; margin-top:2px;">
                Fetching server telemetry...
            </div>
        </div>
    `;

    function updateMetrics() {
        fetch(`${API_BASE}/system-health`, { credentials: 'same-origin' })
            .then(res => {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(data => {
                if (!data.success) throw new Error(data.message || 'API error');

                const cpuText = document.getElementById(`sys-cpu-text-${widget.id}`);
                const cpuCircle = document.getElementById(`sys-cpu-circle-${widget.id}`);
                const ramText = document.getElementById(`sys-ram-text-${widget.id}`);
                const ramCircle = document.getElementById(`sys-ram-circle-${widget.id}`);
                const battText = document.getElementById(`sys-batt-text-${widget.id}`);
                const battCircle = document.getElementById(`sys-batt-circle-${widget.id}`);
                const subtext = document.getElementById(`sys-subtext-${widget.id}`);

                if (!cpuText || !ramText || !subtext) return;

                // CPU
                const cpuPct = Math.min(100, Math.max(0, data.cpuUsage || 0));
                cpuText.textContent = `${Math.round(cpuPct)}%`;
                if (cpuCircle) {
                    const cpuOffset = circ * (1 - cpuPct / 100);
                    cpuCircle.setAttribute('stroke-dashoffset', cpuOffset);
                }

                // RAM
                const ramPct = Math.min(100, Math.max(0, data.memoryUsage || 0));
                ramText.textContent = `${Math.round(ramPct)}%`;
                if (ramCircle) {
                    const ramOffset = circ * (1 - ramPct / 100);
                    ramCircle.setAttribute('stroke-dashoffset', ramOffset);
                }

                // Server JVM Memory / Status
                if (data.batteryAvailable && data.batteryLevel >= 0) {
                    const battPct = Math.min(100, Math.max(0, data.batteryLevel));
                    battText.textContent = `${battPct}%`;
                    if (battCircle) {
                        const battOffset = circ * (1 - battPct / 100);
                        battCircle.setAttribute('stroke-dashoffset', battOffset);
                    }
                    subtext.textContent = `Tomcat Server • RAM: ${data.memoryUsed || ''} / ${data.memoryTotal || ''}`;
                } else {
                    battText.textContent = 'OK';
                    if (battCircle) {
                        battCircle.setAttribute('stroke-dashoffset', 0);
                    }
                    subtext.textContent = `Tomcat Server • RAM: ${data.memoryUsed || ''} / ${data.memoryTotal || ''}`;
                }
            })
            .catch(() => {
                const subtext = document.getElementById(`sys-subtext-${widget.id}`);
                if (subtext) {
                    subtext.textContent = 'Server telemetry unavailable';
                    subtext.style.color = 'var(--text-muted)';
                }
            });
    }

    updateMetrics();
    const timerId = setInterval(updateMetrics, 2500);
    container.dataset.intervalId = timerId;
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('system', renderSystemWidget);
}
