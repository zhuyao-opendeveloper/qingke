/**
 * 轻刻 - Main Application
 */

let _currentRoute = 'home';
const titleMap = {
  home: '轻刻', calendar: '日历', focus: '专注', chat: '聊天',
  projects: '项目', tags: '标签', habits: '习惯', countdowns: '倒计时',
  matrix: '四象限', timeline: '时间线', kanban: '看板', search: '搜索',
  settings: '设置', stats: '统计', more: '更多'
};

// Routes that should show the FAB
const fabRoutes = ['home', 'projects', 'tags', 'habits', 'countdowns', 'matrix', 'timeline', 'kanban', 'search'];

// Routes that should NOT show the bottom bar
const noBottomRoutes = ['settings', 'stats'];

// ============================================
// Navigation
// ============================================
function navigateTo(route) {
  _currentRoute = route;

  // Update app bar title
  const appTitle = document.getElementById('app-title');
  if (appTitle) appTitle.textContent = titleMap[route] || '轻刻';

  // Show/Hide back button based on route
  // (routes from drawer get back, bottom tabs don't)
  const fromDrawer = ['projects', 'tags', 'habits', 'countdowns', 'matrix', 'timeline', 'kanban', 'search', 'settings', 'stats'];
  const menuBtn = document.getElementById('menu-btn');
  if (menuBtn) {
    if (fromDrawer.includes(route)) {
      // Show back arrow for drawer screens
      menuBtn.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>';
      menuBtn.onclick = function() { navigateTo('home'); };
    } else {
      menuBtn.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18M3 6h18M3 18h18"/></svg>';
      menuBtn.onclick = toggleDrawer;
    }
  }

  // Show/hide calendar toggle button
  const actions = document.getElementById('app-bar-actions');
  if (actions) {
    if (route === 'calendar') {
      actions.innerHTML = '<button class="icon-btn" onclick="toggleCalView()">' +
        '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>' +
        '</svg></button>';
    } else if (route === 'settings' || route === 'chat') {
      actions.innerHTML = '';
    } else {
      actions.innerHTML = '<button class="icon-btn" onclick="navigateTo(\'search\')">' +
        '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
        '<circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>' +
        '</svg></button>';
    }
  }

  // Update bottom tab active state
  document.querySelectorAll('.tab-item').forEach(tab => {
    tab.classList.toggle('active', tab.dataset.route === route);
  });

  // Update drawer active state
  document.querySelectorAll('.drawer-item').forEach(item => {
    item.classList.toggle('active', item.dataset.route === route);
  });

  // FAB visibility
  const fab = document.getElementById('fab');
  if (fab) {
    fab.style.display = fabRoutes.includes(route) ? 'flex' : 'none';
  }

  // Bottom bar visibility
  const bottomBar = document.getElementById('bottom-bar');
  if (bottomBar) {
    bottomBar.style.display = noBottomRoutes.includes(route) && window.innerWidth < 768 ? 'none' : 'flex';
    // Re-apply bottom padding for the main content
    const app = document.getElementById('app');
    if (app) {
      app.style.paddingBottom = noBottomRoutes.includes(route) && window.innerWidth < 768
        ? '0px'
        : 'calc(var(--bottom-bar-height) + var(--safe-bottom))';
    }
  }

  // Close drawer if open
  closeDrawer();

  // Render the screen
  renderCurrentScreen();
}

function renderCurrentScreen() {
  const main = document.getElementById('main-content');
  if (!main) return;

  const renderer = ScreenRenderers[_currentRoute];
  if (renderer) {
    main.innerHTML = '<div class="screen active" id="screen-' + _currentRoute + '">' + renderer() + '</div>';
  } else {
    main.innerHTML = '<div class="empty-state"><p>页面加载中...</p></div>';
  }

  // Focus search input if on search screen
  if (_currentRoute === 'search') {
    setTimeout(() => {
      const input = document.getElementById('search-input');
      if (input) input.focus();
    }, 100);
  }
}

// ============================================
// Drawer
// ============================================
function toggleDrawer() {
  const drawer = document.getElementById('drawer');
  const overlay = document.getElementById('drawer-overlay');
  const isOpen = drawer.classList.contains('open');
  drawer.classList.toggle('open');
  overlay.classList.toggle('open');
}

function closeDrawer() {
  document.getElementById('drawer').classList.remove('open');
  document.getElementById('drawer-overlay').classList.remove('open');
}

// ============================================
// Init
// ============================================
document.addEventListener('DOMContentLoaded', function() {
  // Apply saved theme
  const isDark = Store.getSetting('darkMode', false);
  applyTheme(isDark);

  // Initial render
  navigateTo('home');

  // Hide loading view
  setTimeout(() => {
    document.getElementById('loading-view').classList.add('hide');
  }, 400);

  // Handle back button (for Android) - not really needed in browser, but for completeness
  window.addEventListener('popstate', function() {
    navigateTo('home');
  });

  // Keyboard shortcuts
  document.addEventListener('keydown', function(e) {
    // Escape to close modals
    if (e.key === 'Escape') {
      const openModals = document.querySelectorAll('.modal.open');
      if (openModals.length > 0) {
        openModals.forEach(m => m.classList.remove('open'));
        return;
      }
    }
  });

  // Handle FAB label change based on context
  const fab = document.getElementById('fab');
  if (fab) {
    fab.addEventListener('contextmenu', function(e) {
      e.preventDefault();
      const route = _currentRoute;
      const labels = {
        home: '新建任务',
        habits: '新建习惯',
        countdowns: '新建倒计时',
        tags: '新建标签',
        projects: '新建项目'
      };
      showToast(labels[route] || '新建');
    });
  }
});

// ============================================
// Keyboard shortcut for '/' to search
// ============================================
document.addEventListener('keydown', function(e) {
  if (e.key === '/' && !e.ctrlKey && !e.metaKey) {
    const activeEl = document.activeElement;
    if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) return;
    e.preventDefault();
    navigateTo('search');
  }
});
