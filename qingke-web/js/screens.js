/**
 * 轻刻 - All Screen Renderers
 */

// ============================================
// Utility functions
// ============================================
function formatDate(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.getFullYear() + '年' + (d.getMonth()+1) + '月' + d.getDate() + '日';
}

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0');
}

function formatDateTime(ts) {
  if (!ts) return '';
  return formatDate(ts) + ' ' + formatTime(ts);
}

function formatDueDate(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  const today = new Date();
  const tomorrow = new Date(today); tomorrow.setDate(tomorrow.getDate()+1);
  if (d.toDateString() === today.toDateString()) return '今天';
  if (d.toDateString() === tomorrow.toDateString()) return '明天';
  return (d.getMonth()+1) + '/' + d.getDate();
}

function isToday(ts) {
  return new Date(ts).toDateString() === new Date().toDateString();
}

function isOverdue(ts) {
  return ts && new Date(ts) < new Date(new Date().toDateString());
}

function getPriorityText(p) {
  return ['无','低','中','高','紧急'][[0,1,3,5,7].indexOf(p) > -1 ? [0,1,3,5,7].indexOf(p) : 0];
}

function getPriorityLabel(p) {
  if (p >= 7) return '紧急';
  if (p >= 5) return '高';
  if (p >= 3) return '中';
  if (p >= 1) return '低';
  return '无';
}

function getPriorityClass(p) {
  if (p >= 7) return 'priority-urgent';
  if (p >= 5) return 'priority-high';
  if (p >= 3) return 'priority-medium';
  if (p >= 1) return 'priority-low';
  return '';
}

function getTaskMetaHtml(task) {
  let html = '';
  if (task.dueDate) {
    const cls = isOverdue(task.dueDate) && !task.isCompleted ? ' style="color:var(--overdue)"' : '';
    html += '<span class="task-meta-item"' + cls + '>' + Icons.clock + formatDueDate(task.dueDate) + '</span>';
  }
  if (task.projectId) {
    const p = Store.getProjects().find(x => x.id === task.projectId);
    if (p) html += '<span class="task-meta-item">' + Icons.folder + p.name + '</span>';
  }
  return html;
}

function getPriorityColors() {
  return [
    { value: 0, label: '无', cls: '', color: '#CCCCCC' },
    { value: 1, label: '低', cls: 'p-low', color: '#A0E7E5' },
    { value: 3, label: '中', cls: 'p-medium', color: '#FFB84D' },
    { value: 5, label: '高', cls: 'p-high', color: '#FF6B6B' },
    { value: 7, label: '紧急', cls: 'p-critical', color: '#FF6B6B' }
  ];
}

// ============================================
// Batch Mode, Drag & Interaction State
// ============================================
let _batchMode = false;
let _batchSelected = new Set();
let _dragSourceId = null;
let _swipeStartX = 0;
let _swipeTaskId = null;

// ============================================
// Screen: Home (Inbox/Today/Next7/Someday)
// ============================================
function renderHome() {
  const tabs = [
    { id: 'inbox', label: '收件箱' },
    { id: 'today', label: '今天' },
    { id: 'next7', label: '未来7天' },
    { id: 'someday', label: '某天' }
  ];
  let html = '<div class="tab-bar" id="home-tabs">';
  tabs.forEach((t, i) => {
    html += '<div class="tab' + (i === 0 ? ' active' : '') + '" data-tab="' + t.id + '" onclick="switchHomeTab(\'' + t.id + '\')">' + t.label + '</div>';
  });
  html += '</div>';

  tabs.forEach(t => {
    html += '<div class="tab-content' + (t.id === 'inbox' ? ' active' : '') + '" id="home-' + t.id + '">';
    html += renderTaskListForTab(t.id);
    html += '</div>';
  });

  return html;
}

function switchHomeTab(tabId) {
  document.querySelectorAll('#home-tabs .tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('[id^="home-"]').forEach(c => c.classList.remove('active'));
  document.querySelector('#home-tabs .tab[data-tab="' + tabId + '"]').classList.add('active');
  document.getElementById('home-' + tabId).classList.add('active');
  window._homeTab = tabId;
}

function renderTaskListForTab(tabId) {
  let tasks = Store.getTasks({ completed: false });
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const day7 = new Date(todayStart); day7.setDate(day7.getDate() + 7);

  switch (tabId) {
    case 'inbox':
      // All uncompleted tasks sorted by priority and date
      tasks = tasks.sort((a, b) => (b.priority || 0) - (a.priority || 0));
      break;
    case 'today':
      tasks = tasks.filter(t => t.dueDate && isToday(t.dueDate));
      break;
    case 'next7':
      tasks = tasks.filter(t => {
        if (!t.dueDate) return false;
        const d = new Date(t.dueDate);
        return d >= todayStart && d < day7;
      });
      break;
    case 'someday':
      tasks = tasks.filter(t => !t.dueDate);
      break;
  }

  if (tasks.length === 0) {
    let emptyHtml = '<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/><path d="M12 6v6l4 2"/></svg><p>暂无任务</p></div>';
    if (_batchMode) emptyHtml += '<div style="text-align:center;padding:12px"><button class="btn" onclick="exitBatchMode()">退出选择</button></div>';
    return emptyHtml;
  }

  let html = '<div class="task-list">';
  if (_batchMode) {
    html += '<div class="batch-bar show">';
    html += '<span class="batch-count" id="batch-count">' + _batchSelected.size + ' 已选</span>';
    html += '<button class="batch-btn batch-complete" onclick="executeBatchComplete()">完成</button>';
    html += '<button class="batch-btn batch-delete" onclick="executeBatchDelete()">删除</button>';
    html += '<button class="batch-btn" onclick="exitBatchMode()">取消</button>';
    html += '</div>';
  } else {
    html += '<div style="display:flex;justify-content:space-between;align-items:center;padding:0 4px 8px">';
    html += '<div class="task-count">共 ' + tasks.length + ' 个任务</div>';
    html += '<button class="text-btn" onclick="enterBatchMode()" style="font-size:0.813rem;color:var(--primary);background:none;border:none;cursor:pointer;padding:4px 8px">选择</button>';
    html += '</div>';
  }
  tasks.forEach(task => {
    html += renderTaskItem(task);
  });
  html += '</div>';
  return html;
}

function renderTaskItem(task) {
  const prio = task.priority || 0;
  const dotHtml = prio > 0 ? '<div class="priority-dot ' + getPriorityClass(prio) + '"></div>' : '';
  const titleCls = task.isCompleted ? 'task-title completed' : 'task-title';
  const checkCls = task.isCompleted ? 'task-check checked' : 'task-check';
  const metaHtml = getTaskMetaHtml(task);

  // Drag handle (for drag-and-drop reorder)
  const dragHtml = '<div class="drag-handle" draggable="true" data-task-id="' + task.id + '" ondragstart="onDragStart(event)" ondragend="onDragEnd(event)"><span></span><span></span><span></span></div>';

  // Batch selection checkbox
  let batchHtml = '';
  if (_batchMode) {
    const checked = _batchSelected.has(task.id) ? ' batch-checked' : '';
    batchHtml = '<div class="task-check batch-check' + checked + '" onclick="event.stopPropagation();toggleBatchSelect(' + task.id + ')"></div>';
  }

  return '<div class="task-item" data-task-id="' + task.id + '" ondragover="onDragOver(event)" ondrop="onDrop(event)" ontouchstart="onSwipeStart(event)" ontouchmove="onSwipeMove(event)" ontouchend="onSwipeEnd(event)" onclick="openTaskDetail(' + task.id + ')">' +
    dragHtml +
    batchHtml +
    '<div class="' + checkCls + '" onclick="event.stopPropagation();toggleTask(' + task.id + ')"></div>' +
    '<div class="task-content">' +
      '<div class="' + titleCls + '">' + escHtml(task.title) + '</div>' +
      (metaHtml ? '<div class="task-meta">' + metaHtml + '</div>' : '') +
    '</div>' +
    dotHtml +
  '</div>';
}

function escHtml(str) {
  if (!str) return '';
  const d = document.createElement('div');
  d.textContent = str;
  return d.innerHTML;
}

// ============================================
// Screen: Calendar
// ============================================
let _calView = 'month';
let _calDate = new Date().getTime();

function renderCalendar() {
  return renderCalendarContent();
}

function renderCalendarContent() {
  if (_calView === 'year') return renderYearView();
  return renderMonthView();
}

function renderMonthView() {
  const d = new Date(_calDate);
  const year = d.getFullYear(), month = d.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDay = new Date(year, month, 1).getDay();

  let html = '<div class="cal-header">';
  html += '<button class="cal-nav-btn" onclick="calNav(-1)">' + Icons.chevronLeft + '</button>';
  html += '<h2>' + year + '年' + (month+1) + '月</h2>';
  html += '<button class="cal-nav-btn" onclick="calNav(1)">' + Icons.chevronRight + '</button>';
  html += '</div>';

  html += '<div class="cal-weekdays">';
  ['日','一','二','三','四','五','六'].forEach(w => { html += '<span>' + w + '</span>'; });
  html += '</div>';

  html += '<div class="cal-grid">';
  for (let i = 0; i < firstDay; i++) {
    html += '<div class="cal-day empty"></div>';
  }
  const today = new Date();
  const selectedDate = new Date(_calDate);
  for (let day = 1; day <= daysInMonth; day++) {
    const isToday = day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
    const isSelected = day === selectedDate.getDate() && month === selectedDate.getMonth() && year === selectedDate.getFullYear();
    const dateTs = new Date(year, month, day, 12, 0, 0).getTime();
    const hasTasks = Store.getTasks({ completed: false }).some(t => {
      if (!t.dueDate) return false;
      const td = new Date(t.dueDate);
      return td.getDate() === day && td.getMonth() === month && td.getFullYear() === year;
    });
    html += '<div class="cal-day' + (isToday ? ' today' : '') + (isSelected ? ' selected' : '') + '" onclick="calSelectDay(' + dateTs + ')">';
    html += '<span>' + day + '</span>';
    if (hasTasks) html += '<div class="cal-day-dot"></div>';
    html += '</div>';
  }
  html += '</div>';

  // Tasks for selected day
  html += '<div class="cal-day-tasks">';
  const dayStart = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), selectedDate.getDate()).getTime();
  const dayEnd = dayStart + 86400000;
  const dayTasks = Store.getTasks({ completed: false }).filter(t => {
    return t.dueDate && t.dueDate >= dayStart && t.dueDate < dayEnd;
  });
  if (dayTasks.length > 0) {
    html += '<h3>' + formatDate(_calDate) + ' 的任务</h3>';
    dayTasks.forEach(t => {
      html += '<div class="cal-day-task">';
      html += '<div class="task-check' + (t.isCompleted ? ' checked' : '') + '" onclick="event.stopPropagation();toggleTask(' + t.id + ')"></div>';
      html += '<span style="flex:1;' + (t.isCompleted ? 'text-decoration:line-through;color:var(--on-surface-variant)' : '') + '">' + escHtml(t.title) + '</span>';
      html += '</div>';
    });
  } else if (dayTasks.length === 0) {
    html += '<h3>' + formatDate(_calDate) + '</h3>';
    html += '<div class="empty-state" style="padding:20px"><p>暂无任务</p></div>';
  }
  html += '</div>';

  return html;
}

