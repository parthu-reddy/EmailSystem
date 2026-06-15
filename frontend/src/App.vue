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
            <div class="subject">
              {{ email.subject || 'No Subject' }}
              <span v-if="email.attachments && email.attachments.length > 0" class="attachment-indicator">📎</span>
            </div>
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

            <!-- Attachments Section -->
            <div v-if="emailStore.selectedEmail.attachments && emailStore.selectedEmail.attachments.length > 0" class="attachments-section">
              <div class="attachments-header">
                <span class="attachments-icon">📎</span>
                <span>{{ emailStore.selectedEmail.attachments.length }} Attachment{{ emailStore.selectedEmail.attachments.length > 1 ? 's' : '' }}</span>
              </div>
              <div class="attachments-grid">
                <a
                  v-for="att in emailStore.selectedEmail.attachments"
                  :key="att.filename"
                  href="#"
                  @click.prevent="downloadAttachment(att)"
                  class="attachment-card"
                  :title="'Download ' + att.filename"
                >
                  <div class="attachment-icon">{{ getFileIcon(att.contentType, att.filename) }}</div>
                  <div class="attachment-info">
                    <div class="attachment-name">{{ att.filename }}</div>
                    <div class="attachment-meta">{{ att.displaySize }} · {{ getFileTypeLabel(att.contentType) }}</div>
                  </div>
                  <div class="download-icon">⬇️</div>
                </a>
              </div>
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

onMounted(async () => {
  await emailStore.fetchHistoricalEmails()
  emailStore.connectWebSocket()
})

async function downloadAttachment(att) {
  try {
    const response = await fetch(att.downloadUrl);
    if (!response.ok) throw new Error('Network response was not ok');
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = att.filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } catch (error) {
    console.error('Failed to download attachment:', error);
    alert('Failed to download attachment. Please try again.');
  }
}

function getFileIcon(contentType, filename) {
  if (!contentType) return '📄'
  const ct = contentType.toLowerCase()
  if (ct.includes('image')) return '🖼️'
  if (ct.includes('pdf')) return '📕'
  if (ct.includes('zip') || ct.includes('compressed') || ct.includes('archive')) return '🗜️'
  if (ct.includes('video')) return '🎬'
  if (ct.includes('audio') || ct.includes('mp3')) return '🎵'
  if (ct.includes('spreadsheet') || ct.includes('excel') || ct.includes('csv')) return '📊'
  if (ct.includes('presentation') || ct.includes('powerpoint')) return '📽️'
  if (ct.includes('word') || ct.includes('document')) return '📝'
  if (ct.includes('text')) return '📄'
  if (ct.includes('json') || ct.includes('xml')) return '🔧'
  return '📎'
}

function getFileTypeLabel(contentType) {
  if (!contentType) return 'File'
  const ct = contentType.toLowerCase()
  if (ct.includes('pdf')) return 'PDF'
  if (ct.includes('png')) return 'PNG'
  if (ct.includes('jpeg') || ct.includes('jpg')) return 'JPEG'
  if (ct.includes('gif')) return 'GIF'
  if (ct.includes('zip')) return 'ZIP'
  if (ct.includes('csv')) return 'CSV'
  if (ct.includes('json')) return 'JSON'
  if (ct.includes('plain')) return 'Text'
  if (ct.includes('html')) return 'HTML'
  // Extract subtype from MIME e.g. "application/pdf" → "pdf"
  const parts = ct.split('/')
  return parts.length > 1 ? parts[1].toUpperCase().substring(0, 8) : 'File'
}
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

/* ===== Attachment Styles ===== */
.attachment-indicator {
  font-size: 12px;
  margin-left: 6px;
  opacity: 0.7;
}

.attachments-section {
  border-top: 1px solid var(--border);
  padding: 24px 32px;
  background: rgba(255, 255, 255, 0.015);
}

.attachments-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.attachments-icon {
  font-size: 16px;
}

.attachments-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border);
  border-radius: 14px;
  text-decoration: none;
  color: var(--text-main);
  transition: all 0.25s ease;
  cursor: pointer;
  min-width: 220px;
  max-width: 320px;
}

.attachment-card:hover {
  background: rgba(99, 102, 241, 0.12);
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.attachment-icon {
  font-size: 28px;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.attachment-info {
  flex: 1;
  min-width: 0;
}

.attachment-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.download-icon {
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
}

.attachment-card:hover .download-icon {
  opacity: 1;
}
</style>
