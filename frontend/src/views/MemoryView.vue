<template>
  <div class="view">
    <div class="view-header">
      <h2>Memory <span class="gradient">Agent</span></h2>
      <p class="subtitle">Der Agent erinnert sich an frühere Gespräche via Redis. Session-ID bleibt im Browser gespeichert.</p>
      <div class="chips">
        <span class="chip">Redis</span>
        <span class="chip">Session Memory</span>
        <span class="chip">Langzeitgedächtnis</span>
        <span class="chip chip--blue">Session: {{ shortId }}</span>
      </div>
    </div>

    <div class="content">
      <div class="chat-panel">
        <div class="chat-header">
          <div class="chat-title">Gespräch mit Gedächtnis</div>
          <button class="btn-clear" @click="clearHistory" title="Verlauf löschen">Verlauf löschen</button>
        </div>

        <div class="messages" ref="messagesEl">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">&#x1F9E0;</div>
            <p>Der Agent merkt sich alles, was du ihm sagst — auch über mehrere Sessions.</p>
          </div>
          <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
            <div class="message-bubble">{{ msg.text }}</div>
          </div>
          <div v-if="loading" class="message assistant">
            <div class="message-bubble typing"><span></span><span></span><span></span></div>
          </div>
        </div>

        <div v-if="error" class="error-bar">{{ error }}</div>

        <form class="input-row" @submit.prevent="send">
          <input v-model="input" placeholder="Schreib etwas, das er sich merken soll..." :disabled="loading" autocomplete="off" />
          <button type="submit" :disabled="loading || !input.trim()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>

      <div class="panel history-panel">
        <div class="panel-header">Redis Memory Store — rohe Einträge</div>
        <div class="panel-body">
          <div v-if="rawHistory.length === 0" class="empty-docs">Kein Verlauf gespeichert.</div>
          <div v-for="(entry, i) in rawHistory" :key="i" class="history-entry" :class="entry.startsWith('user:') ? 'user-entry' : 'assistant-entry'">
            {{ entry }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'

const SESSION_KEY = 'demo_memory_session_id'

function getOrCreateSessionId() {
  let id = localStorage.getItem(SESSION_KEY)
  if (!id) {
    id = 'session-' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(SESSION_KEY, id)
  }
  return id
}

const sessionId = getOrCreateSessionId()
const shortId = computed(() => sessionId.slice(-8))

const input = ref('')
const messages = ref([])
const loading = ref(false)
const error = ref(null)
const rawHistory = ref([])
const messagesEl = ref(null)

onMounted(async () => {
  await loadHistory()
  // Reconstruct message bubbles from raw history
  for (const entry of rawHistory.value) {
    if (entry.startsWith('user: ')) {
      messages.value.push({ role: 'user', text: entry.slice(6) })
    } else if (entry.startsWith('assistant: ')) {
      messages.value.push({ role: 'assistant', text: entry.slice(11) })
    }
  }
})

async function loadHistory() {
  try {
    const res = await fetch(`/memory/history/${sessionId}`)
    if (res.ok) rawHistory.value = await res.json()
  } catch {}
}

async function send() {
  const text = input.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true
  error.value = null
  await nextTick()
  scrollToBottom()
  try {
    const res = await fetch('/memory/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message: text })
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    messages.value.push({ role: 'assistant', text: data.response })
    rawHistory.value = data.history
  } catch (e) {
    error.value = `Fehler: ${e.message}`
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function clearHistory() {
  await fetch(`/memory/history/${sessionId}`, { method: 'DELETE' })
  messages.value = []
  rawHistory.value = []
}

function scrollToBottom() {
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}
</script>

<style scoped>
.view { flex: 1; padding: 40px 24px; max-width: 900px; margin: 0 auto; width: 100%; }
.view-header { margin-bottom: 32px; }
h2 { font-size: clamp(1.6rem, 4vw, 2.4rem); font-weight: 700; margin-bottom: 8px; }
.gradient {
  background: linear-gradient(135deg, #10b981, #06b6d4);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { color: var(--text-muted); font-size: 1rem; margin-bottom: 16px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}
.chip--blue { border-color: #06b6d4; color: #06b6d4; background: rgba(6,182,212,0.08); }

.content { display: flex; flex-direction: column; gap: 20px; }

.panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden;
}
.panel-header {
  padding: 12px 20px; border-bottom: 1px solid var(--border);
  font-size: 13px; font-weight: 600; color: var(--text-muted);
}
.panel-body { padding: 16px 20px; }

.chat-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  display: flex; flex-direction: column; overflow: hidden;
}
.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; border-bottom: 1px solid var(--border);
}
.chat-title { font-size: 13px; font-weight: 600; color: var(--text-muted); }
.btn-clear {
  background: none; border: 1px solid var(--border); border-radius: 6px;
  padding: 4px 12px; font-size: 12px; color: var(--text-muted); cursor: pointer;
}
.btn-clear:hover { border-color: #ef4444; color: #f87171; }

.messages {
  min-height: 280px; max-height: 380px; overflow-y: auto;
  padding: 20px; display: flex; flex-direction: column; gap: 12px;
}
.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 12px; color: var(--text-muted); font-size: 14px; text-align: center;
}
.empty-icon { font-size: 2.5rem; }
.message { display: flex; }
.message.user { justify-content: flex-end; }
.message-bubble {
  max-width: 75%; padding: 10px 16px; border-radius: 18px;
  font-size: 14px; line-height: 1.5; white-space: pre-wrap; word-break: break-word;
}
.user .message-bubble { background: #10b981; color: white; border-bottom-right-radius: 4px; }
.assistant .message-bubble { background: var(--bg-elevated); color: var(--text); border-bottom-left-radius: 4px; }
.typing { display: flex; align-items: center; gap: 5px; padding: 14px 18px; }
.typing span { width: 7px; height: 7px; border-radius: 50%; background: var(--text-muted); animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-6px); opacity: 1; }
}
.error-bar { background: rgba(239,68,68,0.1); color: #f87171; padding: 10px 20px; font-size: 13px; }
.input-row { display: flex; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--border); }
.input-row input {
  flex: 1; background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 8px; padding: 10px 14px; font-size: 14px; color: var(--text); font-family: inherit; outline: none;
}
.input-row input:focus { border-color: #10b981; }
.input-row button {
  background: #10b981; border: none; border-radius: 8px; padding: 10px 16px;
  color: white; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.input-row button:disabled { opacity: 0.4; cursor: not-allowed; }
.input-row button svg { width: 18px; height: 18px; }

.history-panel { max-height: 280px; overflow: hidden; }
.empty-docs { font-size: 13px; color: var(--text-muted); text-align: center; padding: 20px 0; }
.history-entry {
  font-size: 12px; padding: 5px 8px; border-radius: 4px;
  margin-bottom: 4px; font-family: monospace; line-height: 1.4;
}
.user-entry { background: rgba(16,185,129,0.08); color: #10b981; }
.assistant-entry { background: var(--bg-elevated); color: var(--text-muted); }
</style>