function renderYearView() {
  const d = new Date(_calDate);
  const year = d.getFullYear();

  let html = '<div class="cal-header">';
  html += '<button class="cal-nav-btn" onclick="calNav(-12)">' + Icons.chevronLeft + '</button>';
  html += '<h2>' + year + '年</h2>';
  html += '<button class="cal-nav-btn" onclick="calNav(12)">' + Icons.chevronRight + '</button>';
  html += '</div>';

  html += '<div class="year-grid">';
  for (let m = 0; m < 12; m++) {
    const daysInMonth = new Date(year, m + 1, 0).getDate();
    const firstDay = new Date(year, m, 1).getDay();
    html += '<div class="year-month-card" onclick="calGoToMonth(' + m + ')">';
    html += '<h4>' + ['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月'][m] + '</h4>';
    html += '<div class="year-month-grid">';
    ['日','一','二','三','四','五','六'].forEach(w => { html += '<span>' + w + '</span>'; });
    for (let i = 0; i < firstDay; i++) html += '<span></span>';
    for (let day = 1; day <= daysInMonth; day++) {
      const has = Store.getTasks({ completed: false }).some(t => {
        if (!t.dueDate) return false;
        const td = new Date(t.dueDate);
        return td.getDate() === day && td.getMonth() === m && td.getFullYear() === year;
      });
      html += '<span class="year-month-day' + (has ? ' has-task' : '') + '">' + day + '</span>';
    }
    html += '</div></div>';
  }
  html += '</div>';
  return html;
}

function calNav(delta) {
  const d = new Date(_calDate);
  if (_calView === 'month') {
    d.setMonth(d.getMonth() + delta);
  } else {
    d.setFullYear(d.getFullYear() + (delta > 0 ? 1 : -1));
  }
  _calDate = d.getTime();
  renderCurrentScreen();
}

function calSelectDay(ts) {
  _calDate = ts;
  renderCurrentScreen();
}

function calGoToMonth(m) {
  const d = new Date(_calDate);
  d.setMonth(m);
  d.setDate(1);
  _calDate = d.getTime();
  _calView = 'month';
  renderCurrentScreen();
}

function toggleCalView() {
  _calView = _calView === 'month' ? 'year' : 'month';
  renderCurrentScreen();
}

// ============================================
// Screen: Focus (Pomodoro)
// ============================================
const PomoState = {
  sessionType: 'focus', // focus, shortBreak, longBreak
  remainingTime: 25 * 60 * 1000,
  isRunning: false,
  isPaused: false,
  timerId: null,
  startTime: null
};

const POMO_DURATIONS = {
  focus: 25 * 60 * 1000,
  shortBreak: 5 * 60 * 1000,
  longBreak: 15 * 60 * 1000
};

function renderFocus() {
  const s = PomoState;
  const mins = Math.floor(s.remainingTime / 60000);
  const secs = Math.floor((s.remainingTime % 60000) / 1000);
  const timeStr = String(mins).padStart(2,'0') + ':' + String(secs).padStart(2,'0');

  const typeColors = { focus: 'red', shortBreak: 'teal', longBreak: 'purple' };
  const color = typeColors[s.sessionType];
  const chipActive = s.sessionType;

  const todaySessions = Store.getTodayPomoSessions().filter(s => s.sessionType === 'focus');
  const todayCount = todaySessions.length;
  const todayTime = todaySessions.reduce((sum, s) => sum + s.duration, 0);
  const totalSessions = Store.getAllPomoSessions().filter(s => s.sessionType === 'focus').length;

  let html = '<div class="focus-screen">';

  // Session type chips
  html += '<div class="focus-chips">';
  [['focus','专注','red'], ['shortBreak','短休','teal'], ['longBreak','长休','purple']].forEach(([type, label, clr]) => {
    const activeCls = s.sessionType === type ? ' active active-' + clr : '';
    html += '<div class="focus-chip' + activeCls + '" onclick="setPomoType(\'' + type + '\')">' + label + '</div>';
  });
  html += '</div>';

  // Tomato pixel art
  html += '<div class="focus-tomato">';
  html += '<svg width="120" height="120" viewBox="0 0 120 120" fill="none">';
  html += '<circle cx="60" cy="60" r="50" fill="' + (s.sessionType === 'focus' ? '#ff6b6b' : s.sessionType === 'shortBreak' ? '#4ecdc4' : '#a29bfe') + '"/>';
  html += '<ellipse cx="60" cy="45" rx="8" ry="6" fill="rgba(0,0,0,0.1)"/>';
  html += '<path d="M40 35 Q45 28 55 30" stroke="rgba(0,0,0,0.15)" stroke-width="2" fill="none"/>';
  html += '<path d="M80 35 Q75 28 65 30" stroke="rgba(0,0,0,0.15)" stroke-width="2" fill="none"/>';
  if (s.isRunning) {
    html += '<rect x="50" y="52" width="8" height="18" rx="2" fill="rgba(255,255,255,0.3)"/>';
    html += '<rect x="62" y="52" width="8" height="18" rx="2" fill="rgba(255,255,255,0.3)"/>';
  }
  html += '</svg>';
  html += '</div>';

  // Timer display
  html += '<div class="focus-timer" style="color:' + (s.sessionType === 'focus' ? '#ff6b6b' : s.sessionType === 'shortBreak' ? '#4ecdc4' : '#a29bfe') + '">' + timeStr + '</div>';

  // Buttons
  html += '<div class="focus-actions">';
  if (s.isRunning || s.isPaused) {
    html += '<button class="focus-btn focus-btn-' + color + '" onclick="pomoToggle()">' + (s.isPaused ? '继续' : '暂停') + '</button>';
  } else {
    html += '<button class="focus-btn focus-btn-' + color + '" onclick="pomoStart()">开始</button>';
  }
  html += '<button class="focus-btn focus-btn-gray" onclick="pomoReset()">重置</button>';
  html += '</div>';

  // Stats
  html += '<div class="focus-stats">';
  html += '<p>今日专注: ' + todayCount + ' 次</p>';
  html += '<p class="label">今日时长: ' + formatDuration(todayTime) + '</p>';
  html += '<p class="label">总计专注: ' + totalSessions + ' 次</p>';
  html += '</div>';

  html += '</div>';
  return html;
}

