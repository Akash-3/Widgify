// Tasks & To-Do Widget Renderer (Connected to /tasks API with Nothing OS Styling)
function renderTasksWidget(container, widget) {
    const API_BASE = (window.WIDGIFY_CONTEXT || '').replace(/\/$/, '');

    container.innerHTML = `
        <div class="tasks-widget-container">
            <div class="tasks-header-bar">
                <span class="widget-sub-heading">TASKS</span>
            </div>
            <div class="task-input-group">
                <input type="text" id="new-task-title-${widget.id}" class="task-input-field" placeholder="Add a new task..." />
                <button class="btn-primary-sm" id="add-task-btn-${widget.id}">+ Add</button>
            </div>
            <div class="task-list" id="task-list-${widget.id}">
                <div class="notes-status-text">Loading tasks...</div>
            </div>
        </div>
    `;

    const listEl = document.getElementById(`task-list-${widget.id}`);
    const inputEl = document.getElementById(`new-task-title-${widget.id}`);
    const addBtn = document.getElementById(`add-task-btn-${widget.id}`);

    function loadTasks() {
        fetch(`${API_BASE}/tasks`, { credentials: 'same-origin' })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.tasks) {
                    renderTaskList(data.tasks);
                } else {
                    listEl.innerHTML = `<div class="notes-status-text error">Failed to load tasks.</div>`;
                }
            })
            .catch(() => {
                listEl.innerHTML = `<div class="notes-status-text error">Network error.</div>`;
            });
    }

    function renderTaskList(tasks) {
        if (!tasks || tasks.length === 0) {
            listEl.innerHTML = `<div class="notes-status-text">No tasks yet.</div>`;
            return;
        }

        listEl.innerHTML = tasks.map(t => `
            <div class="task-row ${t.completed ? 'completed' : ''}" data-id="${t.id}">
                <label class="task-checkbox-custom">
                    <input type="checkbox" class="task-check" data-id="${t.id}" ${t.completed ? 'checked' : ''} />
                    <span class="checkmark"></span>
                </label>
                <span class="task-title-text">${escapeHtml(t.title)}</span>
                <button class="widget-action-btn delete-task-btn" data-id="${t.id}" title="Delete Task">✕</button>
            </div>
        `).join('');

        listEl.querySelectorAll('.task-check').forEach(chk => {
            chk.addEventListener('change', () => {
                toggleTask(chk.dataset.id);
            });
        });

        listEl.querySelectorAll('.delete-task-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                deleteTask(btn.dataset.id);
            });
        });
    }

    function addTask() {
        const title = inputEl.value.trim();
        if (!title) return;

        const body = new URLSearchParams();
        body.append('action', 'create');
        body.append('title', title);

        fetch(`${API_BASE}/tasks`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    inputEl.value = '';
                    loadTasks();
                }
            });
    }

    function toggleTask(id) {
        const body = new URLSearchParams();
        body.append('action', 'toggle');
        body.append('id', id);

        fetch(`${API_BASE}/tasks`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) loadTasks();
            });
    }

    function deleteTask(id) {
        const body = new URLSearchParams();
        body.append('action', 'delete');
        body.append('id', id);

        fetch(`${API_BASE}/tasks`, { method: 'POST', credentials: 'same-origin', body: body })
            .then(res => res.json())
            .then(data => {
                if (data.success) loadTasks();
            });
    }

    addBtn.onclick = addTask;
    inputEl.onkeypress = (e) => {
        if (e.key === 'Enter') addTask();
    };

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    loadTasks();
}

if (window.WidgetRegistry) {
    window.WidgetRegistry.register('tasks', renderTasksWidget);
}
