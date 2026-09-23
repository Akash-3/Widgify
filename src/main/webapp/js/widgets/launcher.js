// Quick Web Launcher Widget Renderer (Connected to /launcher Backend API)
function renderLauncherWidget(container, widget) {
    const API_BASE = (window.WIDGIFY_CONTEXT || '').replace(/\/$/, '');

    fetch(`${API_BASE}/launcher`, { credentials: 'same-origin' })
        .then(res => res.json())
        .then(data => {
            let shortcuts = [
                { id: 'vscode', name: 'VS Code', icon: '💻', url: 'https://vscode.dev/' },
                { id: 'github', name: 'GitHub', icon: '🐙', url: 'https://github.com/' },
                { id: 'google', name: 'Google', icon: '🔍', url: 'https://www.google.com/' },
                { id: 'mdn', name: 'MDN Docs', icon: '📚', url: 'https://developer.mozilla.org/' },
                { id: 'stackoverflow', name: 'StackOverflow', icon: '💬', url: 'https://stackoverflow.com/' },
                { id: 'chatgpt', name: 'ChatGPT', icon: '🤖', url: 'https://chatgpt.com/' }
            ];

            if (data && data.success && data.shortcuts && data.shortcuts.length > 0) {
                shortcuts = data.shortcuts;
            }

            renderShortcutGrid(container, widget, shortcuts, API_BASE);
        })
        .catch(() => {
            const fallbackShortcuts = [
                { id: 'vscode', name: 'VS Code', icon: '💻', url: 'https://vscode.dev/' },
                { id: 'github', name: 'GitHub', icon: '🐙', url: 'https://github.com/' },
                { id: 'google', name: 'Google', icon: '🔍', url: 'https://www.google.com/' },
                { id: 'mdn', name: 'MDN Docs', icon: '📚', url: 'https://developer.mozilla.org/' },
                { id: 'stackoverflow', name: 'StackOverflow', icon: '💬', url: 'https://stackoverflow.com/' },
                { id: 'chatgpt', name: 'ChatGPT', icon: '🤖', url: 'https://chatgpt.com/' }
            ];
            renderShortcutGrid(container, widget, fallbackShortcuts, API_BASE);
        });
}

function renderShortcutGrid(container, widget, shortcuts, API_BASE) {
    container.innerHTML = `
        <div class="launcher-widget-wrapper" id="launcher-wrapper-${widget.id}">
            <div class="launcher-grid-tiles" id="launcher-grid-${widget.id}">
                ${shortcuts.map(app => `
                    <div class="app-tile-item" data-appid="${app.id}" data-name="${escapeHtml(app.name)}" data-url="${escapeHtml(app.url || '')}">
                        <div class="app-tile-icon-box">${app.icon || '🌐'}</div>
                        <span class="app-tile-label">${escapeHtml(app.name)}</span>
                    </div>
                `).join('')}
            </div>
            <div id="launcher-status-${widget.id}" class="launcher-status-footer">
                Click a shortcut tile to open website
            </div>
        </div>
    `;

    bindTileEvents(container, widget, API_BASE);
}

function bindTileEvents(container, widget, API_BASE) {
    container.querySelectorAll('.app-tile-item').forEach(tile => {
        tile.onclick = () => {
            const appId = tile.dataset.appid;
            const appName = tile.dataset.name;
            const defaultUrl = tile.dataset.url;
            const statusEl = document.getElementById(`launcher-status-${widget.id}`);

            if (statusEl) {
                statusEl.textContent = `Opening ${appName}...`;
                statusEl.style.color = 'var(--accent)';
            }

            const body = new URLSearchParams();
            body.append('appId', appId);

            fetch(`${API_BASE}/launcher`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body
            })
            .then(res => res.json())
            .then(data => {
                const targetUrl = (data && data.success && data.url) ? data.url : defaultUrl;
                if (targetUrl) {
                    window.open(targetUrl, '_blank');
                }
                if (statusEl) {
                    statusEl.textContent = `${appName} opened in new tab.`;
                    statusEl.style.color = 'var(--success)';
                    setTimeout(() => {
                        if (statusEl) {
                            statusEl.textContent = 'Click a shortcut tile to open website';
                            statusEl.style.color = 'var(--text-muted)';
                        }
                    }, 3500);
                }
            })
            .catch(() => {
                if (defaultUrl) {
                    window.open(defaultUrl, '_blank');
                }
                if (statusEl) {
                    statusEl.textContent = `${appName} opened.`;
                    statusEl.style.color = 'var(--success)';
                }
            });
        };
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('launcher', renderLauncherWidget);
}