function setPomoType(type) {
  if (PomoState.isRunning || PomoState.isPaused) return;
  PomoState.sessionType = type;
  PomoState.remainingTime = POMO_DURATIONS[type];
  renderCurrentScreen();
}

function pomoStart() {
  if (PomoState.isRunning) return;
  PomoState.isRunning = true;
  PomoState.isPaused = false;
  PomoState.startTime = Date.now();
  PomoState.timerId = setInterval(pomoTick, 1000);
  updatePomoDisplay();
}

function pomoToggle() {
  if (PomoState.isPaused) {
    PomoState.isPaused = false;
    PomoState.timerId = setInterval(pomoTick, 1000);
  } else {
    PomoState.isPaused = true;
    clearInterval(PomoState.timerId);
    PomoState.timerId = null;
  }
  updatePomoDisplay();
}

function pomoReset() {
  clearInterval(PomoState.timerId);
  PomoState.isRunning = false;
  PomoState.isPaused = false;
  PomoState.timerId = null;
  PomoState.remainingTime = POMO_DURATIONS[PomoState.sessionType];
  renderCurrentScreen();
}

function pomoTick() {
  PomoState.remainingTime -= 1000;
  if (PomoState.remainingTime <= 0) {
    PomoState.remainingTime = 0;
    clearInterval(PomoState.timerId);
    PomoState.isRunning = false;
    PomoState.timerId = null;

    // Record session
    Store.addPomoSession({
      duration: POMO_DURATIONS[PomoState.sessionType],
      startTime: PomoState.startTime,
      endTime: Date.now(),
      sessionType: PomoState.sessionType,
      isCompleted: true
    });

    showToast('专注完成！🎉');
  }
  updatePomoDisplay();
}

function updatePomoDisplay() {
  if (document.getElementById('screen-focus') && document.getElementById('screen-focus').classList.contains('active')) {
    renderCurrentScreen();
  }
}

function formatDuration(ms) {
  const hours = Math.floor(ms / 3600000);
  const mins = Math.floor((ms % 3600000) / 60000);
  return hours > 0 ? hours + 'h ' + mins + 'm' : mins + 'm';
}

// ============================================
// Screen: Chat
// ============================================
let _chatInput = '';

function renderChat() {
  const messages = Store.getChatMessages();
  let html = '<div class="chat-container">';
  html += '<div class="chat-messages" id="chat-msgs">';
  messages.forEach(msg => {
    html += '<div class="chat-msg ' + msg.role + '">';
    html += '<div class="chat-bubble">' + escHtml(msg.text) + '</div>';
    html += '</div>';
  });
  html += '</div>';
  html += '<div class="chat-input-area">';
  html += '<input class="chat-input" id="chat-input" placeholder="输入消息..." value="' + escHtml(_chatInput) + '" onkeydown="if(event.key===\'Enter\')sendChat()"/>';
  html += '<button class="chat-send-btn" onclick="sendChat()">' + Icons.send + '</button>';
  html += '</div>';
  html += '</div>';
  return html;
}

function sendChat() {
  const input = document.getElementById('chat-input');
  if (!input || !input.value.trim()) return;
  const text = input.value.trim();
  _chatInput = '';
  input.value = '';

  Store.addChatMessage({ role: 'user', text });
  renderCurrentScreen();
  scrollChat();

  // Simple auto-reply
  setTimeout(() => {
    const reply = generateChatReply(text);
    Store.addChatMessage({ role: 'bot', text: reply });
    renderCurrentScreen();
    scrollChat();
  }, 500);
}

function scrollChat() {
  const el = document.getElementById('chat-msgs');
  if (el) el.scrollTop = el.scrollHeight;
}

function generateChatReply(text) {
  const lower = text.toLowerCase();
  if (lower.includes('任务') || lower.includes('添加') || lower.includes('创建')) {
    const taskMatch = text.match(/任务[：:]\s*(.+)/) || text.match(/添加\s*(.+)/);
    if (taskMatch) {
      const title = taskMatch[1];
      Store.addTask({ title: title.trim(), createdAt: Date.now() });
      return '已添加任务: "' + title.trim() + '"';
    }
    return '请告诉我任务名称，例如"添加任务: 买 groceries"';
  }
  if (lower.includes('今天') || lower.includes('今日')) {
    const tasks = Store.getTasks({ completed: false }).filter(t => t.dueDate && isToday(t.dueDate));
    return tasks.length > 0
      ? '今天有 ' + tasks.length + ' 个待办任务：' + tasks.map(t => t.title).join('、')
      : '今天没有待办任务，可以放松一下！';
  }
  if (lower.includes('统计') || lower.includes('进度')) {
    const stats = Store.getStats();
    return '当前进度：总计 ' + stats.total + ' 个任务，已完成 ' + stats.completed + ' 个，完成率 ' + stats.completionRate + '%' +
      (stats.overdue > 0 ? '，逾期 ' + stats.overdue + ' 个' : '');
  }
  if (lower.includes('专注') || lower.includes('番茄')) {
    const todaySessions = Store.getTodayPomoSessions().filter(s => s.sessionType === 'focus');
    return '今天已完成 ' + todaySessions.length + ' 个番茄钟' +
      (todaySessions.length > 0 ? '，总时长 ' + formatDuration(todaySessions.reduce((s, x) => s + x.duration, 0)) : '');
  }
  if (lower.includes('习惯') || lower.includes('打卡')) {
    const habits = Store.getHabits();
    const checked = habits.filter(h => Store.getHabitCheckedToday(h.id));
    return '今天已打卡 ' + checked.length + '/' + habits.length + ' 个习惯' +
      (checked.length > 0 ? '：' + checked.map(h => h.name).join('、') : '');
  }
  return '收到你的消息！我可以帮你：\n- 添加/查询任务\n- 查看今日待办\n- 统计完成进度\n- 查询专注和习惯数据\n请告诉我你需要什么帮助？';
}

// ============================================
// Screen: Habits
// ============================================
function renderHabits() {
  const habits = Store.getHabits();
  if (habits.length === 0) {
    return '<div class="empty-state">' + Icons.star + '<p>暂无习惯，点击 + 添加</p></div>';
  }

  let html = '<div style="padding:8px 0">';
  habits.forEach(h => {
    const checked = Store.getHabitCheckedToday(h.id);
    const records = Store.getHabitRecords(h.id);
    // Get this week's check-in status for mini display
    const weekHtml = renderWeekStatus(h.id, records);
    html += '<div class="habit-card">';
    html += '<div class="habit-color" style="background:' + h.color + '"></div>';
    html += '<div class="habit-info">';
    html += '<div class="habit-name">' + escHtml(h.name) + '</div>';
    html += '<div class="habit-stats">连续 ' + h.currentStreak + ' 天 | 总计 ' + h.totalCompletions + ' 次</div>';
    html += weekHtml;
    html += '</div>';
    html += '<button class="habit-checkin' + (checked ? ' done' : '') + '" onclick="habitCheckIn(' + h.id + ')">' + (checked ? '✓' : '打卡') + '</button>';
    html += '</div>';
  });
  html += '</div>';
  return html;
}

