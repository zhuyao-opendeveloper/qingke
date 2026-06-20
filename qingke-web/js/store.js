/**
 * 轻刻 - Data Store (localStorage backed)
 */
const Store = {
  _db: {},
  _loaded: false,

  _key(id) { return 'qingke_' + id; },

  load() {
    if (this._loaded) return;
    try {
      const raw = localStorage.getItem(this._key('db'));
      this._db = raw ? JSON.parse(raw) : {};
    } catch (e) {
      this._db = {};
    }
    this._ensureDefaults();
    this._loaded = true;
  },

  _save() {
    try {
      localStorage.setItem(this._key('db'), JSON.stringify(this._db));
    } catch (e) { /* quota exceeded - ignore */ }
  },

  _ensureDefaults() {
    if (!this._db.tasks) this._db.tasks = [];
    if (!this._db.projects) this._db.projects = [];
    if (!this._db.tags) this._db.tags = [];
    if (!this._db.habits) this._db.habits = [];
    if (!this._db.habitRecords) this._db.habitRecords = [];
    if (!this._db.countdowns) this._db.countdowns = [];
    if (!this._db.pomoSessions) this._db.pomoSessions = [];
    if (!this._db.checklistItems) this._db.checklistItems = [];
    if (!this._db.settings) this._db.settings = {};
    if (!this._db.chatMessages) this._db.chatMessages = [
      { role: 'bot', text: '你好！我是轻刻助手，可以帮你管理任务。告诉我你想做什么？', time: Date.now() }
    ];
    if (!this._db.templates) this._db.templates = [];
    if (!this._db.attachments) this._db.attachments = [];
    if (!this._db.rules) this._db.rules = [];
    if (!this._db.nextId) this._db.nextId = 1;

    // Ensure inbox project exists
    if (!this._db.projects.find(p => p.isInbox)) {
      this._db.projects.push({
        id: this._genId(), name: '收件箱', color: '#4A90E2',
        sortOrder: 0, isInbox: true, viewMode: 'list',
        createdTime: Date.now(), modifiedTime: Date.now()
      });
    }
    // Ensure default tags
    if (this._db.tags.length === 0) {
      ['工作', '个人', '学习', '健康'].forEach((name, i) => {
        this._db.tags.push({
          id: this._genId(), name, color: ['#4A90E2','#27AE60','#F39C12','#FF6B6B'][i],
          sortOrder: i, createdTime: Date.now()
        });
      });
    }
  },

  _genId() { return this._db.nextId++; },

  // ---- Tasks ----
  getTasks(filters = {}) {
    let tasks = [...this._db.tasks];
    if (filters.projectId !== undefined) tasks = tasks.filter(t => t.projectId === filters.projectId);
    if (filters.completed !== undefined) tasks = tasks.filter(t => t.isCompleted === filters.completed);
    if (filters.search) {
      const q = filters.search.toLowerCase();
      tasks = tasks.filter(t => t.title.toLowerCase().includes(q) || t.description.toLowerCase().includes(q));
    }
    if (filters.tag) tasks = tasks.filter(t => (t.tags || []).includes(filters.tag));
    return tasks.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  },

  getTask(id) { return this._db.tasks.find(t => t.id === id); },

  addTask(task) {
    const now = Date.now();
    const t = {
      id: this._genId(), title: '', description: '', priority: 0,
      isCompleted: false, completedTime: null,
      dueDate: null, startDate: null, allDay: false,
      projectId: null, parentTaskId: null,
      sortOrder: this._db.tasks.length,
      reminderTime: null, isImportant: false, isUrgent: false,
      estimatedPomos: 0, actualPomos: 0,
      tags: [], createdAt: now, modifiedAt: now,
      isDeleted: false, ...task
    };
    this._db.tasks.push(t);
    this._save();
    return t;
  },

  updateTask(id, updates) {
    const idx = this._db.tasks.findIndex(t => t.id === id);
    if (idx === -1) return null;
    this._db.tasks[idx] = { ...this._db.tasks[idx], ...updates, modifiedAt: Date.now() };
    this._save();
    return this._db.tasks[idx];
  },

  deleteTask(id) {
    this._db.tasks = this._db.tasks.filter(t => t.id !== id);
    this._save();
  },

  toggleTask(id) {
    const task = this.getTask(id);
    if (!task) return null;
    return this.updateTask(id, {
      isCompleted: !task.isCompleted,
      completedTime: !task.isCompleted ? Date.now() : null
    });
  },

  // ---- Projects ----
  getProjects() {
    return [...this._db.projects].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  },

  addProject(name, color) {
    const p = {
      id: this._genId(), name: name || '新项目',
      color: color || '#4A90E2', viewMode: 'list',
      sortOrder: this._db.projects.length, isInbox: false,
      createdTime: Date.now(), modifiedTime: Date.now()
    };
    this._db.projects.push(p);
    this._save();
    return p;
  },

  deleteProject(id) {
    this._db.projects = this._db.projects.filter(p => p.id !== id);
    // Remove project from tasks
    this._db.tasks.forEach(t => { if (t.projectId === id) t.projectId = null; });
    this._save();
  },

  getInboxProject() { return this._db.projects.find(p => p.isInbox); },

  // ---- Tags ----
  getTags() {
    return [...this._db.tags].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  },

  addTag(name, color) {
    const tag = { id: this._genId(), name, color: color || '#4A90E2', sortOrder: this._db.tags.length, createdTime: Date.now() };
    this._db.tags.push(tag);
    this._save();
    return tag;
  },

  deleteTag(id) {
    this._db.tags = this._db.tags.filter(t => t.id !== id);
    this._db.tasks.forEach(t => {
      if (t.tags) t.tags = t.tags.filter(tagId => tagId !== id);
    });
    this._save();
  },

  // ---- Habits ----
  getHabits() { return [...this._db.habits].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)); },

  addHabit(name, color, frequency) {
    const h = {
      id: this._genId(), name, color: color || '#27AE60',
      frequency: frequency || 'daily', targetCount: 1,
      frequencyDays: '1111111',
      currentStreak: 0, longestStreak: 0, totalCompletions: 0,
      sortOrder: this._db.habits.length,
      createdTime: Date.now(), modifiedTime: Date.now(), isArchived: false
    };
    this._db.habits.push(h);
    this._save();
    return h;
  },

  deleteHabit(id) {
    this._db.habits = this._db.habits.filter(h => h.id !== id);
    this._db.habitRecords = this._db.habitRecords.filter(r => r.habitId !== id);
    this._save();
  },

  habitCheckIn(habitId, date) {
    const dateStr = date || new Date().toISOString().split('T')[0];
    // Check if already checked in today
    const existing = this._db.habitRecords.find(r => r.habitId === habitId && r.date === dateStr);
    if (existing) return false;

    this._db.habitRecords.push({ habitId, date: dateStr, time: Date.now() });
    const habit = this._db.habits.find(h => h.id === habitId);
    if (habit) {
      habit.totalCompletions++;
      habit.currentStreak = this._calcStreak(habitId);
      if (habit.currentStreak > habit.longestStreak) habit.longestStreak = habit.currentStreak;
      habit.modifiedTime = Date.now();
    }
    this._save();
    return true;
  },

  getHabitCheckedToday(habitId) {
    const today = new Date().toISOString().split('T')[0];
    return this._db.habitRecords.some(r => r.habitId === habitId && r.date === today);
  },

  getHabitRecords(habitId) {
    return this._db.habitRecords.filter(r => r.habitId === habitId);
  },

  _calcStreak(habitId) {
    let streak = 0;
    const today = new Date();
    for (let i = 0; i < 365; i++) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];
      if (this._db.habitRecords.some(r => r.habitId === habitId && r.date === dateStr)) {
        streak++;
      } else if (i > 0) break;
    }
    return streak;
  },

  // ---- Countdowns ----
  getCountdowns() {
    return [...this._db.countdowns].sort((a, b) => {
      if (a.pinnedTime && !b.pinnedTime) return -1;
      if (!a.pinnedTime && b.pinnedTime) return 1;
      return a.targetDate - b.targetDate;
    });
  },

  addCountdown(name, targetDate, color) {
    const cd = {
      id: this._genId(), name, color: color || '#FFAE58',
      targetDate, type: 0, sortOrder: this._db.countdowns.length,
      remark: '', showRemark: true, isArchived: false,
      pinnedTime: null, createdTime: Date.now(), modifiedTime: Date.now()
    };
    this._db.countdowns.push(cd);
    this._save();
    return cd;
  },

  deleteCountdown(id) {
    this._db.countdowns = this._db.countdowns.filter(c => c.id !== id);
    this._save();
  },

  // ---- Pomodoro ----
  addPomoSession(session) {
    const s = {
      id: this._genId(), taskId: null, duration: 25 * 60 * 1000,
      startTime: Date.now(), endTime: Date.now(),
      isCompleted: true, wasInterrupted: false,
      sessionType: 'focus', date: new Date().toISOString().split('T')[0],
      ...session
    };
    this._db.pomoSessions.push(s);
    this._save();
    return s;
  },

  getTodayPomoSessions() {
    const today = new Date().toISOString().split('T')[0];
    return this._db.pomoSessions.filter(s => s.date === today);
  },

  getAllPomoSessions() {
    return [...this._db.pomoSessions];
  },

  // ---- Task Reordering ----
  reorderTask(taskId, newSortOrder) {
    const task = this.getTask(taskId);
    if (!task) return;
    task.sortOrder = newSortOrder;
    task.modifiedAt = Date.now();
    this._save();
  },

  swapSortOrder(id1, id2) {
    const t1 = this.getTask(id1);
    const t2 = this.getTask(id2);
    if (!t1 || !t2) return;
    const tmp = t1.sortOrder;
    t1.sortOrder = t2.sortOrder;
    t2.sortOrder = tmp;
    t1.modifiedAt = Date.now();
    t2.modifiedAt = Date.now();
    this._save();
  },

  // ---- Batch Operations ----
  batchComplete(ids) {
    const now = Date.now();
    ids.forEach(id => {
      const task = this.getTask(id);
      if (task && !task.isCompleted) {
        task.isCompleted = true;
        task.completedTime = now;
        task.modifiedAt = now;
      }
    });
    this._save();
  },

  batchDelete(ids) {
    this._db.tasks = this._db.tasks.filter(t => !ids.includes(t.id));
    this._save();
  },

  batchSetPriority(ids, priority) {
    const now = Date.now();
    ids.forEach(id => {
      const task = this.getTask(id);
      if (task) {
        task.priority = priority;
        task.modifiedAt = now;
      }
    });
    this._save();
  },

  batchSetProject(ids, projectId) {
    const now = Date.now();
    ids.forEach(id => {
      const task = this.getTask(id);
      if (task) {
        task.projectId = projectId;
        task.modifiedAt = now;
      }
    });
    this._save();
  },

  // ---- Templates ----
  getTemplates() {
    return [...this._db.templates].sort((a, b) => a.name.localeCompare(b.name));
  },

  addTemplate(name, taskData) {
    const t = {
      id: this._genId(), name, taskData: { ...taskData },
      createdTime: Date.now()
    };
    this._db.templates.push(t);
    this._save();
    return t;
  },

  deleteTemplate(id) {
    this._db.templates = this._db.templates.filter(t => t.id !== id);
    this._save();
  },

  // ---- Attachments ----
  getAttachments(taskId) {
    return this._db.attachments.filter(a => a.taskId === taskId);
  },

  addAttachment(taskId, file) {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const a = {
          id: this._genId(), taskId, name: file.name,
          type: file.type, size: file.size,
          data: e.target.result,
          createdTime: Date.now()
        };
        this._db.attachments.push(a);
        this._save();
        resolve(a);
      };
      reader.readAsDataURL(file);
    });
  },

  deleteAttachment(id) {
    this._db.attachments = this._db.attachments.filter(a => a.id !== id);
    this._save();
  },

  // ---- Automation Rules ----
  getRules() { return [...this._db.rules]; },

  addRule(rule) {
    const r = {
      id: this._genId(),
      name: rule.name || '新规则',
      enabled: true,
      condition: rule.condition || { type: 'priority', op: '>=', value: 5 },
      action: rule.action || { type: 'moveToProject', value: null },
      createdTime: Date.now()
    };
    this._db.rules.push(r);
    this._save();
    return r;
  },

  updateRule(id, updates) {
    const idx = this._db.rules.findIndex(r => r.id === id);
    if (idx === -1) return null;
    this._db.rules[idx] = { ...this._db.rules[idx], ...updates };
    this._save();
    return this._db.rules[idx];
  },

  deleteRule(id) {
    this._db.rules = this._db.rules.filter(r => r.id !== id);
    this._save();
  },

  applyRules(task) {
    const rules = this._db.rules.filter(r => r.enabled);
    const results = [];
    rules.forEach(rule => {
      let matched = false;
      const c = rule.condition;
      switch (c.type) {
        case 'priority':
          if (c.op === '>=' && (task.priority || 0) >= c.value) matched = true;
          if (c.op === '==' && (task.priority || 0) === c.value) matched = true;
          if (c.op === '<=' && (task.priority || 0) <= c.value) matched = true;
          break;
        case 'hasDueDate':
          if (c.op === 'yes' && task.dueDate) matched = true;
          if (c.op === 'no' && !task.dueDate) matched = true;
          break;
        case 'titleContains':
          if (task.title && task.title.toLowerCase().includes((c.value || '').toLowerCase())) matched = true;
          break;
      }
      if (matched) {
        const a = rule.action;
        switch (a.type) {
          case 'setPriority':
            this.updateTask(task.id, { priority: a.value });
            results.push(`已设置优先级为 ${getPriorityLabel(a.value)}`);
            break;
          case 'moveToProject':
            if (a.value) {
              this.updateTask(task.id, { projectId: a.value });
              const p = this.getProjects().find(x => x.id === a.value);
              results.push(`已移动到 "${p ? p.name : '项目'}"`);
            }
            break;
          case 'setImportant':
            this.updateTask(task.id, { isImportant: true });
            results.push('已标记为重要');
            break;
          case 'addTag':
            if (a.value && !(task.tags || []).includes(a.value)) {
              this.updateTask(task.id, { tags: [...(task.tags || []), a.value] });
              const tag = this.getTags().find(x => x.id === a.value);
              results.push(`已添加标签 "${tag ? tag.name : ''}"`);
            }
            break;
        }
      }
    });
    return results;
  },

  // ---- Export CSV ----
  exportCSV() {
    const headers = ['ID','标题','描述','优先级','已完成','截止日期','项目','标签','创建时间','完成时间'];
    const rows = this._db.tasks.filter(t => !t.isDeleted).map(t => {
      const p = this._db.projects.find(x => x.id === t.projectId);
      const tags = (t.tags || []).map(tagId => {
        const tag = this._db.tags.find(x => x.id === tagId);
        return tag ? tag.name : '';
      }).join(';');
      return [
        t.id, t.title, (t.description || '').replace(/"/g, '""'),
        t.priority || 0, t.isCompleted ? '是' : '否',
        t.dueDate ? new Date(t.dueDate).toISOString().split('T')[0] : '',
        p ? p.name : '', tags,
        t.createdAt ? new Date(t.createdAt).toISOString().split('T')[0] : '',
        t.completedTime ? new Date(t.completedTime).toISOString().split('T')[0] : ''
      ].map(v => `"${v}"`).join(',');
    });
    return '\uFEFF' + headers.join(',') + '\n' + rows.join('\n');
  },

  // ---- Import from TickTick ----
  importFromTickTick(json) {
    try {
      const data = typeof json === 'string' ? JSON.parse(json) : json;
      let count = 0;
      // TickTick export format
      const tasks = data.tasks || data.tasks || [];
      if (tasks.length > 0) {
        tasks.forEach(item => {
          this.addTask({
            title: item.title || item.name || '(无标题)',
            description: item.content || item.desc || '',
            priority: item.priority || 0,
            dueDate: item.dueDate ? new Date(item.dueDate).getTime() : null,
            isCompleted: item.status === 2 || item.completed || false,
            tags: item.tags ? item.tags.map(t => {
              const existing = this._db.tags.find(x => x.name === t);
              if (existing) return existing.id;
              const tag = this.addTag(t);
              return tag.id;
            }) : []
          });
          count++;
        });
        this._save();
      }
      return count;
    } catch (e) { return 0; }
  },

  // ---- Import from Todoist ----
  importFromTodoist(json) {
    try {
      const data = typeof json === 'string' ? JSON.parse(json) : json;
      let count = 0;
      // Todoist backup format
      const items = data.items || data.tasks || [];
      if (items.length > 0) {
        const projects = data.projects || [];
        items.forEach(item => {
          let projectId = null;
          if (item.project_id) {
            const proj = projects.find(p => p.id === item.project_id);
            if (proj) {
              const existing = this._db.projects.find(x => x.name === proj.name);
              if (existing) {
                projectId = existing.id;
              } else {
                const p = this.addProject(proj.name);
                projectId = p.id;
              }
            }
          }
          this.addTask({
            title: item.content || item.title || '(无标题)',
            description: item.description || '',
            priority: 5 - (item.priority || 1), // Todoist: 1-4, ours: 0-7
            dueDate: item.due ? new Date(item.due.date).getTime() : null,
            isCompleted: item.checked === 1 || item.completed || false,
            projectId
          });
          count++;
        });
        this._save();
      }
      return count;
    } catch (e) { return 0; }
  },

  // ---- Settings ----
  getSettings() { return this._db.settings; },

  updateSetting(key, value) {
    this._db.settings[key] = value;
    this._save();
  },

  getSetting(key, def) {
    return this._db.settings[key] !== undefined ? this._db.settings[key] : def;
  },

  // ---- Chat ----
  getChatMessages() { return this._db.chatMessages; },

  addChatMessage(msg) {
    this._db.chatMessages.push({ ...msg, time: Date.now() });
    this._save();
  },

  clearChat() {
    this._db.chatMessages = [
      { role: 'bot', text: '你好！我是轻刻助手，可以帮你管理任务。告诉我你想做什么？', time: Date.now() }
    ];
    this._save();
  },

  // ---- Stats ----
  getStats() {
    const allTasks = this._db.tasks.filter(t => !t.isDeleted);
    const completed = allTasks.filter(t => t.isCompleted);
    const total = allTasks.length;
    const completionRate = total > 0 ? Math.round(completed.length / total * 100) : 0;
    const overdue = allTasks.filter(t => !t.isCompleted && t.dueDate && t.dueDate < Date.now());

    // This week stats
    const now = new Date();
    const startOfWeek = new Date(now);
    startOfWeek.setDate(now.getDate() - now.getDay());
    startOfWeek.setHours(0, 0, 0, 0);
    const weekCompleted = completed.filter(t => t.completedTime && t.completedTime >= startOfWeek.getTime());

    // Pomodoro stats
    const pomoSessions = this._db.pomoSessions;
    const todayFocus = pomoSessions.filter(s => {
      const sd = new Date(s.startTime).toISOString().split('T')[0];
      return sd === now.toISOString().split('T')[0] && s.sessionType === 'focus';
    });
    const todayFocusTime = todayFocus.reduce((sum, s) => sum + s.duration, 0);

    return { total, completed: completed.length, completionRate, overdue: overdue.length,
      weekCompleted: weekCompleted.length, todayFocus: todayFocus.length, todayFocusTime };
  },

  // ---- Export/Import ----
  exportData() {
    return JSON.stringify(this._db);
  },

  importData(json) {
    try {
      const data = JSON.parse(json);
      if (data.tasks && data.projects) {
        this._db = data;
        this._ensureDefaults();
        this._save();
        return true;
      }
      return false;
    } catch (e) { return false; }
  },

  clearAll() {
    this._db = {};
    this._ensureDefaults();
    this._save();
  }
};

// Initialize
Store.load();
