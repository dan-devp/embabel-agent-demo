<template>
  <div class="view">
    <div class="view-header">
      <h2>Multi-Agent <span class="gradient">Collaboration</span></h2>
      <p class="subtitle">6 spezialisierte Agenten arbeiten zusammen — GOAP-Orchestrierung, Feedback-Loop, Human-in-the-Loop.</p>
      <div class="chips">
        <span class="chip">Multi-Agent System</span>
        <span class="chip">GOAP Orchestration</span>
        <span class="chip">Feedback Loop</span>
        <span class="chip">Human-in-the-Loop</span>
        <span class="chip">Live SSE</span>
      </div>
    </div>

    <!-- Start Form -->
    <div v-if="!jobId" class="panel">
      <div class="panel-header">Produkt-Launch-Analyse starten</div>
      <div class="panel-body">
        <div class="example-grid">
          <button v-for="ex in examples" :key="ex.name" class="example-card" @click="applyExample(ex)">
            <span class="example-title">{{ ex.name }}</span>
            <span class="example-market">{{ ex.market }}</span>
          </button>
        </div>
        <form @submit.prevent="startDemo" class="start-form">
          <div class="form-row">
            <div class="form-field">
              <label>Produktname</label>
              <input v-model="form.productName" placeholder="z.B. DataVault Pro" :disabled="loading" />
            </div>
            <div class="form-field">
              <label>Zielmarkt</label>
              <input v-model="form.targetMarket" placeholder="z.B. DACH-Region, Mittelstand" :disabled="loading" />
            </div>
          </div>
          <div class="form-field">
            <label>Beschreibung</label>
            <textarea v-model="form.description" rows="2"
              placeholder="Kurze Beschreibung des Produkts und seiner Kernfunktion" :disabled="loading" />
          </div>
          <button type="submit" :disabled="loading || !isFormValid" class="btn-primary">
            Analyse starten
          </button>
        </form>
      </div>
    </div>

    <!-- Main Demo Layout -->
    <div v-if="jobId" class="demo-layout">
      <!-- Approval Banner -->
      <div v-if="waitingForApproval" class="approval-banner">
        <div class="approval-content">
          <div class="approval-icon">👤</div>
          <div class="approval-text">
            <div class="approval-title">Executive-Entscheidung erforderlich</div>
            <div class="approval-summary">{{ approvalSummary }}</div>
          </div>
        </div>
        <div class="approval-actions">
          <button class="btn-approve" @click="submitApproval(true)">Genehmigen</button>
          <button class="btn-reject" @click="submitApproval(false)">Ablehnen</button>
        </div>
      </div>

      <div class="panels">
        <!-- Log Panel -->
        <div class="log-panel">
          <div class="panel-header">Agent-Log</div>
          <div class="log-body" ref="logBody">
            <div
              v-for="(step, i) in steps"
              :key="i"
              class="log-entry"
              :class="'log-' + step.status.toLowerCase()"
            >
              <span class="log-icon">{{ statusIcon(step.status) }}</span>
              <div class="log-content">
                <div class="log-name">{{ step.stepName }}</div>
                <div v-if="step.message" class="log-message">{{ step.message }}</div>
              </div>
              <span class="log-time">{{ formatTime(step.timestamp) }}</span>
            </div>
            <div v-if="loading && steps.length === 0" class="log-placeholder">
              <span class="spinner"></span> Agenten werden gestartet...
            </div>
          </div>
        </div>

        <!-- Diagram Panel -->
        <div class="diagram-panel">
          <div class="panel-header">
            Flow-Diagramm
            <span class="diagram-legend">
              <span class="legend-dot running"></span>läuft
              <span class="legend-dot done"></span>fertig
              <span class="legend-dot feedback"></span>Feedback
              <span class="legend-dot waiting"></span>wartet
            </span>
          </div>
          <div class="diagram-body">
            <div ref="diagramEl" class="mermaid-container"></div>
          </div>
        </div>
      </div>

      <!-- Final Report -->
      <div v-if="report" class="report-panel">
        <div class="report-header">
          <span class="report-icon">📋</span>
          <div>
            <div class="report-label">Executive Summary Report</div>
            <div class="report-title">{{ report.productName }}</div>
          </div>
          <span class="decision-badge" :class="report.decision.startsWith('GO') ? 'go' : 'nogo'">
            {{ report.decision.startsWith('GO') ? 'GO' : 'NO-GO' }}
          </span>
        </div>

        <div class="report-grid">
          <div class="report-section">
            <div class="section-label">Executive Summary</div>
            <p>{{ report.executiveSummary }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Marktkontext</div>
            <p>{{ report.marketContext }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Finanzielle Highlights</div>
            <p>{{ report.financialHighlights }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Entscheidung</div>
            <p>{{ report.decision }}</p>
          </div>
        </div>

        <div class="report-section report-steps">
          <div class="section-label">Nächste Schritte</div>
          <ol>
            <li v-for="(step, i) in report.nextSteps" :key="i">{{ step }}</li>
          </ol>
        </div>

        <div class="report-footer">
          <button class="btn-secondary" @click="reset">Neue Analyse starten</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import mermaid from 'mermaid'

mermaid.initialize({
  startOnLoad: false,
  theme: 'dark',
  themeVariables: {
    darkMode: true,
    background: '#0f172a',
    primaryColor: '#1e3a5f',
    primaryTextColor: '#e2e8f0',
    lineColor: '#475569',
    nodeBorder: '#334155',
  },
  flowchart: { curve: 'basis', htmlLabels: false },
})

let renderCounter = 0

const form = ref({ productName: '', targetMarket: '', description: '' })
const jobId = ref(null)
const loading = ref(false)
const steps = ref([])
const report = ref(null)
const waitingForApproval = ref(false)
const approvalSummary = ref('')
const diagramEl = ref(null)
const logBody = ref(null)

const nodeStatuses = ref({
  analyze_request: 'pending',
  market_research: 'pending',
  customer_insights: 'pending',
  financial_model: 'pending',
  validator: 'pending',
  risk_assessment: 'pending',
  approval_gate: 'pending',
  recommendation: 'pending',
  final_report: 'pending',
})

const examples = [
  { name: 'DataVault Pro', market: 'DACH-Mittelstand', description: 'Sichere, DSGVO-konforme Cloud-Datenverwaltung für KMUs mit automatischer Compliance-Prüfung.' },
  { name: 'FleetIQ', market: 'Logistik EU', description: 'KI-gestützte Flottenoptimierung für Logistikunternehmen — Routenplanung, Verbrauchsanalyse, Wartungsvorhersage.' },
  { name: 'HealthDesk', market: 'DACH Kliniken', description: 'Digitale Patientenkommunikation und Terminverwaltung für Krankenhäuser und MVZs.' },
]

let currentSource = null

const isFormValid = computed(() => form.value.productName.trim() && form.value.targetMarket.trim() && form.value.description.trim())

function applyExample(ex) {
  form.value.productName = ex.name
  form.value.targetMarket = ex.market
  form.value.description = ex.description
}

function statusIcon(status) {
  const icons = { RUNNING: '⟳', DONE: '✓', FEEDBACK: '↩', WAITING: '⏳', REJECTED: '✗', ERROR: '!' }
  return icons[status] || '·'
}

function formatTime(ts) {
  return new Date(ts).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function nodeClass(stepId) {
  const s = nodeStatuses.value[stepId]
  if (!s || s === 'pending') return 'pending'
  const map = { RUNNING: 'running', DONE: 'done', FEEDBACK: 'feedback', WAITING: 'waiting', REJECTED: 'rejected', ERROR: 'rejected' }
  return map[s] || 'pending'
}

function buildDiagram() {
  const c = (id) => nodeClass(id)
  return `flowchart TD
  n0([Start])
  n0 --> n1[Anfrage analysieren]
  n1 --> n2[Marktrecherche]
  n2 --> n3[Kundenanalyse]
  n3 --> n4[Finanzmodell]
  n4 --> n5[Validator]
  n5 -->|Feedback| n4
  n5 --> n6[Risikoanalyse]
  n6 --> n7{Executive Approval}
  n7 -->|Genehmigt| n8[Empfehlung]
  n7 -->|Abgelehnt| n9x([Abgebrochen])
  n8 --> n9[Abschlussbericht]
  n9 --> n10([Ende])

  classDef pending fill:#1e293b,stroke:#334155,color:#94a3b8
  classDef running fill:#1d3461,stroke:#3b82f6,color:#fff,stroke-width:2px
  classDef done fill:#14532d,stroke:#22c55e,color:#fff
  classDef feedback fill:#431407,stroke:#f97316,color:#fff,stroke-width:2px
  classDef waiting fill:#451a03,stroke:#f59e0b,color:#fff,stroke-width:2px
  classDef rejected fill:#450a0a,stroke:#ef4444,color:#fff
  classDef terminal fill:#0f172a,stroke:#818cf8,color:#818cf8,stroke-width:2px

  class n0 terminal
  class n10 terminal
  class n9x rejected
  class n1 ${c('analyze_request')}
  class n2 ${c('market_research')}
  class n3 ${c('customer_insights')}
  class n4 ${c('financial_model')}
  class n5 ${c('validator')}
  class n6 ${c('risk_assessment')}
  class n7 ${c('approval_gate')}
  class n8 ${c('recommendation')}
  class n9 ${c('final_report')}`
}

async function renderDiagram() {
  if (!diagramEl.value) return
  try {
    const id = 'collab-diagram-' + (++renderCounter)
    const { svg } = await mermaid.render(id, buildDiagram())
    diagramEl.value.innerHTML = svg
  } catch (e) {
    console.warn('Mermaid render error:', e)
  }
}

watch(nodeStatuses, async () => {
  await nextTick()
  renderDiagram()
}, { deep: true })

const STEP_NODE_MAP = {
  analyze_request: 'analyze_request',
  market_research: 'market_research',
  customer_insights: 'customer_insights',
  financial_model: 'financial_model',
  validator: 'validator',
  risk_assessment: 'risk_assessment',
  approval_gate: 'approval_gate',
  recommendation: 'recommendation',
  final_report: 'final_report',
}

function handleStepEvent(data) {
  steps.value.push(data)
  nextTick(() => {
    if (logBody.value) logBody.value.scrollTop = logBody.value.scrollHeight
  })

  const nodeId = STEP_NODE_MAP[data.stepId]
  if (nodeId) {
    nodeStatuses.value = { ...nodeStatuses.value, [nodeId]: data.status }
  }

  if (data.status === 'WAITING') {
    waitingForApproval.value = true
    approvalSummary.value = data.message || ''
  }
}

async function startDemo() {
  if (!isFormValid.value) return
  loading.value = true
  steps.value = []
  report.value = null
  waitingForApproval.value = false

  Object.keys(nodeStatuses.value).forEach(k => nodeStatuses.value[k] = 'pending')
  await nextTick()
  renderDiagram()

  try {
    const res = await fetch('/collaboration/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        productName: form.value.productName,
        targetMarket: form.value.targetMarket,
        description: form.value.description,
      }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    jobId.value = data.jobId

    await nextTick()
    renderDiagram()

    currentSource = new EventSource(`/collaboration/stream/${data.jobId}`)
    currentSource.onmessage = (e) => {
      const event = JSON.parse(e.data)
      if (event.type === 'step') {
        handleStepEvent(event)
      } else if (event.type === 'done') {
        report.value = event.report
        loading.value = false
        waitingForApproval.value = false
        currentSource?.close()
      } else if (event.type === 'error') {
        loading.value = false
        currentSource?.close()
      }
    }
    currentSource.onerror = () => {
      if (loading.value) loading.value = false
      currentSource?.close()
    }
  } catch (e) {
    loading.value = false
  }
}

async function submitApproval(approved) {
  const endpoint = approved ? 'approve' : 'reject'
  await fetch(`/collaboration/${jobId.value}/${endpoint}`, { method: 'POST' })
  waitingForApproval.value = false
}

function reset() {
  currentSource?.close()
  jobId.value = null
  steps.value = []
  report.value = null
  loading.value = false
  waitingForApproval.value = false
  Object.keys(nodeStatuses.value).forEach(k => nodeStatuses.value[k] = 'pending')
}

onUnmounted(() => currentSource?.close())
</script>

<style scoped>
.view { flex: 1; display: flex; flex-direction: column; padding: 40px 24px; max-width: 1200px; margin: 0 auto; width: 100%; }
.view-header { margin-bottom: 32px; }
h2 { font-size: clamp(1.6rem, 4vw, 2.4rem); font-weight: 700; margin-bottom: 8px; }
.gradient {
  background: linear-gradient(135deg, #818cf8, #c084fc);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.subtitle { color: var(--text-muted); font-size: 1rem; margin-bottom: 16px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px; font-size: 12px; color: var(--text-muted);
}

/* Start Form */
.panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden;
}
.panel-header {
  padding: 12px 20px; border-bottom: 1px solid var(--border);
  font-size: 13px; font-weight: 600; color: var(--text-muted);
  display: flex; align-items: center; justify-content: space-between;
}
.panel-body { padding: 20px; }
.example-grid { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
.example-card {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 8px; padding: 8px 14px; cursor: pointer; text-align: left;
  display: flex; flex-direction: column; gap: 2px; transition: all 0.15s;
}
.example-card:hover { border-color: #818cf8; }
.example-title { font-size: 13px; font-weight: 600; color: var(--text); }
.example-market { font-size: 11px; color: var(--text-muted); }
.start-form { display: flex; flex-direction: column; gap: 14px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field label { font-size: 12px; font-weight: 500; color: var(--text-muted); }
.form-field input, .form-field textarea {
  background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 8px;
  padding: 10px 12px; font-size: 14px; color: var(--text); font-family: inherit; outline: none;
}
.form-field input:focus, .form-field textarea:focus { border-color: #818cf8; }
.form-field textarea { resize: none; }

/* Buttons */
.btn-primary {
  background: linear-gradient(135deg, #6366f1, #a855f7); border: none; border-radius: 8px;
  padding: 10px 24px; color: white; font-size: 14px; font-weight: 500;
  cursor: pointer; align-self: flex-end;
}
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-secondary {
  background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 8px;
  padding: 8px 20px; color: var(--text-muted); font-size: 13px; cursor: pointer;
  transition: all 0.15s;
}
.btn-secondary:hover { border-color: #6366f1; color: var(--text); }
.spinner {
  display: inline-block; width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Approval Banner */
.approval-banner {
  background: linear-gradient(135deg, rgba(245,158,11,0.12), rgba(234,179,8,0.06));
  border: 1px solid #92400e; border-radius: var(--radius);
  padding: 16px 20px; margin-bottom: 16px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap;
}
.approval-content { display: flex; align-items: flex-start; gap: 14px; flex: 1; }
.approval-icon { font-size: 1.8rem; line-height: 1; }
.approval-text { display: flex; flex-direction: column; gap: 4px; }
.approval-title { font-size: 14px; font-weight: 600; color: #fbbf24; }
.approval-summary { font-size: 13px; color: var(--text-muted); }
.approval-actions { display: flex; gap: 10px; }
.btn-approve {
  background: #14532d; border: 1px solid #22c55e; border-radius: 8px;
  padding: 8px 20px; color: #4ade80; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.15s;
}
.btn-approve:hover { background: #166534; }
.btn-reject {
  background: #450a0a; border: 1px solid #ef4444; border-radius: 8px;
  padding: 8px 20px; color: #f87171; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.15s;
}
.btn-reject:hover { background: #7f1d1d; }

/* Demo Layout */
.demo-layout { display: flex; flex-direction: column; gap: 16px; flex: 1; }
.panels { display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr; gap: 16px; align-items: stretch; flex: 1; }
@media (max-width: 900px) { .panels { grid-template-columns: 1fr; } }

/* Log Panel */
.log-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden; display: flex; flex-direction: column;
}
.log-body {
  padding: 12px; display: flex; flex-direction: column; gap: 6px;
  flex: 1; min-height: 0; overflow-y: auto;
}
.log-entry {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 8px 10px; border-radius: 6px;
  background: var(--bg-elevated); font-size: 13px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }
.log-icon {
  font-size: 14px; width: 18px; text-align: center; flex-shrink: 0; margin-top: 1px;
}
.log-running .log-icon { color: #3b82f6; animation: spin 1s linear infinite; display: inline-block; }
.log-done .log-icon { color: #22c55e; }
.log-feedback .log-icon { color: #f97316; }
.log-waiting .log-icon { color: #eab308; }
.log-rejected .log-icon { color: #ef4444; }
.log-error .log-icon { color: #ef4444; }
.log-content { flex: 1; }
.log-name { font-weight: 500; color: var(--text); }
.log-message { color: var(--text-muted); font-size: 12px; margin-top: 2px; line-height: 1.4; }
.log-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; margin-top: 2px; }
.log-placeholder { display: flex; align-items: center; gap: 10px; color: var(--text-muted); padding: 8px; }

/* Diagram Panel */
.diagram-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden; display: flex; flex-direction: column;
}
.diagram-legend { display: flex; align-items: center; gap: 8px; font-size: 11px; color: var(--text-muted); font-weight: 400; }
.legend-dot {
  width: 8px; height: 8px; border-radius: 2px; display: inline-block;
}
.legend-dot.running { background: #3b82f6; }
.legend-dot.done { background: #22c55e; }
.legend-dot.feedback { background: #f97316; }
.legend-dot.waiting { background: #eab308; }
.diagram-body {
  padding: 16px; flex: 1; min-height: 0; overflow: auto;
  display: flex; align-items: flex-start; justify-content: center;
}
.mermaid-container { width: 100%; }
.mermaid-container :deep(svg) { max-width: 100%; height: auto; }

/* Report */
.report-panel {
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden;
}
.report-header {
  display: flex; align-items: center; gap: 16px; padding: 20px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(168,85,247,0.04));
}
.report-icon { font-size: 2rem; }
.report-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 4px; }
.report-title { font-size: 18px; font-weight: 700; color: var(--text); }
.decision-badge {
  margin-left: auto; padding: 6px 16px; border-radius: 99px;
  font-size: 13px; font-weight: 700; letter-spacing: 0.05em;
}
.decision-badge.go { background: rgba(34,197,94,0.15); color: #4ade80; border: 1px solid #166534; }
.decision-badge.nogo { background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid #7f1d1d; }
.report-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0; }
@media (max-width: 700px) { .report-grid { grid-template-columns: 1fr; } }
.report-section { padding: 20px; border-bottom: 1px solid var(--border); }
.report-section:nth-child(odd) { border-right: 1px solid var(--border); }
.report-steps { border-right: none; }
.section-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 10px; }
.report-section p { font-size: 14px; line-height: 1.7; color: var(--text); margin: 0; }
.report-section ol { margin: 0; padding-left: 20px; display: flex; flex-direction: column; gap: 6px; }
.report-section li { font-size: 14px; line-height: 1.6; color: var(--text); }
.report-footer { padding: 16px 20px; display: flex; justify-content: flex-end; }
</style>