function renderWeekStatus(habitId, records) {
  let html = '<div style="display:flex;gap:4px;margin-top:6px">';
  const today = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().split('T')[0];
    const done = records.some(r => r.date === dateStr);
    html += '<div style="width:12px;height:12px;border-radius:50%;background:' + (done ? '#27AE60' : 'var(--divider)') + '"></div>';
  }
  html += '</div>';
  return html;
}

function habitCheckIn(id) {
  if (Store.habitCheckIn(id)) {
    showToast('打卡成功！');
  } else {
    showToast('今天已打卡');
  }
  renderCurrentScreen();
}

// ============================================
// Screen: Countdowns
// ============================================
function renderCountdowns() {
  const countdowns = Store.getCountdowns().filter(c => !c.isArchived);
  if (countdowns.length === 0) {
    return '<div class="empty-state">' + Icons.clock + '<p>暂无倒计时，点击 + 添加</p></div>';
  }

  let html = '<div style="padding:8px 0">';
  countdowns.forEach(cd => {
    const diff = cd.targetDate - Date.now();
    const isUp = diff <= 0;
    const days = isUp ? Math.floor(Math.abs(diff) / 86400000) : Math.floor(diff / 86400000);
    const hours = Math.floor((Math.abs(diff) % 86400000) / 3600000);

    html += '<div class="countdown-card">';
    html += '<div class="cd-header">';
    html += '<div style="width:12px;height:12px;border-radius:50%;background:' + cd.color + '"></div>';
    html += '<span class="cd-name">' + escHtml(cd.name) + '</span>';
    html += '</div>';
    html += '<div class="cd-time" style="color:' + cd.color + '">';
    if (isUp) {
      html += '已过去 ' + days + ' 天';
    } else {
      html += days + ' 天' + (hours > 0 ? ' ' + hours + ' 小时' : '');
    }
    html += '</div>';
    if (cd.remark && cd.showRemark) {
      html += '<div class="cd-note">' + escHtml(cd.remark) + '</div>';
    }
    html += '</div>';
  });
  html += '</div>';
  return html;
}

// ============================================
// Screen: Projects
// ============================================
function renderProjects() {
  const projects = Store.getProjects();
  let html = '';
  projects.forEach(p => {
    const count = Store.getTasks().filter(t => t.projectId === p.id && !t.isCompleted).length;
    html += '<div class="project-item" onclick="showProjectTasks(' + p.id + ')">';
    html += '<div class="project-color" style="background:' + p.color + '"></div>';
    html += '<span class="project-name">' + escHtml(p.name) + '</span>';
    html += '<span class="project-count">' + count + '</span>';
    html += '</div>';
  });
  return html;
}

function showProjectTasks(projectId) {
  const project = Store.getProjects().find(p => p.id === projectId);
  if (!project) return;
  const tasks = Store.getTasks({ projectId, completed: false });
  let html = '<div class="section-title" style="padding:12px 16px">' + escHtml(project.name) + '</div>';
  if (tasks.length === 0) {
    html += '<div class="empty-state"><p>暂无任务</p></div>';
  } else {
    html += '<div class="task-list">';
    tasks.forEach(t => { html += renderTaskItem(t); });
    html += '</div>';
  }
  showDialog(html);
}

// ============================================
// Screen: Tags
// ============================================
function renderTags() {
  const tags = Store.getTags();
  let html = '';
  tags.forEach(tag => {
    const count = Store.getTasks().filter(t => (t.tags || []).includes(tag.id)).length;
    html += '<div class="tag-item" onclick="showTagTasks(' + tag.id + ')">';
    html += '<div style="width:12px;height:12px;border-radius:50%;background:' + tag.color + '"></div>';
    html += '<span class="project-name">' + escHtml(tag.name) + '</span>';
    html += '<span class="project-count">' + count + '</span>';
    html += '</div>';
  });
  return html;
}

function showTagTasks(tagId) {
  const tag = Store.getTags().find(t => t.id === tagId);
  if (!tag) return;
  const tasks = Store.getTasks({ completed: false }).filter(t => (t.tags || []).includes(tagId));
  let html = '<div class="section-title" style="padding:12px 16px">' + escHtml(tag.name) + '</div>';
  if (tasks.length === 0) {
    html += '<div class="empty-state"><p>暂无任务</p></div>';
  } else {
    html += '<div class="task-list">';
    tasks.forEach(t => { html += renderTaskItem(t); });
    html += '</div>';
  }
  showDialog(html);
}

// ============================================
// Screen: Matrix (Eisenhower)
// ============================================
function renderMatrix() {
  const tasks = Store.getTasks({ completed: false });
  const q1 = tasks.filter(t => t.isUrgent && t.isImportant);
  const q2 = tasks.filter(t => !t.isUrgent && t.isImportant);
  const q3 = tasks.filter(t => t.isUrgent && !t.isImportant);
  const q4 = tasks.filter(t => !t.isUrgent && !t.isImportant);

  let html = '<div class="matrix-grid">';
  html += '<div class="matrix-quad q1"><h4>重要且紧急</h4>';
  q1.forEach(t => { html += '<div class="matrix-task" onclick="openTaskDetail(' + t.id + ')">· ' + escHtml(t.title) + '</div>'; });
  html += '</div>';
  html += '<div class="matrix-quad q2"><h4>重要不紧急</h4>';
  q2.forEach(t => { html += '<div class="matrix-task" onclick="openTaskDetail(' + t.id + ')">· ' + escHtml(t.title) + '</div>'; });
  html += '</div>';
  html += '<div class="matrix-quad q3"><h4>紧急不重要</h4>';
  q3.forEach(t => { html += '<div class="matrix-task" onclick="openTaskDetail(' + t.id + ')">· ' + escHtml(t.title) + '</div>'; });
  html += '</div>';
  html += '<div class="matrix-quad q4"><h4>不紧急不重要</h4>';
  q4.forEach(t => { html += '<div class="matrix-task" onclick="openTaskDetail(' + t.id + ')">· ' + escHtml(t.title) + '</div>'; });
  html += '</div>';
  html += '</div>';
  return html;
}

// ============================================
// Screen: Timeline
// ============================================
function renderTimeline() {
  const tasks = Store.getTasks({ completed: false }).filter(t => t.dueDate);
  const grouped = {};
  tasks.sort((a, b) => (a.dueDate || 0) - (b.dueDate || 0));
  tasks.forEach(t => {
    const key = formatDate(t.dueDate);
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(t);
  });

  let html = '<div class="timeline">';
  const entries = Object.entries(grouped);
  if (entries.length === 0) {
    html += '<div class="empty-state"><p>暂无带日期的任务</p></div>';
  } else {
    entries.forEach(([date, items]) => {
      html += '<div class="timeline-group">';
      html += '<div class="timeline-date">' + date + '</div>';
      items.forEach(t => {
        html += '<div class="timeline-item" onclick="openTaskDetail(' + t.id + ')">';
        html += '<div class="timeline-item-title">' + escHtml(t.title) + '</div>';
        html += '</div>';
      });
      html += '</div>';
    });
  }
  html += '</div>';
  return html;
}

// ============================================
// Screen: Kanban
// ============================================
function renderKanban() {
  const tasks = Store.getTasks({ completed: false });
  const columns = [
    { title: '待办', color: '#6366F1', filter: t => !t.dueDate },
    { title: '进行中', color: '#F59E0B', filter: t => !!t.dueDate && !isOverdue(t.dueDate) },
    { title: '逾期', color: '#EF4444', filter: t => !!t.dueDate && isOverdue(t.dueDate) }
  ];

  let html = '<div class="kanban-scroll"><div class="kanban-container">';
  columns.forEach(col => {
    const colTasks = tasks.filter(col.filter);
    html += '<div class="kanban-column">';
    html += '<div class="kanban-col-header">';
    html += '<div style="width:12px;height:12px;border-radius:50%;background:' + col.color + '"></div>';
    html += '<span class="kanban-col-title">' + col.title + '</span>';
    html += '<span class="kanban-col-count">' + colTasks.length + '</span>';
    html += '</div>';
    colTasks.forEach(t => {
      html += '<div class="kanban-task" onclick="openTaskDetail(' + t.id + ')">';
      html += '<div class="kanban-task-title">' + escHtml(t.title) + '</div>';
      if (t.priority > 0) {
        html += '<span style="font-size:0.75rem;color:' + getPriorityColors().find(p => p.value === t.priority).color + '">' + getPriorityLabel(t.priority) + '</span>';
      }
      html += '</div>';
    });
    if (colTasks.length === 0) {
      html += '<div style="padding:20px;text-align:center;color:var(--on-surface-variant);font-size:0.813rem">暂无任务</div>';
    }
    html += '</div>';
  });
  html += '</div></div>';
  return html;
}

