<template>
  <div class="app-container">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo-icon">📧</div>
        <h2>Antigravity Mail</h2>
      </div>
      
      <nav class="nav-menu">
        <button class="nav-item active">
          <span class="icon">📥</span> Inbox
          <span class="badge" v-if="emailStore.unreadCount > 0">{{ emailStore.unreadCount }}</span>
        </button>
        <button class="nav-item">
          <span class="icon">⭐</span> Starred
        </button>
        <button class="nav-item">
          <span class="icon">📤</span> Sent
        </button>
        <button class="nav-item">
          <span class="icon">🗑️</span> Trash
        </button>
      </nav>

      <div class="connection-status" :class="{ online: emailStore.connected }">
        <div class="status-dot"></div>
        {{ emailStore.connected ? 'Live Sync Active' : 'Connecting...' }}
      </div>
    </aside>

    <main class="main-content">
      <header class="topbar">
        <div class="search-bar">
          <span class="search-icon">🔍</span>
          <input type="text" placeholder="Search in mail..." />
        </div>
        <div class="user-profile">
          <img src="https://ui-avatars.com/api/?name=User&background=6c5ce7&color=fff" alt="Profile" />
        </div>
      </header>

      <div class="inbox-container">
        <!-- Email List -->
        <div class="email-list">
          <div v-if="emailStore.emails.length === 0" class="empty-list">
            <p>Inbox is empty.</p>
            <small>Send an email via terminal to see it here!</small>
          </div>
          
          <div 
            v-for="email in emailStore.emails" 
            :key="email.id"
            class="email-item" 
            :class="{ unread: !email.read, active: emailStore.selectedEmail?.id === email.id }"
            @click="emailStore.selectEmail(email)"
          >
            <div class="sender">{{ email.from }}</div>
            <div class="subject">{{ email.subject || 'No Subject' }}</div>
            <div class="preview">{{ email.body }}</div>
            <div class="time">{{ email.timestamp }}</div>
          </div>
        </div>

        <!-- Email Viewer -->
        <div class="email-viewer" :class="{ 'empty-state': !emailStore.selectedEmail }">
          <div v-if="!emailStore.selectedEmail" class="glass-panel">
            <div class="empty-icon">✉️</div>
            <h3>Select an email to read</h3>
            <p>Waiting for WebSocket events from Kafka...</p>
          </div>
          
          <div v-else class="email-details">
            <div class="email-header">
              <h2>{{ emailStore.selectedEmail.subject || 'No Subject' }}</h2>
              <div class="meta-row">
                <div class="sender-info">
                  <img :src="`https://ui-avatars.com/api/?name=${emailStore.selectedEmail.from}&background=random`" class="avatar" />
                  <div>
                    <div class="name">{{ emailStore.selectedEmail.from }}</div>
                    <div class="to">to {{ emailStore.selectedEmail.to }}</div>
                  </div>
                </div>
                <div class="date-info">{{ emailStore.selectedEmail.timestamp }}</div>
              </div>
            </div>
            <div class="email-body">
              {{ emailStore.selectedEmail.body }}
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useEmailStore } from './store/emailStore'

const emailStore = useEmailStore()

onMounted(() => {
  emailStore.connectWebSocket()
})
</script>

<style>
/* Modern Premium Dark Mode Aesthetics */
:root {
  --bg-dark: #0f172a;
  --bg-panel: rgba(30, 41, 59, 0.7);
  --text-main: #f8fafc;
  --text-muted: #94a3b8;
  --accent: #6366f1;
  --accent-hover: #4f46e5;
  --border: rgba(255, 255, 255, 0.08);
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

body {
  background-color: var(--bg-dark);
  color: var(--text-main);
  height: 100vh;
  overflow: hidden;
}

.app-container {
  display: flex;
  height: 100vh;
  background: radial-gradient(circle at top left, #1e1b4b, var(--bg-dark));
}

.sidebar {
  width: 260px;
  background: var(--bg-panel);
  backdrop-filter: blur(12px);
  border-right: 1px solid var(--border);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 24px;
  background: linear-gradient(135deg, var(--accent), #ec4899);
  -webkit-background-clip: text;
  color: transparent;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-main);
}

.nav-item.active {
  background: rgba(99, 102, 241, 0.15);
  color: var(--accent);
}

.badge {
  margin-left: auto;
  background: var(--accent);
  color: white;
  padding: 2px 8px;
  border-radius: 100px;
  font-size: 12px;
}

.connection-status {
  padding: 12px;
  border-radius: 8px;
  background: rgba(0,0,0,0.2);
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.connection-status.online .status-dot {
  background: #22c55e;
  box-shadow: 0 0 10px #22c55e;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 72px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: var(--bg-panel);
  backdrop-filter: blur(12px);
}

.search-bar {
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 8px 16px;
  width: 400px;
}

.search-bar input {
  background: transparent;
  border: none;
  color: var(--text-main);
  outline: none;
  margin-left: 12px;
  width: 100%;
}

.user-profile img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--accent);
}

.inbox-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.email-list {
  width: 400px;
  border-right: 1px solid var(--border);
  overflow-y: auto;
  background: rgba(15, 23, 42, 0.4);
}

.empty-list {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-muted);
}

.email-item {
  padding: 20px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.email-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.email-item.active {
  background: rgba(99, 102, 241, 0.1);
}

.email-item.unread {
  background: rgba(99, 102, 241, 0.05);
}

.email-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent);
}

.email-item .sender {
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--text-main);
}

.email-item .subject {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.email-item .preview {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.email-item .time {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 12px;
  color: var(--text-muted);
}

.email-viewer {
  flex: 1;
  display: flex;
  background: var(--bg-dark);
}

.email-viewer.empty-state {
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.email-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.email-header {
  padding: 32px;
  border-bottom: 1px solid var(--border);
  background: rgba(255,255,255,0.02);
}

.email-header h2 {
  font-size: 24px;
  margin-bottom: 24px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sender-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sender-info .avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.sender-info .name {
  font-weight: 600;
  font-size: 15px;
}

.sender-info .to {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.date-info {
  color: var(--text-muted);
  font-size: 14px;
}

.email-body {
  padding: 40px 32px;
  line-height: 1.6;
  color: #e2e8f0;
  white-space: pre-wrap;
  overflow-y: auto;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 60px;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0px); }
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.8;
}

.glass-panel h3 {
  font-size: 24px;
  margin-bottom: 12px;
  background: linear-gradient(to right, #fff, #94a3b8);
  -webkit-background-clip: text;
  color: transparent;
}

.glass-panel p {
  color: var(--text-muted);
}
</style>
