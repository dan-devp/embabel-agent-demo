<template>
  <div class="view">
    <div class="view-header">
      <h2>Knowledge <span class="gradient">RAG</span></h2>
      <p class="subtitle">Fragen an das Unternehmenswissen der Quaddel GmbH. Antworten aus indizierten Dokumenten.</p>
      <div class="chips">
        <span class="chip">Vector Search</span>
        <span class="chip">Spring AI</span>
        <span class="chip">In-Memory Store</span>
        <span class="chip chip--green" v-if="documents.length > 0">{{ documents.length }} Dokumente indexiert</span>
      </div>
    </div>

    <div class="content">
      <!-- Query Panel -->
      <div class="panel">
        <div class="panel-header">Wissensdatenbank abfragen</div>
        <div class="panel-body">
          <div class="example-queries">
            <button v-for="q in exampleQueries" :key="q" class="example-chip" @click="question = q">
              {{ q }}
            </button>
          </div>
          <form @submit.prevent="query" class="query-form">
            <textarea v-model="question" placeholder="Stell eine Frage zum Unternehmenswissen..." rows="3" :disabled="loading" />
            <button type="submit" :disabled="loading || !question.trim()" class="btn-primary">
              <span v-if="loading" class="spinner"></span>
              {{ loading ? 'Suche...' : 'Fragen' }}
            </button>
          </form>
          <div v-if="error" class="error-bar">{{ error }}</div>
        </div>
      </div>

      <!-- Answer Panel -->
      <div v-if="answer" class="panel answer-panel">
        <div class="panel-header">
          Antwort
          <span class="badge-small">{{ answer.documentsFound }} Dokumente gefunden</span>
        </div>
        <div class="panel-body">
          <div class="answer-text">{{ answer.answer }}</div>
          <div v-if="answer.sources.length > 0" class="sources">
            <span class="sources-label">Quellen:</span>
            <span v-for="src in answer.sources" :key="src" class="source-chip">{{ src }}</span>
          </div>
        </div>
      </div>

      <!-- Documents + Upload -->
      <div class="two-col">
        <div class="panel">
          <div class="panel-header">Dokumente hochladen</div>
          <div class="panel-body">
            <div class="upload-zone" @dragover.prevent @drop.prevent="onDrop" @click="fileInput.click()">
              <div class="upload-icon">&#x1F4C4;</div>
              <p>TXT oder MD-Datei hierher ziehen oder klicken</p>
              <input ref="fileInput" type="file" accept=".txt,.md" style="display:none" @change="onFileSelect" />
            </div>
            <div v-if="uploadStatus" class="upload-status" :class="uploadStatus.type">{{ uploadStatus.msg }}</div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">Indexierte Dokumente</div>
          <div class="panel-body">
            <div v-if="documents.length === 0" class="empty-docs">Noch keine Dokumente geladen.</div>
            <div v-for="doc in documents" :key="doc.name" class="doc-item">
              <span class="doc-icon">&#x1F4DD;</span>
              <span class="doc-name">{{ doc.name }}</span>
              <span class="doc-type">{{ doc.type }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const question = ref('')
const answer = ref(null)
const loading = ref(false)
const error = ref(null)
const documents = ref([])
const fileInput = ref(null)
const uploadStatus = ref(null)

const exampleQueries = [
  'Was kostet QuaddelInsight für 10 User?',
  'Wie viele Urlaubstage haben Mitarbeiter?',
  'Ist das Produkt DSGVO-konform?',
  'Welche Zertifizierungen hat das Unternehmen?',
]

onMounted(loadDocuments)

async function loadDocuments() {
  try {
    const res = await fetch('/knowledge/documents')
    if (res.ok) documents.value = await res.json()
  } catch {}
}

async function query() {
  if (!question.value.trim()) return
  loading.value = true
  error.value = null
  answer.value = null
  try {
    const res = await fetch('/knowledge/query', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: question.value })
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    answer.value = await res.json()
  } catch (e) {
    error.value = `Fehler: ${e.message}`
  } finally {
    loading.value = false
  }
}