// ============================================
// Screen: Search
// ============================================
function renderSearch() {
  return '<div class="search-box"><input class="search-input" id="search-input" placeholder="搜索任务..." oninput="searchTasks()" autofocus/></div>' +
    '<div id="search-results"></div>';
}

function searchTasks() {
  const q = document.getElementById('search-input').value.trim();
  const results = document.getElementById('search-results');
  if (!q) { results.innerHTML = ''; return; }

  const tasks = Store.getTasks({ search: q });
  if (tasks.length === 0) {
    results.innerHTML = '<div class="empty-state"><p>未找到匹配的任务</p></div>';
    return;
  }
  let html = '<div class="section-title">找到 ' + tasks.length + ' 个任务</div><div class="task-list">';
  tasks.forEach(t => { html += renderTaskItem(t); });
  html += '</div>';
  results.innerHTML = html;
}

// ============================================
// Screen: Settings
// ============================================
function renderSettings() {
  const settings = Store.getSettings();
  const isDark = Store.getSetting('darkMode', false);

  let html = '<div class="settings-list">';

  // General
  html += '<div class="settings-section">';
  html += '<div class="settings-section-title">通用</div>';
  html += settingsItem('theme', '主题模式', isDark ? '深色模式' : '浅色模式', 'toggleDarkMode()');
  html += settingsItem('language', '语言', '简体中文');
  html += '</div>';

  // Custom Theme
  html += '<div class="settings-section">';
  html += '<div class="settings-section-title">主题</div>';
  const accentColor = Store.getSetting('accentColor', '');
  html += '<div style="padding:4px 16px 12px">';
  html += '<div class="color-picker" id="color-picker">';
  const colors = ['#4A90E2','#6366F1','#8B5CF6','#EC4899','#EF4444','#F59E0B','#10B981','#14B8A6','#06B6D4'];
  colors.forEach(c => {
    const sel = accentColor === c ? ' selected' : '';
    html += '<div class="color-swatch' + sel + '" style="background:' + c + '" onclick="setAccentColor(\'' + c + '\')"></div>';
  });
  html += '</div>';
  html += '<button class="text-btn" onclick="resetAccentColor()" style="margin-top:8px;font-size:0.813rem;color:var(--on-surface-variant);background:none;border:none;cursor:pointer">重置默认</button>';
  html += '</div>';
  html += '</div>';

  // Data
  html += '<div class="settings-section">';
  html += '<div class="settings-section-title">数据</div>';
  html += settingsItem('storage', '数据存储', '仅本地存储');
  html += settingsItem('export', '导出数据', '备份到文件', 'exportData()');
  html += settingsItem('import', '导入数据', '从文件恢复', 'document.getElementById(\'import-file\').click()');
  html += settingsItem('export', '导出 CSV', '导出为表格文件', 'exportCSV()');
  html += settingsItem('import', '从 TickTick 导入', '导入 TickTick 备份', 'importFromTickTick()');
  html += settingsItem('import', '从 Todoist 导入', '导入 Todoist 备份', 'importFromTodoist()');
  html += settingsItem('clear', '清除所有数据', '重置应用', 'confirmClearData()');
  html += '</div>';

  // Automation
  html += '<div class="settings-section">';
  html += '<div class="settings-section-title">自动化</div>';
  html += settingsItem('theme', '自动化规则', '条件触发自动操作', 'showRules()');
  html += '</div>';

  // About
  html += '<div class="settings-section">';
  html += '<div class="settings-section-title">关于</div>';
  html += settingsItem('about', '关于轻刻', '版本 1.0.0');
  html += '</div>';

  html += '<div class="settings-footer">';
  html += '<h2>轻刻</h2>';
  html += '<p>让每一天都有所进步</p>';
  html += '</div>';

  html += '</div>';

  html += '<input type="file" id="import-file" accept=".json" style="display:none" onchange="importData(event)"/>';

  return html;
}

function settingsItem(icon, title, desc, onClick) {
  const icons = {
    theme: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>',
    language: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z"/></svg>',
    storage: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>',
    export: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>',
    import: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>',
    clear: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon" style="color:var(--danger)"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>',
    about: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="settings-item-icon"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
  };

  return '<div class="settings-item"' + (onClick ? ' onclick="' + onClick + '"' : '') + '>' +
    (icons[icon] || '') +
    '<div class="settings-item-info">' +
      '<div class="settings-item-title">' + title + '</div>' +
      '<div class="settings-item-desc">' + desc + '</div>' +
    '</div>' +
    '<span class="settings-item-arrow">' + Icons.chevronRight + '</span>' +
  '</div>';
}

function toggleDarkMode() {
  const isDark = Store.getSetting('darkMode', false);
  Store.updateSetting('darkMode', !isDark);
  applyTheme(!isDark);
  renderCurrentScreen();
}

function applyTheme(dark) {
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  document.querySelector('meta[name="theme-color"]').content = dark ? '#1A1A1A' : '#4A90E2';
  // Apply custom accent color
  const accent = Store.getSetting('accentColor', '');
  if (accent) {
    document.documentElement.style.setProperty('--primary', accent);
  }
}

