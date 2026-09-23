// Widgify Core Dashboard Controller & Widget Engine (JavaFX WebView Session Cookie Fix)
document.addEventListener('DOMContentLoaded', () => {
    const contextPath = (window.WIDGIFY_CONTEXT || '/widgify').replace(/\/$/, '');
    const origin = (window.location && window.location.origin && window.location.origin !== 'null') 
        ? window.location.origin 
        : '';
    const API_BASE = origin ? (origin + contextPath) : contextPath;

    let widgetsState = [];
    let isLayoutLocked = false;
    const canvas = document.getElementById('widgetCanvas');
    const layoutLockBtn = document.getElementById('layoutLockBtn');
    const layoutLockIcon = document.getElementById('layoutLockIcon');
    const layoutLockText = document.getElementById('layoutLockText');
    const addWidgetModal = document.getElementById('addWidgetModal');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const settingsModal = document.getElementById('settingsModal');
    const closeSettingsModalBtn = document.getElementById('closeSettingsModalBtn');
    const searchInput = document.getElementById('widgetSearchInput');

    const navDashboardItem = document.getElementById('navDashboardItem');
    const navWidgetsItem = document.getElementById('navWidgetsItem');
    const navSettingsItem = document.getElementById('navSettingsItem');

    // 1. Load User Settings
    function loadUserSettings() {
        fetch(`${API_BASE}/settings`, { credentials: 'same-origin' })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.settings) {
                    isLayoutLocked = (data.settings.layoutMode === 'locked');
                    applyLayoutLockState();
                }
            })
            .catch(() => console.warn('Could not load user settings.'));
    }

    // 2. Fetch and Render Widgets
    function loadWidgets() {
        showLoadingState();
        const requestUrl = `${API_BASE}/widgets`;
        console.log("[WIDGIFY DIAGNOSTIC] Context:", window.WIDGIFY_CONTEXT, "| Location:", window.location.href);
        console.log("[WIDGIFY DIAGNOSTIC] Requesting widgets at:", requestUrl);

        fetch(requestUrl, { credentials: 'same-origin' })
            .then(res => {
                console.log("[WIDGIFY DIAGNOSTIC] Response status:", res.status, res.statusText, "URL:", res.url);
                if (!res.ok) {
                    return res.text().then(text => {
                        console.error("[WIDGIFY DIAGNOSTIC] Non-200 response:", res.status, text);
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    });
                }
                return res.json();
            })
            .then(data => {
                console.log("[WIDGIFY DIAGNOSTIC] Received widgets payload:", data);
                if (data.success) {
                    widgetsState = data.widgets || [];
                    renderDashboardCanvas();
                } else {
                    showErrorState(data.message || 'Failed to load widgets.');
                }
            })
            .catch(err => {
                console.error("[WIDGIFY DIAGNOSTIC] Fetch exception:", err);
                showErrorState('We couldn\'t connect to the local Widgify service.');
            });
    }

    function showLoadingState() {
        if (!canvas) return;
        canvas.innerHTML = `
            <div class="canvas-message-state">
                <div class="brand-name" style="letter-spacing:4px; font-size:16px; margin-bottom:8px;">W I D G I F Y</div>
                <h3>Loading workspace...</h3>
                <p>Retrieving your desktop layout.</p>
            </div>
        `;
    }

    function showErrorState(msg) {
        if (!canvas) return;
        canvas.innerHTML = `
            <div class="canvas-message-state">
                <span class="error-state-icon">◌</span>
                <h3>Widgets unavailable</h3>
                <p>${escapeHtml(msg)}</p>
                <button class="btn-widgify-retry" id="retryLoadWidgetsBtn">Retry</button>
            </div>
        `;
        const retryBtn = document.getElementById('retryLoadWidgetsBtn');
        if (retryBtn) {
            retryBtn.onclick = () => loadWidgets();
        }
    }

    function renderDashboardCanvas() {
        if (!canvas) return;
        if (!widgetsState || widgetsState.length === 0) {
            canvas.innerHTML = `
                <div class="canvas-message-state">
                    <span style="font-size:32px; margin-bottom: 8px;">🧩</span>
                    <h3>Your workspace is empty</h3>
                    <p>Add widgets to customize your desktop productivity space.</p>
                    <button class="btn-add-widget btn-open-add-widget">+ Add Widget</button>
                </div>
            `;
            bindAddWidgetButtons();
            return;
        }

        canvas.innerHTML = '';
        widgetsState.forEach(widget => {
            const card = createWidgetCardElement(widget);
            canvas.appendChild(card);
        });

        applyLayoutLockState();
        bindAddWidgetButtons();
    }

    function createWidgetCardElement(widget) {
        const card = document.createElement('div');
        card.className = 'widget-card';
        card.id = `widget-card-${widget.id}`;
        card.dataset.id = widget.id;
        card.dataset.title = (widget.title || widget.widgetType).toLowerCase();

        const colStart = (widget.positionX != null ? widget.positionX : 0) + 1;
        const rowStart = (widget.positionY != null ? widget.positionY : 0) + 1;
        const colSpan = widget.width || 4;
        const rowSpan = widget.height || 2;

        card.style.gridColumn = `${colStart} / span ${colSpan}`;
        card.style.gridRow = `${rowStart} / span ${rowSpan}`;

        // Header
        const header = document.createElement('div');
        header.className = 'widget-header';
        header.innerHTML = `
            <div class="widget-title-area">
                <span class="drag-handle" title="Drag to move">⋮⋮</span>
                <span class="widget-header-title">${escapeHtml(widget.title || widget.widgetType)}</span>
            </div>
            <div class="widget-controls">
                <button class="widget-action-btn delete-btn" data-id="${widget.id}" title="Remove Widget">✕</button>
            </div>
        `;

        // Body
        const body = document.createElement('div');
        body.className = 'widget-body';
        body.id = `widget-body-${widget.id}`;

        // Resize Handle
        const resizeHandle = document.createElement('div');
        resizeHandle.className = 'resize-handle';
        resizeHandle.title = 'Resize Widget';

        card.appendChild(header);
        card.appendChild(body);
        card.appendChild(resizeHandle);

        // Attach Renderer
        const renderFn = window.WidgetRegistry ? window.WidgetRegistry.get(widget.widgetType) : null;
        if (renderFn) {
            renderFn(body, widget);
        } else {
            body.innerHTML = `<div style="font-size:12px; color:var(--text-muted); text-align:center; padding:12px;">Unknown widget type: ${escapeHtml(widget.widgetType)}</div>`;
        }

        // Attach Event Handlers
        header.querySelector('.delete-btn').addEventListener('click', () => deleteWidget(widget.id));
        setupDragAndDrop(card, header.querySelector('.drag-handle'), widget, colSpan, rowSpan);
        setupResize(card, resizeHandle, widget);

        return card;
    }

    // 3. Drag & Drop Position Persistence
    function setupDragAndDrop(card, handle, widget, colSpan, rowSpan) {
        let isDragging = false;
        let startX, startY;

        handle.addEventListener('mousedown', (e) => {
            if (isLayoutLocked) return;
            e.preventDefault();
            isDragging = true;
            card.classList.add('dragging');
            startX = e.clientX;
            startY = e.clientY;

            const initialPosX = widget.positionX || 0;
            const initialPosY = widget.positionY || 0;

            function onMouseMove(moveEvent) {
                if (!isDragging) return;
                const dx = moveEvent.clientX - startX;
                const dy = moveEvent.clientY - startY;

                const colShift = Math.round(dx / 95);
                const rowShift = Math.round(dy / 105);

                let newPosX = Math.max(0, initialPosX + colShift);
                let newPosY = Math.max(0, initialPosY + rowShift);

                card.style.gridColumn = `${newPosX + 1} / span ${widget.width || colSpan}`;
                card.style.gridRow = `${newPosY + 1} / span ${widget.height || rowSpan}`;
            }

            function onMouseUp(upEvent) {
                if (!isDragging) return;
                isDragging = false;
                card.classList.remove('dragging');
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);

                const dx = upEvent.clientX - startX;
                const dy = upEvent.clientY - startY;

                const colShift = Math.round(dx / 95);
                const rowShift = Math.round(dy / 105);

                let finalPosX = Math.max(0, initialPosX + colShift);
                let finalPosY = Math.max(0, initialPosY + rowShift);

                if (finalPosX !== widget.positionX || finalPosY !== widget.positionY) {
                    widget.positionX = finalPosX;
                    widget.positionY = finalPosY;
                    persistWidgetPosition(widget.id, finalPosX, finalPosY);
                }
            }

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });
    }

    function persistWidgetPosition(id, posX, posY) {
        const body = new URLSearchParams();
        body.append('action', 'position');
        body.append('id', id);
        body.append('positionX', posX);
        body.append('positionY', posY);

        fetch(`${API_BASE}/widgets`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (!data.success) console.warn('Failed to persist position.');
            });
    }

    // 4. Resizing Persistence
    function setupResize(card, handle, widget) {
        let isResizing = false;
        let startX, startY, startW, startH;

        handle.addEventListener('mousedown', (e) => {
            if (isLayoutLocked) return;
            e.preventDefault();
            e.stopPropagation();
            isResizing = true;
            startX = e.clientX;
            startY = e.clientY;
            startW = widget.width || 4;
            startH = widget.height || 2;

            function onMouseMove(moveEvent) {
                if (!isResizing) return;
                const dx = moveEvent.clientX - startX;
                const dy = moveEvent.clientY - startY;

                const newW = Math.max(2, Math.min(12, startW + Math.round(dx / 95)));
                const newH = Math.max(2, Math.min(8, startH + Math.round(dy / 105)));

                card.style.gridColumn = `${(widget.positionX || 0) + 1} / span ${newW}`;
                card.style.gridRow = `${(widget.positionY || 0) + 1} / span ${newH}`;
            }

            function onMouseUp(upEvent) {
                if (!isResizing) return;
                isResizing = false;
                document.removeEventListener('mousemove', onMouseMove);
                document.removeEventListener('mouseup', onMouseUp);

                const dx = upEvent.clientX - startX;
                const dy = upEvent.clientY - startY;

                const finalW = Math.max(2, Math.min(12, startW + Math.round(dx / 95)));
                const finalH = Math.max(2, Math.min(8, startH + Math.round(dy / 105)));

                if (finalW !== widget.width || finalH !== widget.height) {
                    widget.width = finalW;
                    widget.height = finalH;
                    persistWidgetSize(widget.id, finalW, finalH);
                }
            }

            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
        });
    }

    function persistWidgetSize(id, width, height) {
        const body = new URLSearchParams();
        body.append('action', 'resize');
        body.append('id', id);
        body.append('width', width);
        body.append('height', height);

        fetch(`${API_BASE}/widgets`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (!data.success) console.warn('Failed to persist size.');
            });
    }

    // 5. Layout Lock Manager
    function applyLayoutLockState() {
        if (isLayoutLocked) {
            document.body.classList.add('layout-locked');
            if (layoutLockBtn) layoutLockBtn.className = 'btn-layout-toggle locked';
            if (layoutLockIcon) layoutLockIcon.textContent = '🔒';
            if (layoutLockText) layoutLockText.textContent = 'Layout Locked';
        } else {
            document.body.classList.remove('layout-locked');
            if (layoutLockBtn) layoutLockBtn.className = 'btn-layout-toggle';
            if (layoutLockIcon) layoutLockIcon.textContent = '🔓';
            if (layoutLockText) layoutLockText.textContent = 'Edit Layout';
        }
    }

    if (layoutLockBtn) {
        layoutLockBtn.addEventListener('click', () => {
            isLayoutLocked = !isLayoutLocked;
            applyLayoutLockState();

            const body = new URLSearchParams();
            body.append('layoutMode', isLayoutLocked ? 'locked' : 'freeform');
            fetch(`${API_BASE}/settings`, { method: 'POST', credentials: 'same-origin', body: body });
        });
    }

    // 6. Navigation Buttons
    if (navDashboardItem) {
        navDashboardItem.onclick = () => {
            setActiveNavItem(navDashboardItem);
            closeSettingsModal();
            closeAddWidgetModal();
        };
    }

    if (navWidgetsItem) {
        navWidgetsItem.onclick = () => {
            setActiveNavItem(navWidgetsItem);
            openAddWidgetModal();
        };
    }

    if (navSettingsItem) {
        navSettingsItem.onclick = () => {
            setActiveNavItem(navSettingsItem);
            openSettingsModal();
        };
    }

    function setActiveNavItem(activeEl) {
        [navDashboardItem, navWidgetsItem, navSettingsItem].forEach(item => {
            if (item) item.classList.remove('active');
        });
        if (activeEl) activeEl.classList.add('active');
    }

    // 7. Settings Modal Controls
    if (closeSettingsModalBtn) closeSettingsModalBtn.onclick = closeSettingsModal;
    if (settingsModal) {
        settingsModal.addEventListener('click', (e) => {
            if (e.target === settingsModal) closeSettingsModal();
        });
    }

    function openSettingsModal() {
        if (settingsModal) settingsModal.classList.add('active');
    }

    function closeSettingsModal() {
        if (settingsModal) settingsModal.classList.remove('active');
        setActiveNavItem(navDashboardItem);
    }

    const setFreeformBtn = document.getElementById('setFreeformBtn');
    const setLockedBtn = document.getElementById('setLockedBtn');

    if (setFreeformBtn && setLockedBtn) {
        setFreeformBtn.onclick = () => {
            isLayoutLocked = false;
            applyLayoutLockState();
            setFreeformBtn.classList.add('active');
            setLockedBtn.classList.remove('active');
            const body = new URLSearchParams();
            body.append('layoutMode', 'freeform');
            fetch(`${API_BASE}/settings`, { method: 'POST', credentials: 'same-origin', body: body });
        };

        setLockedBtn.onclick = () => {
            isLayoutLocked = true;
            applyLayoutLockState();
            setLockedBtn.classList.add('active');
            setFreeformBtn.classList.remove('active');
            const body = new URLSearchParams();
            body.append('layoutMode', 'locked');
            fetch(`${API_BASE}/settings`, { method: 'POST', credentials: 'same-origin', body: body });
        };
    }

    // 8. Add Widget Modal Controls
    function bindAddWidgetButtons() {
        document.querySelectorAll('.btn-open-add-widget').forEach(btn => {
            btn.onclick = openAddWidgetModal;
        });
    }

    if (closeModalBtn) closeModalBtn.onclick = closeAddWidgetModal;
    if (addWidgetModal) {
        addWidgetModal.addEventListener('click', (e) => {
            if (e.target === addWidgetModal) closeAddWidgetModal();
        });
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeAddWidgetModal();
            closeSettingsModal();
        }
    });

    function openAddWidgetModal() {
        if (addWidgetModal) addWidgetModal.classList.add('active');
    }

    function closeAddWidgetModal() {
        if (addWidgetModal) addWidgetModal.classList.remove('active');
        setActiveNavItem(navDashboardItem);
    }

    document.querySelectorAll('.add-widget-option').forEach(option => {
        option.addEventListener('click', () => {
            const type = option.dataset.type;
            if (!type) return;
            addNewWidget(type);
            closeAddWidgetModal();
        });
    });

    function addNewWidget(widgetType) {
        const body = new URLSearchParams();
        body.append('action', 'create');
        body.append('widgetType', widgetType);

        fetch(`${API_BASE}/widgets`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    loadWidgets();
                } else {
                    alert(data.message || 'Failed to add widget.');
                }
            });
    }

    // 9. Delete Widget
    function deleteWidget(id) {
        if (!confirm('Remove this widget from your dashboard?')) return;
        const body = new URLSearchParams();
        body.append('action', 'delete');
        body.append('id', id);

        fetch(`${API_BASE}/widgets`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    const card = document.getElementById(`widget-card-${id}`);
                    if (card) card.remove();
                    widgetsState = widgetsState.filter(w => w.id !== id);
                    if (widgetsState.length === 0) renderDashboardCanvas();
                }
            });
    }

    // 10. Real-Time Search Filter
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const query = searchInput.value.toLowerCase().trim();
            document.querySelectorAll('.widget-card').forEach(card => {
                const title = card.dataset.title || '';
                const bodyText = card.innerText.toLowerCase();
                if (query === '' || title.includes(query) || bodyText.includes(query)) {
                    card.style.display = 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Initialize Dashboard
    loadUserSettings();
    loadWidgets();
});
