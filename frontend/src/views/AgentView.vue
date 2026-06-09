<template>
  <div class="layout">
    <header class="hero">
      <div class="hero-content">
        <div class="badge">Powered by Embabel</div>
        <h1>AI Agent <span class="gradient">Chat</span></h1>
        <p class="subtitle">Spring Boot 3.5 + Embabel — direkter Agenten-Test.</p>
        <div class="hero-meta">
          <span class="meta-chip">GPT-4.1 Mini</span>
          <span class="meta-chip">Java 21</span>
          <span class="meta-chip">Embabel 0.4.0</span>
        </div>
        <div class="server-status" :class="serverStatus">
          <span class="status-indicator"></span>
          <span>{{ serverMessage }}</span>
        </div>
      </div>
      <div class="hero-visual">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
      </div>
    </header>

    <main class="chat-section">
      <div class="chat-card">
        <div class="chat-header">
          <div class="status-dot" :class="{ active: serverStatus === 'ok' }"></div>
          <span>Agent Chat</span>
        </div>
        <div class="messages" ref="messagesEl">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">&#x1F916;</div>
            <p>Schreib eine Nachricht um den Agenten zu starten.</p>
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
          <input v-model="input" placeholder="Nachricht eingeben..." :disabled="loading" autocomplete="off" />
          <button type="submit" :disabled="loading || !input.trim()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>
    </main>

    <section class="features">
      <div class="feature-card">
        <div class="feature-icon">&#x26A1;</div>
        <h3>Embabel Agents</h3>
        <p>Deklaratives Agenten-Framework für Spring Boot.</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">&#x1F4AC;</div>
        <h3>REST API</h3>
        <p>POST /chat — direkte Agenteninvokation via HTTP.</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">&#x1F4CA;</div>
        <h3>Observability</h3>
        <p>Tracing und Logging. Vollständige Sichtbarkeit.</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'

const input = ref('')
const messages = ref([])
const loading = ref(false)
const error = ref(null)
const messagesEl = ref(null)
const serverStatus = ref('pending')
const serverMessage = ref('Backend wird geprüft...')

onMounted(async () => {
  try {
    const res = await fetch('/chat')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    serverStatus.value = 'ok'
    serverMessage.value = await res.text()
  } catch (e) {
    serverStatus.value = 'error'
    serverMessage.value = 'Backend nicht erreichbar'
  }
})

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
    const res = await fetch('/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text })
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    messages.value.push({ role: 'assistant', text: await res.text() })
  } catch (e) {
    error.value = `Fehler: ${e.message}`
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function scrollToBottom() {
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}
</script>

<style scoped>
.layout { display: flex; flex-direction: column; gap: 0; flex: 1; }

.hero {
  position: relative;
  padding: 80px 24px 60px;
  text-align: center;
  overflow: hidden;
}
.hero-content { position: relative; z-index: 1; max-width: 640px; margin: 0 auto; }
.badge {
  display: inline-block; padding: 4px 14px; border-radius: 99px;
  border: 1px solid var(--primary); color: var(--primary-light);
  font-size: 12px; font-weight: 500; letter-spacing: 0.05em;
  text-transform: uppercase; margin-bottom: 20px;
}
h1 { font-size: clamp(2rem, 5vw, 3.5rem); font-weight: 700; line-height: 1.15; margin-bottom: 16px; }
.gradient {
  background: linear-gradient(135deg, var(--primary-light), var(--primary));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { color: var(--text-muted); font-size: 1.05rem; margin-bottom: 24px; }
.hero-meta { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.meta-chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 4px 12px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}
.server-status {
  display: inline-flex; align-items: center; gap: 8px; margin-top: 20px;
  padding: 8px 18px; border-radius: 99px; font-size: 13px; font-weight: 500;
  border: 1px solid var(--border); background: var(--bg-card); color: var(--text-muted);
}
.server-status.ok { border-color: var(--success); color: var(--success); background: rgba(16,185,129,0.08); }
.server-status.error { border-color: #ef4444; color: #f87171; background: rgba(239,68,68,0.08); }
.server-status .status-indicator { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
.orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.15; pointer-events: none; }
.orb-1 { width: 400px; height: 400px; background: var(--primary); top: -100px; left: 50%; transform: translateX(-50%); }
.orb-2 { width: 200px; height: 200px; background: #ec4899; bottom: 0; left: 10%; }
.orb-3 { width: 250px; height: 250px; background: #06b6d4; bottom: 0; right: 10%; }

.chat-section { padding: 0 24px 48px; flex: 1; display: flex; justify-content: center; }
.chat-card {
  width: 100%; max-width: 680px; background: var(--bg-card);
  border: 1px solid var(--border); border-radius: var(--radius);
  display: flex; flex-direction: column; overflow: hidden;
}
.chat-header {
  display: flex; align-items: center; gap: 8px; padding: 14px 20px;
  border-bottom: 1px solid var(--border); font-size: 14px; font-weight: 500; color: var(--text-muted);
}
.status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--border); }
.status-dot.active { background: var(--success); box-shadow: 0 0 6px var(--success); }
.messages {
  flex: 1; min-height: 320px; max-height: 420px; overflow-y: auto;
  padding: 20px; display: flex; flex-direction: column; gap: 12px;
}
.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 12px; color: var(--text-muted); font-size: 14px;
}
.empty-icon { font-size: 2.5rem; }
.message { display: flex; }
.message.user { justify-content: flex-end; }
.message-bubble {
  max-width: 75%; padding: 10px 16px; border-radius: 18px;
  font-size: 14px; line-height: 1.5; white-space: pre-wrap; word-break: break-word;
}
.user .message-bubble { background: var(--primary); color: white; border-bottom-right-radius: 4px; }
.assistant .message-bubble { background: var(--bg-elevated); color: var(--text); border-bottom-left-radius: 4px; }
.typing { display: flex; align-items: center; gap: 5px; padding: 14px 18px; }
.typing span { width: 7px; height: 7px; border-radius: 50%; background: var(--text-muted); animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-6px); opacity: 1; }
}
.error-bar { background: rgba(239,68,68,0.1); border-top: 1px solid rgba(239,68,68,0.3); color: #f87171; font-size: 13px; padding: 10px 20px; }
.input-row { display: flex; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--border); }
.input-row input {
  flex: 1; background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 8px; padding: 10px 14px; font-size: 14px; color: var(--text); font-family: inherit; outline: none;
}
.input-row input:focus { border-color: var(--primary); }
.input-row input::placeholder { color: var(--text-muted); }
.input-row button {
  background: var(--primary); border: none; border-radius: 8px; padding: 10px 16px;
  color: white; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.input-row button:hover:not(:disabled) { background: var(--primary-dark); }
.input-row button:disabled { opacity: 0.4; cursor: not-allowed; }
.input-row button svg { width: 18px; height: 18px; }

.features {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px; padding: 0 24px 48px; max-width: 1000px; margin: 0 auto; width: 100%;
}
.feature-card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 24px; text-align: center;
}
.feature-card:hover { border-color: var(--primary); }
.feature-icon { font-size: 2rem; margin-bottom: 12px; }
.feature-card h3 { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.feature-card p { font-size: 13px; color: var(--text-muted); line-height: 1.5; }
</style>
