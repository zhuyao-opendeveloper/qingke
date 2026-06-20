# 轻刻 (Qingke / OpenTodo)

> 开源的任务管理工具，轻量、现代、全平台可用

## 项目简介

轻刻是一款清新、现代的任务管理 Web 应用，灵感来自 TickTick / 滴答清单。纯前端 SPA，无需后端服务，数据存储在浏览器本地。

### 功能特性

- ✅ **任务管理** — 创建、编辑、删除、完成、优先级、项目、标签
- 📅 **日历视图** — 月视图、年视图，按日查看任务
- 🍅 **番茄钟专注** — 内置番茄钟，支持短/长休息
- 🗂️ **看板视图** — 拖拽式任务看板管理
- 📊 **艾森豪威尔矩阵** — 四象限优先级管理
- 📈 **习惯追踪** — 每日打卡习惯养成
- ⏱️ **倒计时** — 重要事项倒计时
- 💬 **AI 对话** — 内置聊天助手（需 API Key）
- 📋 **模板** — 任务模板保存与复用
- 🔄 **自动化规则** — 条件触发自动执行动作
- 🎨 **自定义主题** — 主题色 + 深色模式
- 📥 **数据导入/导出** — CSV 导出、TickTick/Todoist 导入
- 🎤 **语音输入** — 语音创建任务
- 🔗 **文件附件** — 图片、文档附件绑定
- 👆 **手势操作** — 拖拽排序、滑动完成/删除
- 📱 **响应式设计** — 移动端优先，支持 PWA 安装
- 🌙 **深色模式** — 跟随系统或手动切换

### 在线体验

👉 [https://zhuyao-opendeveloper.github.io/qingke/](https://zhuyao-opendeveloper.github.io/qingke/) — 项目介绍页
👉 [https://zhuyao-opendeveloper.github.io/qingke/qingke-web/](https://zhuyao-opendeveloper.github.io/qingke/qingke-web/) — 应用入口

## 技术栈

- **HTML / CSS / JavaScript** — 纯前端 SPA，零框架零构建
- **localStorage** — 浏览器本地持久化存储
- **GitHub Pages** — 静态部署
- **PWA** — 支持离线缓存 + 安装到主屏幕

## 快速开始

```bash
git clone https://github.com/zhuyao-opendeveloper/qingke.git
cd qingke-web
# 直接用浏览器打开 index.html 即可
```

或使用任意 HTTP 服务器：

```bash
# Python
python -m http.server 8080

# Node.js
npx serve qingke-web
```

## 项目结构

```
/
├── index.html           # 项目介绍页
├── qingke-web/          # 主应用
│   ├── index.html       # 入口页面
│   ├── manifest.json    # PWA 配置
│   ├── sw.js            # Service Worker 离线缓存
│   ├── icons/           # 应用图标
│   ├── css/style.css    # 样式
│   └── js/
│       ├── app.js       # 应用初始化与路由
│       ├── screens.js   # 各页面渲染
│       ├── store.js     # 数据持久化
│       └── icons.js     # SVG 图标系统
├── .github/workflows/   # GitHub Actions 部署
└── LICENSE
```

## 手机安装

用手机浏览器打开 [https://zhuyao-opendeveloper.github.io/qingke/qingke-web/](https://zhuyao-opendeveloper.github.io/qingke/qingke-web/)：

- **Android** — Chrome 菜单 → 添加到主屏幕
- **iOS** — Safari 分享 → 添加到主屏幕

安装后离线可用，全屏沉浸式体验。

## 开源协议

本项目采用 MIT 协议开源 — 详见 [LICENSE](LICENSE) 文件。
