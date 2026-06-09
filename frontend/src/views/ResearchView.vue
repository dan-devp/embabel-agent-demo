<template>
  <div class="view">
    <div class="view-header">
      <h2>Deep <span class="gradient">Research</span></h2>
      <p class="subtitle">Mehrschrittige autonome Analyse — der Agent zerlegt Fragen, durchsucht Dokumente und synthetisiert Berichte.</p>
      <div class="chips">
        <span class="chip">Multi-Step Agent</span>
        <span class="chip">Embabel</span>
        <span class="chip">RAG + Synthesis</span>
        <span class="chip">Live SSE</span>
      </div>
    </div>

    <div class="content">
      <div class="panel">
        <div class="panel-header">Forschungsauftrag</div>
        <div class="panel-body">
          <div class="example-queries">
            <button v-for="q in exampleTopics" :key="q" class="example-chip" @click="topic = q">
              {{ q }}
            </button>
          </div>
          <form @submit.prevent="startResearch" class="query-form">
            <textarea v-model="topic" placeholder="Welches Thema soll tief analysiert werden?" rows="3" :disabled="loading" />
            <button type="submit" :disabled="loading || !topic.trim()" class="btn-primary">
              <span v-if="loading" class="spinner"></span>
              {{ loading ? 'Analysiere...' : 'Deep Research starten' }}
            </button>
          </form>
          <div v-if="error" class="error-bar">{{ error }}</div>
        </div>
      </div>

      <!-- Live Step Progress (nur während Analyse) -->
      <div v-if="loading || steps.length > 0" class="steps-panel">
        <div class="steps-title">Agent-Fortschritt</div>
        <div
          v-for="(s, i) in allSteps"
          :key="i"
          class="step"
          :class="stepClass(i)"
        >
          <span class="step-dot"></span>
          <span class="step-label">{{ s.label }}</span>
          <span v-if="isActiveStep(i)" class="step-badge">läuft...</span>
          <span v-else-if="isDoneStep(i)" class="step-badge step-badge--done">fertig</span>
        </div>
      </div>

      <div v-if="report" class="report-panel">
        <div class="report-header">
          <span class="report-icon">&#x1F4CB;</span>
          <div>
            <div class="report-title">Forschungsbericht</div>
            <div class="report-topic">{{ report.topic }}</div>
          </div>
        </div>

        <div class="report-section">
          <div class="section-label">Zusammenfassung</div>
          <div class="summary-text">{{ report.summary }}</div>
        </div>

        <div class="report-section">
          <div class="section-label">Wichtigste Erkenntnisse</div>
          <ul class="findings-list">
            <li v-for="(finding, i) in report.keyFindings" :key="i">{{ finding }}</li>
          </ul>
        </div>

        <div class="report-section" v-if="report.sources && report.sources.length > 0">
          <div class="section-label">Quellen</div>
          <div class="sources">
            <span v-for="src in report.sources" :key="src" class="source-chip">{{ src }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const topic = ref('')
const report = ref(null)
const loading = ref(false)
const error = ref(null)
const steps = ref([])  // { step, label } events received so far

const STEP_LABELS = [
  'Frage zerlegen in Teilfragen',
  'Wissensbasis durchsuchen',
  'Bericht synthetisieren',
]

const allSteps = computed(() => STEP_LABELS.map((label, i) => ({ step: i + 1, label })))

function stepClass(i) {
  const stepNum = i + 1
  const maxDone = steps.value.length > 0 ? Math.max(...steps.value.map(s => s.step)) : 0
  if (stepNum < maxDone) return 'done'
  if (stepNum === maxDone) return loading.value ? 'active' : 'done'
  return ''
}

function isActiveStep(i) {
  if (!loading.value) return false
  const stepNum = i + 1
  const maxDone = steps.value.length > 0 ? Math.max(...steps.value.map(s => s.step)) : 0
  return stepNum === maxDone
}

function isDoneStep(i) {
  const stepNum = i + 1
  const maxDone = steps.value.length > 0 ? Math.max(...steps.value.map(s => s.step)) : 0
  return stepNum < maxDone || (!loading.value && stepNum <= maxDone)
}

const exampleTopics = [
  'Preisstruktur und Lizenzmodelle',
  'Datenschutz und Compliance',
  'Unternehmenskultur und Benefits',
  'Produktportfolio Übersicht',
]

let currentSource = null

async function startResearch() {
  if (!topic.value.trim()) return
  loading.value = true
  error.value = null
  report.value = null
  steps.value = []

  if (currentSource) {
    currentSource.close()
    currentSource = null
  }

  try {
    // 1. Start the job — backend returns jobId immediately
    const res = await fetch('/research/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ topic: topic.value })
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const { jobId } = await res.json()

    // 2. Open SSE stream for real-time step updates
    currentSource = new EventSource(`/research/stream/${jobId}`)

    currentSource.onmessage = (e) => {
      const data = JSON.parse(e.data)

      if (data.type === 'step') {
        steps.value.push({ step: data.step, label: data.label })
      } else if (data.type === 'done') {
        report.value = data.report
        loading.value = false
        currentSource.close()
        currentSource = null
      } else if (data.type === 'error') {
        error.value = `Agent-Fehler: ${data.message}`
        loading.value = false
        currentSource.close()
        currentSource = null
      }
    }

    currentSource.onerror = () => {
      if (loading.value) {
        error.value = 'Verbindung zum Server unterbrochen.'
        loading.value = false
      }
      currentSource?.close()
      currentSource = null
    }
  } catch (e) {
    error.value = `Fehler: ${e.message}`
    loading.value = false
  }
}
</script>

<style scoped>
.view { flex: 1; padding: 40px 24px; max-width: 900px; margin: 0 auto; width: 100%; }
.view-header { margin-bottom: 32px; }
h2 { font-size: clamp(1.6rem, 4vw, 2.4rem); font-weight: 700; margin-bottom: 8px; }
.gradient {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { color: var(--text-muted); font-size: 1rem; margin-bottom: 16px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}

.content { display: flex; flex-direction: column; gap: 20px; }

.panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden;
}
.panel-header {
  padding: 12px 20px; border-bottom: 1px solid var(--border);
  font-size: 13px; font-weight: 600; color: var(--text-muted);
}
.panel-body { padding: 20px; }

.example-queries { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.example-chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 5px 12px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
  cursor: pointer; transition: all 0.15s;
}
.example-chip:hover { border-color: #f59e0b; color: #f59e0b; }

.query-form { display: flex; flex-direction: column; gap: 12px; }
.query-form textarea {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 8px; padding: 12px; font-size: 14px; color: var(--text);
  font-family: inherit; resize: vertical; outline: none;
}
.query-form textarea:focus { border-color: #f59e0b; }

.btn-primary {
  background: linear-gradient(135deg, #f59e0b, #ef4444); border: none; border-radius: 8px;
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

.steps-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  padding: 20px; display: flex; flex-direction: column; gap: 14px;
}
.steps-title { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 4px; }

.step {
  display: flex; align-items: center; gap: 12px;
  font-size: 13px; color: var(--text-muted); opacity: 0.35; transition: all 0.4s;
}
.step.active { opacity: 1; color: var(--text); }
.step.done { opacity: 0.8; color: var(--success); }

.step-dot {
  width: 9px; height: 9px; border-radius: 50%; background: var(--border); flex-shrink: 0; transition: all 0.4s;
}
.step.active .step-dot { background: #f59e0b; box-shadow: 0 0 10px #f59e0b; animation: pulse 1s infinite; }
.step.done .step-dot { background: var(--success); box-shadow: none; }

.step-label { flex: 1; }

.step-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 99px;
  background: rgba(245,158,11,0.15); color: #f59e0b;
}
.step-badge--done { background: rgba(16,185,129,0.12); color: var(--success); }

@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.report-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden;
}
.report-header {
  display: flex; align-items: center; gap: 16px; padding: 20px;
  border-bottom: 1px solid var(--border); background: rgba(245,158,11,0.06);
}
.report-icon { font-size: 2rem; }
.report-title { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 4px; }
.report-topic { font-size: 16px; font-weight: 600; color: var(--text); }

.report-section { padding: 20px; border-bottom: 1px solid var(--border); }
.report-section:last-child { border-bottom: none; }
.section-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 12px; }

.summary-text { font-size: 14px; line-height: 1.7; color: var(--text); white-space: pre-wrap; }

.findings-list { margin: 0; padding-left: 20px; display: flex; flex-direction: column; gap: 8px; }
.findings-list li { font-size: 14px; line-height: 1.6; color: var(--text); }

.sources { display: flex; gap: 8px; flex-wrap: wrap; }
.source-chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}
</style>
