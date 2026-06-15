import { defineStore } from 'pinia'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client } from '@stomp/stompjs'

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
      // Create a native SockJS client since Vite might struggle with the bare import in some setups
      const socket = new SockJS('http://localhost:8080/ws-email')
      
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
              newEmail.id = Date.now()
              newEmail.read = false
              newEmail.timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
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
    
    selectEmail(email) {
      this.selectedEmail = email
      email.read = true
    }
  }
})
