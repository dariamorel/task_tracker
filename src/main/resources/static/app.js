const API = '/api/v1';
const PRIORITY_LABEL = { LOW: 'Низкий', MEDIUM: 'Средний', HIGH: 'Высокий' };

const el = {
    error: document.getElementById('error'),
    taskForm: document.getElementById('task-form'),
    tasks: document.getElementById('tasks'),
    editDialog: document.getElementById('edit-dialog'),
    editForm: document.getElementById('edit-form'),
    editCancel: document.getElementById('edit-cancel'),
};

let tags = [];
let tasks = [];

const ICONS = {
    edit: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>`,
    trash: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/></svg>`,
};

async function api(path, options = {}) {
    const res = await fetch(API + path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (res.status === 204) return null;
    const body = await res.json().catch(() => null);
    if (!res.ok) throw new Error(body?.error ?? `HTTP ${res.status}`);
    return body;
}

function showError(e) {
    const message = e.message || String(e);
    if (el.editDialog.open) alert(message);
    el.error.hidden = false;
    el.error.textContent = message;
}

function clearError() {
    el.error.hidden = true;
    el.error.textContent = '';
}

function fillTagSelects() {
    document.querySelectorAll('.tag-select').forEach(select => {
        const current = select.value;
        select.replaceChildren();
        const none = document.createElement('option');
        none.value = '';
        none.textContent = 'Без тега';
        select.append(none);
        for (const tag of tags) {
            const opt = document.createElement('option');
            opt.value = tag.id;
            opt.textContent = tag.name;
            select.append(opt);
        }
        select.value = current && [...select.options].some(o => o.value === current) ? current : '';
    });
    syncPickerButtons();
}

function syncPickerButtons() {
    document.querySelectorAll('.tag-picker').forEach(picker => {
        const empty = picker.querySelector('.tag-select').value === '';
        picker.querySelector('.tag-edit').disabled = empty;
        picker.querySelector('.tag-delete').disabled = empty;
    });
}

function selectedTag(picker) {
    const id = Number(picker.querySelector('.tag-select').value);
    return tags.find(t => t.id === id);
}

async function addTag(picker) {
    const name = prompt('Название нового тега');
    if (!name || !name.trim()) return;
    const tag = await api('/tags', { method: 'POST', body: JSON.stringify({ name: name.trim() }) });
    await reload();
    picker.querySelector('.tag-select').value = String(tag.id);
    syncPickerButtons();
}

async function renameTag(picker) {
    const tag = selectedTag(picker);
    if (!tag) return;
    const name = prompt('Новое название тега', tag.name);
    if (!name || !name.trim() || name.trim() === tag.name) return;
    await api(`/tags/${tag.id}`, { method: 'PUT', body: JSON.stringify({ name: name.trim() }) });
    await reload();
}

async function deleteTag(picker) {
    const tag = selectedTag(picker);
    if (!tag) return;
    if (!confirm(`Удалить тег «${tag.name}»? У задач с этим тегом он будет снят.`)) return;
    await api(`/tags/${tag.id}`, { method: 'DELETE' });
    await reload();
}

function tagName(id) {
    return tags.find(t => t.id === id)?.name;
}

function iconButton(svg, title, onClick, danger = false) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `icon-btn${danger ? ' danger' : ''}`;
    btn.title = title;
    btn.innerHTML = svg;
    btn.onclick = () => onClick().catch(showError);
    return btn;
}

function renderTasks() {
    if (!tasks.length) {
        el.tasks.innerHTML = '<p class="empty">Пока нет задач — добавьте первую выше</p>';
        return;
    }

    el.tasks.replaceChildren(...tasks.map(task => {
        const card = document.createElement('article');
        card.className = `task-card${task.done ? ' done' : ''}`;

        const check = document.createElement('input');
        check.type = 'checkbox';
        check.className = 'task-check';
        check.checked = task.done;
        check.onchange = () => api(`/tasks/${task.id}`, {
            method: 'PUT',
            body: JSON.stringify({ ...task, done: check.checked }),
        }).then(reload).catch(showError);

        const body = document.createElement('div');
        body.className = 'task-body';

        const title = document.createElement('h3');
        title.className = 'task-title';
        title.textContent = task.title;

        const meta = document.createElement('div');
        meta.className = 'meta';

        const priority = document.createElement('span');
        priority.className = `chip chip-priority-${task.priority}`;
        priority.textContent = PRIORITY_LABEL[task.priority] ?? task.priority;
        meta.append(priority);

        if (task.tagId) {
            const tag = document.createElement('span');
            tag.className = 'chip chip-tag';
            tag.textContent = tagName(task.tagId) ?? `тег #${task.tagId}`;
            meta.append(tag);
        }

        body.append(title, meta);

        const actions = document.createElement('div');
        actions.className = 'task-actions';
        actions.append(
            iconButton(ICONS.edit, 'Изменить', async () => openEdit(task)),
            iconButton(ICONS.trash, 'Удалить', async () => {
                await api(`/tasks/${task.id}`, { method: 'DELETE' });
                await reload();
            }, true),
        );

        card.append(check, body, actions);
        return card;
    }));
}

function openEdit(task) {
    el.editForm.id.value = task.id;
    el.editForm.title.value = task.title;
    el.editForm.priority.value = task.priority;
    el.editForm.tagId.value = task.tagId ? String(task.tagId) : '';
    syncPickerButtons();
    el.editDialog.showModal();
}

async function reload() {
    try {
        clearError();
        [tags, tasks] = await Promise.all([api('/tags'), api('/tasks')]);
        fillTagSelects();
        renderTasks();
    } catch (e) {
        showError(e);
    }
}

document.querySelectorAll('.tag-picker').forEach(picker => {
    picker.querySelector('.tag-edit').innerHTML = ICONS.edit;
    picker.querySelector('.tag-delete').innerHTML = ICONS.trash;
    picker.querySelector('.tag-select').addEventListener('change', syncPickerButtons);
    picker.querySelector('.tag-add').addEventListener('click', () => addTag(picker).catch(showError));
    picker.querySelector('.tag-edit').addEventListener('click', () => renameTag(picker).catch(showError));
    picker.querySelector('.tag-delete').addEventListener('click', () => deleteTag(picker).catch(showError));
});

el.taskForm.addEventListener('submit', async e => {
    e.preventDefault();
    const data = new FormData(el.taskForm);
    const tagRaw = data.get('tagId');
    try {
        await api('/tasks', {
            method: 'POST',
            body: JSON.stringify({
                title: data.get('title'),
                priority: data.get('priority'),
                tagId: tagRaw ? Number(tagRaw) : null,
                done: false,
            }),
        });
        el.taskForm.reset();
        el.taskForm.priority.value = 'MEDIUM';
        syncPickerButtons();
        await reload();
    } catch (err) {
        showError(err);
    }
});

el.editCancel.addEventListener('click', () => el.editDialog.close());

el.editForm.addEventListener('submit', async e => {
    e.preventDefault();
    const data = new FormData(el.editForm);
    const id = Number(data.get('id'));
    const existing = tasks.find(t => t.id === id);
    if (!existing) return;
    const tagRaw = data.get('tagId');
    try {
        await api(`/tasks/${id}`, {
            method: 'PUT',
            body: JSON.stringify({
                ...existing,
                title: data.get('title'),
                priority: data.get('priority'),
                tagId: tagRaw ? Number(tagRaw) : null,
            }),
        });
        el.editDialog.close();
        await reload();
    } catch (err) {
        showError(err);
    }
});

reload();