function exportData() {
  const data = Store.exportData();
  const blob = new Blob([data], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = 'qingke-backup-' + new Date().toISOString().split('T')[0] + '.json';
  a.click();
  URL.revokeObjectURL(url);
  showToast('数据已导出');
}

function importData(event) {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = function(e) {
    if (Store.importData(e.target.result)) {
      showToast('数据已导入');
      renderCurrentScreen();
    } else {
      showToast('导入失败，请检查文件格式');
    }
  };
  reader.readAsText(file);
}

function confirmClearData() {
  if (confirm('确定要清除所有数据吗？此操作不可恢复！')) {
    Store.clearAll();
    showToast('数据已清除');
    renderCurrentScreen();
  }
}

// ============================================
// Screen: Stats
// ============================================
function renderStats() {
  const stats = Store.getStats();
  let html = '<div style="padding:20px">';

  html += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px">';
  html += statCard('总任务', stats.total, '#4A90E2');
  html += statCard('已完成', stats.completed, '#27AE60');
  html += statCard('完成率', stats.completionRate + '%', '#F39C12');
  html += statCard('逾期', stats.overdue, '#E74C3C');
  html += '</div>';

  html += '<div class="detail-section">';
  html += '<h4>本周完成</h4>';
  html += '<div style="font-size:1.5rem;font-weight:700;color:var(--completed)">' + stats.weekCompleted + ' 个</div>';
  html += '</div>';

  html += '<div class="detail-section">';
  html += '<h4>今日专注</h4>';
  html += '<div style="font-size:1.5rem;font-weight:700;color:var(--primary)">' + stats.todayFocus + ' 次</div>';
  html += '<div style="color:var(--on-surface-variant);font-size:0.875rem">总时长 ' + formatDuration(stats.todayFocusTime) + '</div>';
  html += '</div>';

  html += '</div>';
  return html;
}

function statCard(label, value, color) {
  return '<div class="card" style="padding:16px;text-align:center">' +
    '<div style="font-size:1.75rem;font-weight:700;color:' + color + '">' + value + '</div>' +
    '<div style="font-size:0.75rem;color:var(--on-surface-variant);margin-top:4px">' + label + '</div>' +
  '</div>';
}

// ============================================
// Task Detail & Form
// ============================================
let _currentTaskId = null;
let _editTaskId = null;

function openTaskDetail(taskId) {
  _currentTaskId = taskId;
  const task = Store.getTask(taskId);
  if (!task) return;

  const title = document.getElementById('task-modal-title');
  const body = document.getElementById('task-detail-body');
  if (title) title.textContent = '任务详情';
  document.getElementById('task-edit-btn').style.display = '';
  document.getElementById('task-delete-btn').style.display = '';

  const prioColor = getPriorityColors().find(p => p.value === (task.priority || 0));

  let html = '<div style="display:flex;align-items:flex-start;gap:12px;margin-bottom:16px">';
  html += '<div class="task-check' + (task.isCompleted ? ' checked' : '') + '" onclick="toggleTask(' + task.id + ')"></div>';
  html += '<div style="flex:1">';
  html += '<h3 style="font-size:1.125rem;word-break:break-word;' + (task.isCompleted ? 'text-decoration:line-through;color:var(--on-surface-variant)' : '') + '">' + escHtml(task.title) + '</h3>';
  html += '</div></div>';

  if (task.description) {
    html += '<div class="detail-section">';
    html += '<h4>描述</h4>';
    html += '<p style="font-size:0.875rem;line-height:1.5;white-space:pre-wrap">' + escHtml(task.description) + '</p>';
    html += '</div>';
  }

  html += '<div class="detail-section">';
  html += '<h4>详细信息</h4>';
  if (task.priority > 0 && prioColor) {
    html += '<div class="detail-row"><span class="detail-row-label">优先级</span><span class="detail-row-value" style="color:' + prioColor.color + '">' + prioColor.label + '</span></div>';
  }
  if (task.dueDate) {
    html += '<div class="detail-row"><span class="detail-row-label">截止日期</span><span class="detail-row-value">' + formatDate(task.dueDate) + '</span></div>';
  }
  if (task.projectId) {
    const p = Store.getProjects().find(x => x.id === task.projectId);
    html += '<div class="detail-row"><span class="detail-row-label">项目</span><span class="detail-row-value">' + (p ? p.name : '') + '</span></div>';
  }
  html += '</div>';

  html += '<div class="detail-section">';
  html += '<h4>时间信息</h4>';
  html += '<div class="detail-row"><span class="detail-row-label">创建时间</span><span class="detail-row-value">' + formatDateTime(task.createdAt) + '</span></div>';
  if (task.completedTime) {
    html += '<div class="detail-row"><span class="detail-row-label">完成时间</span><span class="detail-row-value" style="color:var(--completed)">' + formatDateTime(task.completedTime) + '</span></div>';
  }
  html += '</div>';

  // Attachments
  html += renderAttachments(task.id);
  html += '<div style="padding:8px 0">';
  html += '<button class="btn" onclick="document.getElementById(\'attachment-file\').click()" style="font-size:0.813rem">+ 添加附件</button>';
  html += '</div>';

  body.innerHTML = html;
  document.getElementById('task-modal').classList.add('open');
}

function closeTaskModal() {
  document.getElementById('task-modal').classList.remove('open');
  _currentTaskId = null;
}

function editCurrentTask() {
  const task = Store.getTask(_currentTaskId);
  if (!task) return;
  closeTaskModal();
  openTaskForm(task);
}

function deleteCurrentTask() {
  const task = Store.getTask(_currentTaskId);
  if (!task) return;
  showConfirm('确定删除"' + task.title + '"吗？', () => {
    Store.deleteTask(_currentTaskId);
    closeTaskModal();
    renderCurrentScreen();
    showToast('已删除');
  });
}

function toggleTask(id) {
  Store.toggleTask(id);
  renderCurrentScreen();
  // Update open detail if visible
  if (_currentTaskId === id && document.getElementById('task-modal').classList.contains('open')) {
    openTaskDetail(id);
  }
}

// ============================================
// Task Form
// ============================================
function openTaskForm(existingTask) {
  _editTaskId = existingTask ? existingTask.id : null;
  const title = document.getElementById('task-form-title');
  title.textContent = existingTask ? '编辑任务' : '新建任务';

  const task = existingTask || { title: '', description: '', priority: 0, dueDate: '', projectId: null, isImportant: false, isUrgent: false };
  const projects = Store.getProjects();
  const prios = getPriorityColors();

  let html = '<div class="form-group">';
  html += '<label class="form-label">任务标题</label>';
  html += '<div style="display:flex;gap:8px">';
  html += '<input class="form-input" id="form-title" value="' + escHtml(task.title) + '" placeholder="输入任务标题" style="flex:1"/>';
  html += '<button type="button" class="icon-btn" id="voice-btn" onclick="startVoiceInput()" title="语音输入"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/><path d="M19 10v2a7 7 0 01-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg></button>';
  html += '</div>';
  html += '</div>';

  html += '<div class="form-group">';
  html += '<label class="form-label">描述</label>';
  html += '<textarea class="form-input" id="form-desc" placeholder="添加描述...">' + escHtml(task.description) + '</textarea>';
  html += '</div>';

  html += '<div class="form-group">';
  html += '<label class="form-label">优先级</label>';
  html += '<div class="priority-selector" id="form-priority">';
  prios.forEach(p => {
    const sel = task.priority === p.value ? ' selected' : '';
    html += '<div class="priority-opt ' + p.cls + sel + '" onclick="selectPriority(' + p.value + ')" data-prio="' + p.value + '">' + p.label + '</div>';
  });
  html += '</div>';
  html += '</div>';

  html += '<div class="form-group">';
  html += '<label class="form-label">截止日期</label>';
  if (task.dueDate) {
    const d = new Date(task.dueDate);
    const val = d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
    html += '<input class="form-input" type="date" id="form-date" value="' + val + '"/>';
  } else {
    html += '<input class="form-input" type="date" id="form-date"/>';
  }
  html += '</div>';

  html += '<div class="form-group">';
  html += '<label class="form-label">项目</label>';
  html += '<select class="form-select" id="form-project">';
  html += '<option value="">无</option>';
  projects.forEach(p => {
    const sel = task.projectId === p.id ? ' selected' : '';
    html += '<option value="' + p.id + '"' + sel + '>' + escHtml(p.name) + '</option>';
  });
  html += '</select>';
  html += '</div>';

  html += '<div class="form-group">';
  html += '<label class="form-label">四象限属性</label>';
  html += '<div style="display:flex;gap:16px;font-size:0.875rem">';
  html += '<label><input type="checkbox" id="form-important"' + (task.isImportant ? ' checked' : '') + ' onchange=""/> 重要</label>';
  html += '<label><input type="checkbox" id="form-urgent"' + (task.isUrgent ? ' checked' : '') + ' onchange=""/> 紧急</label>';
  html += '</div>';
  html += '</div>';

  document.getElementById('task-form-body').innerHTML = html;
  document.getElementById('task-form-modal').classList.add('open');
}

function selectPriority(value) {
  document.querySelectorAll('#form-priority .priority-opt').forEach(el => {
    el.classList.toggle('selected', parseInt(el.dataset.prio) === value);
  });
}

function saveTaskForm() {
  const title = document.getElementById('form-title').value.trim();
  if (!title) { showToast('请输入任务标题'); return; }

  const dueDateVal = document.getElementById('form-date').value;
  const dueDate = dueDateVal ? new Date(dueDateVal + 'T12:00:00').getTime() : null;
  const projectVal = document.getElementById('form-project').value;
  const priorityEl = document.querySelector('#form-priority .selected');
  const priority = priorityEl ? parseInt(priorityEl.dataset.prio) : 0;
  const isImportant = document.getElementById('form-important').checked;
  const isUrgent = document.getElementById('form-urgent').checked;

  const data = {
    title,
    description: document.getElementById('form-desc').value.trim(),
    priority,
    dueDate,
    projectId: projectVal ? parseInt(projectVal) : null,
    isImportant,
    isUrgent
  };

  if (_editTaskId) {
    Store.updateTask(_editTaskId, data);
    showToast('任务已更新');
  } else {
    const newTask = Store.addTask(data);
    const ruleResults = Store.applyRules(newTask);
    if (ruleResults.length > 0) {
      showToast('任务已创建 (' + ruleResults.join(', ') + ')');
    } else {
      showToast('任务已创建');
    }
  }

  closeTaskFormModal();
  renderCurrentScreen();
}

function closeTaskFormModal() {
  document.getElementById('task-form-modal').classList.remove('open');
  _editTaskId = null;
}

// ============================================
// FAB handler
// ============================================
function onFabClick() {
  const route = window._currentRoute;
  switch (route) {
    case 'home': case 'projects': case 'timeline': case 'kanban':
    case 'matrix': case 'search':
      openTaskForm();
      break;
    case 'habits':
      const name = prompt('习惯名称：');
      if (name && name.trim()) {
        Store.addHabit(name.trim());
        renderCurrentScreen();
        showToast('习惯已创建');
      }
      break;
    case 'countdowns':
      openCountdownForm();
      break;
    case 'tags':
      const tagName = prompt('标签名称：');
      if (tagName && tagName.trim()) {
        Store.addTag(tagName.trim());
        renderCurrentScreen();
        showToast('标签已创建');
      }
      break;
    case 'projects':
      const projName = prompt('项目名称：');
      if (projName && projName.trim()) {
        Store.addProject(projName.trim());
        renderCurrentScreen();
        showToast('项目已创建');
      }
      break;
    default:
      openTaskForm();
  }
}

function openCountdownForm() {
  const name = prompt('倒计时名称：');
  if (!name || !name.trim()) return;
  const dateStr = prompt('目标日期 (YYYY-MM-DD)：', new Date().toISOString().split('T')[0]);
  if (!dateStr) return;
  const targetDate = new Date(dateStr + 'T00:00:00').getTime();
  Store.addCountdown(name.trim(), targetDate);
  renderCurrentScreen();
  showToast('倒计时已创建');
}

// ============================================
// Dialogs
// ============================================
function showDialog(html) {
  document.getElementById('task-detail-body').innerHTML = html;
  document.getElementById('task-modal-title').textContent = '';
  document.getElementById('task-edit-btn').style.display = 'none';
  document.getElementById('task-delete-btn').style.display = 'none';
  document.getElementById('task-modal').classList.add('open');
}

let _confirmCallback = null;

function showConfirm(msg, callback) {
  _confirmCallback = callback;
  document.getElementById('confirm-message').textContent = msg;
  document.getElementById('confirm-btn').onclick = function() {
    closeConfirmDialog();
    if (_confirmCallback) _confirmCallback();
  };
  document.getElementById('confirm-dialog').classList.add('open');
}

function closeConfirmDialog() {
  document.getElementById('confirm-dialog').classList.remove('open');
  _confirmCallback = null;
}

function closeModal(event) {
  if (event && event.target !== event.currentTarget) return;
  document.querySelectorAll('.modal.open').forEach(m => m.classList.remove('open'));
}

function showToast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._timer);
  el._timer = setTimeout(() => el.classList.remove('show'), 2000);
}