async function uploadFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  uploadStatus.value = { type: 'info', msg: `Lade ${file.name} hoch...` }
  try {
    const res = await fetch('/knowledge/upload', { method: 'POST', body: fd })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    uploadStatus.value = { type: 'success', msg: `${file.name} indexiert.` }
    await loadDocuments()
  } catch (e) {
    uploadStatus.value = { type: 'error', msg: `Fehler: ${e.message}` }
  }
}

function onFileSelect(e) { if (e.target.files[0]) uploadFile(e.target.files[0]) }
function onDrop(e) { if (e.dataTransfer.files[0]) uploadFile(e.dataTransfer.files[0]) }
</script>

<style scoped>
.view { flex: 1; padding: 40px 24px; max-width: 900px; margin: 0 auto; width: 100%; }
.view-header { margin-bottom: 32px; }
h2 { font-size: clamp(1.6rem, 4vw, 2.4rem); font-weight: 700; margin-bottom: 8px; }
.gradient {
  background: linear-gradient(135deg, var(--primary-light), var(--primary));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { color: var(--text-muted); font-size: 1rem; margin-bottom: 16px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}
.chip--green { border-color: var(--success); color: var(--success); background: rgba(16,185,129,0.08); }

.content { display: flex; flex-direction: column; gap: 20px; }

.panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden;
}
.panel-header {
  padding: 12px 20px; border-bottom: 1px solid var(--border);
  font-size: 13px; font-weight: 600; color: var(--text-muted);
  display: flex; align-items: center; gap: 10px;
}
.badge-small {
  background: rgba(99,102,241,0.12); color: var(--primary-light);
  padding: 2px 8px; border-radius: 99px; font-size: 11px;
}
.panel-body { padding: 20px; }

.example-queries { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.example-chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 5px 12px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
  cursor: pointer; transition: all 0.15s;
}
.example-chip:hover { border-color: var(--primary); color: var(--primary-light); }

.query-form { display: flex; flex-direction: column; gap: 12px; }
.query-form textarea {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 8px; padding: 12px; font-size: 14px; color: var(--text);
  font-family: inherit; resize: vertical; outline: none;
}
.query-form textarea:focus { border-color: var(--primary); }

.btn-primary {
  background: var(--primary); border: none; border-radius: 8px;
  padding: 10px 24px; color: white; font-size: 14px; font-weight: 500;
  cursor: pointer; display: flex; align-items: center; gap: 8px; align-self: flex-end;
}
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }
.spinner {
  width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-bar { background: rgba(239,68,68,0.1); color: #f87171; padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-top: 8px; }

.answer-text { font-size: 14px; line-height: 1.7; white-space: pre-wrap; color: var(--text); margin-bottom: 16px; }
.sources { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.sources-label { font-size: 12px; color: var(--text-muted); }
.source-chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 2px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
@media (max-width: 600px) { .two-col { grid-template-columns: 1fr; } }

.upload-zone {
  border: 2px dashed var(--border); border-radius: 8px; padding: 32px;
  text-align: center; cursor: pointer; transition: border-color 0.2s;
}
.upload-zone:hover { border-color: var(--primary); }
.upload-icon { font-size: 2rem; margin-bottom: 8px; }
.upload-zone p { font-size: 13px; color: var(--text-muted); }
.upload-status { margin-top: 10px; font-size: 13px; padding: 8px 12px; border-radius: 6px; }
.upload-status.success { background: rgba(16,185,129,0.1); color: var(--success); }
.upload-status.error { background: rgba(239,68,68,0.1); color: #f87171; }
.upload-status.info { background: rgba(99,102,241,0.1); color: var(--primary-light); }

.empty-docs { font-size: 13px; color: var(--text-muted); text-align: center; padding: 20px 0; }
.doc-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 0;
  border-bottom: 1px solid var(--border); font-size: 13px;
}
.doc-item:last-child { border-bottom: none; }
.doc-icon { font-size: 1.1rem; }
.doc-name { flex: 1; color: var(--text); }
.doc-type { font-size: 11px; color: var(--text-muted); }
</style>
