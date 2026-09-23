// Sticky Notes Widget Renderer (Connected to /notes API with Nothing OS Styling)
function renderNotesWidget(container, widget) {
    const API_BASE = (window.WIDGIFY_CONTEXT || '').replace(/\/$/, '');
    let currentView = 'list';
    let currentNote = null;

    container.innerHTML = `
        <div class="notes-widget-container" id="notes-container-${widget.id}">
            <div class="notes-header-bar">
                <span class="widget-sub-heading">STICKY NOTES</span>
                <button class="widget-action-btn" id="notes-add-btn-${widget.id}" title="Create Note">+</button>
            </div>
            <div id="notes-body-content-${widget.id}" class="notes-body-area">
                <div class="notes-status-text">Loading notes...</div>
            </div>
        </div>
    `;

    const bodyContent = document.getElementById(`notes-body-content-${widget.id}`);
    const addBtn = document.getElementById(`notes-add-btn-${widget.id}`);

    function loadNotes() {
        currentView = 'list';
        fetch(`${API_BASE}/notes`, { credentials: 'same-origin' })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.notes) {
                    renderListView(data.notes);
                } else {
                    bodyContent.innerHTML = `<div class="notes-status-text error">Failed to load notes.</div>`;
                }
            })
            .catch(() => {
                bodyContent.innerHTML = `<div class="notes-status-text error">Network error.</div>`;
            });
    }

    function renderListView(notes) {
        if (!notes || notes.length === 0) {
            bodyContent.innerHTML = `
                <div class="notes-status-text">
                    No notes yet.<br>Click <strong>+</strong> to add a note.
                </div>
            `;
            return;
        }

        const colors = ['#1A1C16', '#141824', '#1C161C'];

        bodyContent.innerHTML = `
            <div class="notes-grid-view">
                ${notes.map((n, idx) => {
                    const cardBg = colors[idx % colors.length];
                    return `
                        <div class="note-card-tile" style="background-color: ${cardBg};" data-id="${n.id}">
                            <div class="note-card-top">
                                <span class="note-card-title">${escapeHtml(n.title)}</span>
                                <button class="widget-action-btn delete-note-btn" data-id="${n.id}" title="Delete Note">✕</button>
                            </div>
                            <div class="note-card-body">${escapeHtml(n.content || 'Empty note')}</div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        bodyContent.querySelectorAll('.delete-note-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                deleteNote(btn.dataset.id);
            });
        });

        bodyContent.querySelectorAll('.note-card-tile').forEach(card => {
            card.addEventListener('click', () => {
                const note = notes.find(n => n.id == card.dataset.id);
                if (note) renderEditorView(note);
            });
        });
    }

    function renderEditorView(note = null) {
        currentView = 'edit';
        currentNote = note;

        bodyContent.innerHTML = `
            <div class="notes-editor-form">
                <input type="text" id="note-title-input-${widget.id}" class="notes-input-field" placeholder="Note Title" value="${note ? escapeHtml(note.title) : ''}" />
                <textarea id="note-content-input-${widget.id}" class="notes-textarea-field" rows="3" placeholder="Write contents here...">${note ? escapeHtml(note.content || '') : ''}</textarea>
                <div class="notes-editor-actions">
                    <button class="btn-secondary-sm" id="note-cancel-btn-${widget.id}">Cancel</button>
                    <button class="btn-primary-sm" id="note-save-btn-${widget.id}">Save</button>
                </div>
            </div>
        `;

        document.getElementById(`note-cancel-btn-${widget.id}`).onclick = loadNotes;
        document.getElementById(`note-save-btn-${widget.id}`).onclick = saveNote;
    }

    function saveNote() {
        const titleInput = document.getElementById(`note-title-input-${widget.id}`);
        const contentInput = document.getElementById(`note-content-input-${widget.id}`);
        
        const title = titleInput ? titleInput.value.trim() : 'Untitled Note';
        const content = contentInput ? contentInput.value.trim() : '';

        const body = new URLSearchParams();
        if (currentNote) {
            body.append('action', 'update');
            body.append('id', currentNote.id);
        } else {
            body.append('action', 'create');
        }
        body.append('title', title || 'Untitled Note');
        body.append('content', content);

        fetch(`${API_BASE}/notes`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) loadNotes();
            });
    }

    function deleteNote(id) {
        const body = new URLSearchParams();
        body.append('action', 'delete');
        body.append('id', id);

        fetch(`${API_BASE}/notes`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) loadNotes();
            });
    }

    addBtn.onclick = () => renderEditorView(null);

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    loadNotes();
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('notes', renderNotesWidget);
}