// ============================================
// Drag & Drop Handlers
// ============================================
function onDragStart(e) {
  _dragSourceId = parseInt(e.target.dataset.taskId);
  e.dataTransfer.effectAllowed = 'move';
  e.dataTransfer.setData('text/plain', _dragSourceId);
  const item = e.target.closest('.task-item');
  if (item) item.classList.add('dragging');
}

function onDragOver(e) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'move';
  const item = e.target.closest('.task-item');
  if (item) item.classList.add('drag-over');
}

function onDrop(e) {
  e.preventDefault();
  const targetEl = e.target.closest('.task-item');
  if (!targetEl) return;
  const targetId = parseInt(targetEl.dataset.taskId);
  if (_dragSourceId && targetId && _dragSourceId !== targetId) {
    Store.swapSortOrder(_dragSourceId, targetId);
    renderCurrentScreen();
  }
  document.querySelectorAll('.task-item.dragging, .task-item.drag-over').forEach(el => {
    el.classList.remove('dragging', 'drag-over');
  });
  _dragSourceId = null;
}

function onDragEnd(e) {
  const item = e.target.closest('.task-item');
  if (item) item.classList.remove('dragging');
  document.querySelectorAll('.task-item.drag-over').forEach(el => el.classList.remove('drag-over'));
  _dragSourceId = null;
}

// ============================================
// Batch Mode
// ============================================
function enterBatchMode() {
  _batchMode = true;
  _batchSelected = new Set();
  renderCurrentScreen();
}

function exitBatchMode() {
  _batchMode = false;
  _batchSelected = new Set();
  renderCurrentScreen();
}

function toggleBatchSelect(id) {
  if (_batchSelected.has(id)) {
    _batchSelected.delete(id);
  } else {
    _batchSelected.add(id);
  }
  const countEl = document.getElementById('batch-count');
  if (countEl) countEl.textContent = _batchSelected.size + ' 已选';
  renderCurrentScreen();
}

function executeBatchComplete() {
  if (_batchSelected.size === 0) { showToast('请选择任务'); return; }
  Store.batchComplete([..._batchSelected]);
  showToast('已完成 ' + _batchSelected.size + ' 个任务');
  exitBatchMode();
}

function executeBatchDelete() {
  if (_batchSelected.size === 0) { showToast('请选择任务'); return; }
  showConfirm('确定删除选中的 ' + _batchSelected.size + ' 个任务吗？', () => {
    Store.batchDelete([..._batchSelected]);
    showToast('已删除 ' + _batchSelected.size + ' 个任务');
    exitBatchMode();
  });
}

function quickDeleteTask(id) {
  Store.deleteTask(id);
  renderCurrentScreen();
  showToast('已删除');
}

// ============================================
// Templates
// ============================================
function saveAsTemplate() {
  const title = document.getElementById('form-title').value.trim();
  if (!title) { showToast('请先输入任务标题'); return; }
  const tplName = prompt('模板名称：', title);
  if (!tplName) return;
  const dueDateVal = document.getElementById('form-date').value;
  const dueDate = dueDateVal ? new Date(dueDateVal + 'T12:00:00').getTime() : null;
  const projectVal = document.getElementById('form-project').value;
  const priorityEl = document.querySelector('#form-priority .selected');
  const priority = priorityEl ? parseInt(priorityEl.dataset.prio) : 0;
  const isImportant = document.getElementById('form-important').checked;
  const isUrgent = document.getElementById('form-urgent').checked;

  Store.addTemplate(tplName, {
    title: tplName,
    description: document.getElementById('form-desc').value.trim(),
    priority, dueDate,
    projectId: projectVal ? parseInt(projectVal) : null,
    isImportant, isUrgent
  });
  showToast('模板已保存');
}

function showTemplates() {
  const templates = Store.getTemplates();
  if (templates.length === 0) {
    showToast('暂无模板，请先在任务表单中保存模板');
    return;
  }
  let html = '<div style="padding:12px">';
  html += '<h3 style="margin-bottom:12px">选择模板</h3>';
  templates.forEach(t => {
    html += '<div class="template-item" onclick="applyTemplate(' + t.id + ')">';
    html += '<span style="flex:1">' + escHtml(t.name) + '</span>';
    html += '<button class="icon-btn" onclick="event.stopPropagation();deleteTemplateConfirm(' + t.id + ')" style="color:var(--danger);font-size:0.75rem">✕</button>';
    html += '</div>';
  });
  html += '</div>';
  showDialog(html);
}

function applyTemplate(id) {
  const tpl = Store.getTemplates().find(t => t.id === id);
  if (!tpl) return;
  closeModal({ target: document.getElementById('task-modal'), currentTarget: document.getElementById('task-modal') });
  // Pre-fill the form with template data - use a slight delay for modal close
  setTimeout(() => openTaskForm(tpl.taskData), 200);
}

function deleteTemplateConfirm(id) {
  showConfirm('确定删除此模板？', () => {
    Store.deleteTemplate(id);
    showToast('模板已删除');
    showTemplates();
  });
}

// ============================================
// Voice Input
// ============================================
function startVoiceInput() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    showToast('当前浏览器不支持语音输入');
    return;
  }
  const recognition = new SpeechRecognition();
  recognition.lang = 'zh-CN';
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;

  const btn = document.getElementById('voice-btn');
  if (btn) btn.classList.add('listening');

  recognition.onresult = function(e) {
    const text = e.results[0][0].transcript;
    const input = document.getElementById('form-title');
    if (input) input.value = text;
    if (btn) btn.classList.remove('listening');
    showToast('已识别: ' + text);
  };

  recognition.onerror = function() {
    if (btn) btn.classList.remove('listening');
    showToast('语音识别失败');
  };

  recognition.onend = function() {
    if (btn) btn.classList.remove('listening');
  };

  try { recognition.start(); } catch (err) { showToast('语音识别启动失败'); }
}

