import { defineStore } from 'pinia'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client } from '@stomp/stompjs'

const BACKEND_URL = 'http://localhost:8080'

export const useEmailStore = defineStore('emails', {
  state: () => ({
    emails: [],
    selectedEmail: null,
    stompClient: null,
    connected: false
  }),
  
  getters: {
    unreadCount: (state) => state.emails.filter(e => !e.read).length
  },
  
  actions: {
    connectWebSocket() {
      const socket = new SockJS(`${BACKEND_URL}/ws-email`)
      
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        debug: (str) => console.log('STOMP: ', str),
        onConnect: () => {
          this.connected = true
          console.log('✅ Connected to WebSocket')
          
          this.stompClient.subscribe('/topic/emails', (message) => {
            if (message.body) {
              const newEmail = JSON.parse(message.body)

              // Use server-generated ID if present, otherwise fallback
              if (!newEmail.id) {
                newEmail.id = Date.now().toString()
              }

              // Use server timestamp if present, otherwise generate
              if (!newEmail.timestamp) {
                newEmail.timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
              }

              newEmail.read = false

              // Build download URLs for any attachments
              if (newEmail.attachments && newEmail.attachments.length > 0) {
                newEmail.attachments = newEmail.attachments.map(att => ({
                  ...att,
                  downloadUrl: `${BACKEND_URL}/api/attachments/${att.id}/${encodeURIComponent(att.filename)}`,
                  displaySize: formatFileSize(att.size)
                }))
              } else {
                newEmail.attachments = []
              }

              this.emails.unshift(newEmail)
            }
          })
        },
        onStompError: (frame) => {
          console.error('Broker reported error: ' + frame.headers['message'])
          console.error('Additional details: ' + frame.body)
        }
      })
      
      this.stompClient.activate()
    },
    
    async fetchHistoricalEmails() {
      try {
        const response = await fetch(`${BACKEND_URL}/api/emails`)
        if (response.ok) {
          const historicalData = await response.json()
          
          // Map backend entities to frontend structure
          this.emails = historicalData.map(dbEmail => {
            const parsedAttachments = dbEmail.attachmentsJson && dbEmail.attachmentsJson !== 'null' 
              ? JSON.parse(dbEmail.attachmentsJson) 
              : []
            
            return {
              id: dbEmail.id,
              from: dbEmail.sender,
              to: dbEmail.recipient,
              subject: dbEmail.subject,
              body: dbEmail.body,
              timestamp: dbEmail.timestamp,
              read: true, // Mark historical emails as read by default
              attachments: parsedAttachments.map(att => ({
                ...att,
                downloadUrl: `${BACKEND_URL}/api/attachments/${dbEmail.id}/${encodeURIComponent(att.filename)}`,
                displaySize: formatFileSize(att.size)
              }))
            }
          })
          console.log(`✅ Loaded ${this.emails.length} historical emails from Cassandra`)
        }
      } catch (error) {
        console.error('❌ Failed to fetch historical emails:', error)
      }
    },
    
    selectEmail(email) {
      this.selectedEmail = email
      email.read = true
    }
  }
})

/**
 * Convert bytes to a human-readable file size string
 */
function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  const size = (bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)
  return `${size} ${units[i]}`
}