// ============================================
// Attachments
// ============================================
function handleAttachmentFile(event) {
  const file = event.target.files[0];
  if (!file) return;
  if (_currentTaskId) {
    Store.addAttachment(_currentTaskId, file).then(() => {
      showToast('附件已添加');
      if (document.getElementById('task-modal').classList.contains('open')) {
        openTaskDetail(_currentTaskId);
      }
    });
  } else {
    showToast('请先保存任务');
  }
  event.target.value = '';
}

function renderAttachments(taskId) {
  const attachments = Store.getAttachments(taskId);
  if (attachments.length === 0) return '';
  let html = '<div class="detail-section"><h4>附件</h4>';
  attachments.forEach(a => {
    html += '<div class="attachment-item">';
    if (a.type && a.type.startsWith('image/')) {
      html += '<img src="' + a.data + '" alt="' + escHtml(a.name) + '" style="width:40px;height:40px;border-radius:6px;object-fit:cover"/>';
    } else {
      html += '<span style="font-size:1.25rem">📎</span>';
    }
    html += '<span style="flex:1;font-size:0.813rem">' + escHtml(a.name) + '</span>';
    html += '<button class="icon-btn" onclick="event.stopPropagation();deleteAttachmentConfirm(' + a.id + ')" style="color:var(--danger);font-size:0.75rem">✕</button>';
    html += '</div>';
  });
  html += '</div>';
  return html;
}

function deleteAttachmentConfirm(id) {
  showConfirm('确定删除此附件？', () => {
    Store.deleteAttachment(id);
    showToast('附件已删除');
    if (_currentTaskId && document.getElementById('task-modal').classList.contains('open')) {
      openTaskDetail(_currentTaskId);
    }
  });
}

// ============================================
// Automation Rules
// ============================================
function showRules() {
  const rules = Store.getRules();
  let html = '<div style="padding:12px">';
  html += '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">';
  html += '<h3>自动化规则</h3>';
  html += '<button class="btn btn-primary" onclick="addNewRule()">添加</button>';
  html += '</div>';
  if (rules.length === 0) {
    html += '<div class="empty-state"><p>暂无规则</p></div>';
  } else {
    rules.forEach(r => {
      html += '<div class="rule-item">';
      html += '<div class="toggle-switch' + (r.enabled ? ' on' : '') + '" onclick="event.stopPropagation();toggleRule(' + r.id + ')"></div>';
      html += '<div style="flex:1;margin:0 12px">';
      html += '<div style="font-weight:500;font-size:0.875rem">' + escHtml(r.name) + '</div>';
      html += '<div style="font-size:0.75rem;color:var(--on-surface-variant)">' + conditionText(r) + ' → ' + actionText(r) + '</div>';
      html += '</div>';
      html += '<button class="icon-btn" onclick="event.stopPropagation();deleteRuleConfirm(' + r.id + ')" style="color:var(--danger);font-size:0.75rem">✕</button>';
      html += '</div>';
    });
  }
  html += '</div>';
  showDialog(html);
}

function conditionText(rule) {
  const c = rule.condition;
  switch (c.type) {
    case 'priority': return '优先级 ' + (c.op === '>=' ? '≥' : c.op === '<=' ? '≤' : '=') + ' ' + getPriorityLabel(c.value);
    case 'hasDueDate': return c.op === 'yes' ? '有截止日期' : '无截止日期';
    case 'titleContains': return '标题包含 "' + c.value + '"';
    default: return '未知条件';
  }
}

function actionText(rule) {
  const a = rule.action;
  switch (a.type) {
    case 'setPriority': return '设置优先级为 ' + getPriorityLabel(a.value);
    case 'moveToProject': {
      const p = Store.getProjects().find(x => x.id === a.value);
      return '移动到 ' + (p ? p.name : '项目');
    }
    case 'setImportant': return '标记为重要';
    case 'addTag': {
      const tag = Store.getTags().find(x => x.id === a.value);
      return '添加标签 ' + (tag ? tag.name : '');
    }
    default: return '未知动作';
  }
}

function addNewRule() {
  const name = prompt('规则名称：');
  if (!name) return;
  const condType = prompt('条件类型 (priority / hasDueDate / titleContains)：', 'priority');
  if (!condType) return;
  const rule = {
    name: name.trim(),
    condition: { type: condType, op: '>=', value: 5 },
    action: { type: 'setPriority', value: 3 }
  };
  Store.addRule(rule);
  showToast('规则已创建');
  showRules();
}

function toggleRule(id) {
  const rule = Store.getRules().find(r => r.id === id);
  if (rule) {
    Store.updateRule(id, { enabled: !rule.enabled });
    showRules();
  }
}

function deleteRuleConfirm(id) {
  showConfirm('确定删除此规则？', () => {
    Store.deleteRule(id);
    showToast('规则已删除');
  });
}

// ============================================
// Export CSV
// ============================================
function exportCSV() {
  const csv = Store.exportCSV();
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = 'qingke-tasks-' + new Date().toISOString().split('T')[0] + '.csv';
  a.click();
  URL.revokeObjectURL(url);
  showToast('CSV 已导出');
}

// ============================================
// External Import (TickTick / Todoist)
// ============================================
function importFromTickTick() {
  document.getElementById('external-import-file').dataset.importType = 'ticktick';
  document.getElementById('external-import-file').click();
}

function importFromTodoist() {
  document.getElementById('external-import-file').dataset.importType = 'todoist';
  document.getElementById('external-import-file').click();
}

function handleExternalImport(event) {
  const file = event.target.files[0];
  if (!file) return;
  const type = event.target.dataset.importType || 'ticktick';
  const reader = new FileReader();
  reader.onload = function(e) {
    let count = 0;
    if (type === 'ticktick') {
      count = Store.importFromTickTick(e.target.result);
    } else {
      count = Store.importFromTodoist(e.target.result);
    }
    if (count > 0) {
      showToast('成功导入 ' + count + ' 个任务');
      renderCurrentScreen();
    } else {
      showToast('导入失败，请检查文件格式');
    }
  };
  reader.readAsText(file);
  event.target.value = '';
}

// ============================================
// Swipe Gestures
// ============================================
function onSwipeStart(e) {
  _swipeStartX = e.touches[0].clientX;
  _swipeTaskId = parseInt(e.currentTarget.dataset.taskId);
}

function onSwipeMove(e) {
  if (!_swipeTaskId) return;
  const diff = e.touches[0].clientX - _swipeStartX;
  const el = e.currentTarget;
  if (diff < -30 || diff > 30) {
    el.style.transform = 'translateX(' + diff + 'px)';
  }
}

function onSwipeEnd(e) {
  if (!_swipeTaskId) return;
  const diff = e.changedTouches[0].clientX - _swipeStartX;
  const el = e.currentTarget;
  el.style.transform = '';
  if (diff < -80) {
    toggleTask(_swipeTaskId);
  } else if (diff > 80) {
    quickDeleteTask(_swipeTaskId);
  }
  _swipeTaskId = null;
  _swipeStartX = 0;
}

// ============================================
// Custom Theme Color
// ============================================
function setAccentColor(color) {
  Store.updateSetting('accentColor', color);
  document.documentElement.style.setProperty('--primary', color);
  renderCurrentScreen();
  showToast('主题色已更新');
}

function resetAccentColor() {
  Store.updateSetting('accentColor', null);
  document.documentElement.style.setProperty('--primary', '#4A90E2');
  renderCurrentScreen();
  showToast('主题色已重置');
}

// ============================================
// Screen registry
// ============================================
const ScreenRenderers = {
  home: renderHome,
  calendar: renderCalendar,
  focus: renderFocus,
  chat: renderChat,
  projects: renderProjects,
  tags: renderTags,
  habits: renderHabits,
  countdowns: renderCountdowns,
  matrix: renderMatrix,
  timeline: renderTimeline,
  kanban: renderKanban,
  search: renderSearch,
  settings: renderSettings,
  stats: renderStats,
  more: () => ''
};
