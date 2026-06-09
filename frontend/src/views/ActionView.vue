<template>
  <div class="view">
    <div class="view-header">
      <h2>Tool-Using <span class="gradient">Action Agent</span></h2>
      <p class="subtitle">5 spezialisierte Tools — CRM-Abfrage, Kalkulation, Angebotserstellung, E-Mail-Entwurf, Terminplanung mit Human-in-the-Loop Freigabe.</p>
      <div class="chips">
        <span class="chip">Tool-Using Agent</span>
        <span class="chip">CRM Integration</span>
        <span class="chip">Pricing Engine</span>
        <span class="chip">HITL Approval</span>
        <span class="chip">Live SSE</span>
      </div>
    </div>

    <!-- Start Form -->
    <div v-if="!jobId" class="panel">
      <div class="panel-header">Kundenanfrage starten</div>
      <div class="panel-body">
        <div class="example-grid">
          <button v-for="ex in examples" :key="ex.name" class="example-card" @click="applyExample(ex)">
            <span class="example-title">{{ ex.name }}</span>
            <span class="example-market">{{ ex.project }}</span>
          </button>
        </div>
        <form @submit.prevent="startDemo" class="start-form">
          <div class="form-row">
            <div class="form-field">
              <label>Kundenname / Firma</label>
              <input v-model="form.customerName" placeholder="z.B. Müller GmbH" :disabled="loading" />
            </div>
            <div class="form-field">
              <label>Projekttyp (z.B. Web-App, ERP-Migration)</label>
              <input v-model="form.projectType" placeholder="z.B. Web-App-Modernisierung" :disabled="loading" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label>Laufzeit (Monate)</label>
              <input v-model.number="form.durationMonths" type="number" min="1" max="36" placeholder="z.B. 6" :disabled="loading" />
            </div>
            <div class="form-field">
              <label>Teamgröße (Entwickler)</label>
              <input v-model.number="form.teamSize" type="number" min="1" max="20" placeholder="z.B. 4" :disabled="loading" />
            </div>
          </div>
          <div class="form-field">
            <label>Kurzbeschreibung der Anforderungen</label>
            <textarea v-model="form.description" rows="3"
              placeholder="Beschreiben Sie die wichtigsten Anforderungen und Ziele des Projekts" :disabled="loading" />
          </div>
          <button type="submit" :disabled="loading || !isFormValid" class="btn-primary">
            Angebot erstellen
          </button>
        </form>
      </div>
    </div>

    <!-- Main Demo Layout -->
    <div v-if="jobId" class="demo-layout">
      <!-- Approval Banner -->
      <div v-if="waitingForApproval" class="approval-banner">
        <div class="approval-content">
          <div class="approval-icon">📋</div>
          <div class="approval-text">
            <div class="approval-title">Angebot-Freigabe erforderlich</div>
            <div class="approval-summary">{{ approvalSummary }}</div>
            <div v-if="approvalEmailSubject" class="approval-email-subject">Betreff: {{ approvalEmailSubject }}</div>
            <div v-if="approvalEmailTo" class="approval-email-to">An: {{ approvalEmailTo }}</div>
          </div>
        </div>
        <div class="approval-actions">
          <button class="btn-approve" @click="submitApproval(true)">Freigeben</button>
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
              <span class="spinner"></span> Agent wird gestartet...
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
              <span class="legend-dot waiting"></span>wartet
            </span>
          </div>
          <div class="diagram-body">
            <div ref="diagramEl" class="mermaid-container"></div>
          </div>
        </div>
      </div>

      <!-- Final Result -->
      <div v-if="result" class="report-panel">
        <div class="report-header">
          <span class="report-icon">🤝</span>
          <div>
            <div class="report-label">Angebot erstellt</div>
            <div class="report-title">{{ result.offerTitle }}</div>
          </div>
          <span class="amount-badge">{{ result.totalAmount }}</span>
        </div>

        <div class="report-grid">
          <div class="report-section">
            <div class="section-label">Zusammenfassung</div>
            <p>{{ result.summary }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Angebot</div>
            <p>{{ result.offerTitle }}<br>Gesamtbetrag: {{ result.totalAmount }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Kickoff-Meeting</div>
            <p>{{ result.proposedMeetingDate }}</p>
          </div>
          <div class="report-section">
            <div class="section-label">Kunde</div>
            <p>{{ result.customerName }}</p>
          </div>
        </div>

        <div class="report-section report-steps">
          <div class="section-label">Meeting-Agenda</div>
          <ol>
            <li v-for="(item, i) in result.meetingAgenda" :key="'agenda-' + i">{{ item }}</li>
          </ol>
        </div>

        <div class="report-section report-steps">
          <div class="section-label">Nächste Schritte</div>
          <ol>
            <li v-for="(step, i) in result.nextSteps" :key="'step-' + i">{{ step }}</li>
          </ol>
        </div>

        <div class="report-footer">
          <button class="btn-secondary" @click="reset">Neue Anfrage</button>
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

const form = ref({ customerName: '', projectType: '', durationMonths: null, teamSize: null, description: '' })
const jobId = ref(null)
const loading = ref(false)
const steps = ref([])
const result = ref(null)
const waitingForApproval = ref(false)
const approvalSummary = ref('')
const approvalEmailSubject = ref('')
const approvalEmailTo = ref('')
const diagramEl = ref(null)
const logBody = ref(null)

const nodeStatuses = ref({
  analyze_request: 'pending',
  crm_lookup: 'pending',
  pricing: 'pending',
  generate_offer: 'pending',
  draft_email: 'pending',
  approval_gate: 'pending',
  schedule_meeting: 'pending',
})

const toolStatuses = ref({})

const examples = [
  { name: 'Müller GmbH', project: 'Web-App-Modernisierung', duration: 6, team: 4, description: 'Migration eines Legacy-Portals auf moderne React/Spring-Architektur mit SSO-Anbindung.' },
  { name: 'TechCorp AG', project: 'ERP-Integration', duration: 3, team: 3, description: 'REST-API-Integration zwischen SAP und bestehenden Microservices inklusive Datenmigration.' },
  { name: 'StartupXY', project: 'MVP-Entwicklung', duration: 4, team: 5, description: 'Greenfield-MVP für B2B SaaS-Plattform — Backend, Frontend, CI/CD, Cloud-Deployment.' },
]

let currentSource = null

const isFormValid = computed(() =>
  form.value.customerName.trim() &&
  form.value.projectType.trim() &&
  form.value.durationMonths > 0 &&
  form.value.teamSize > 0 &&
  form.value.description.trim()
)

function applyExample(ex) {
  form.value.customerName = ex.name
  form.value.projectType = ex.project
  form.value.durationMonths = ex.duration
  form.value.teamSize = ex.team
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

function toolClass(toolId) {
  const s = toolStatuses.value[toolId]
  return s === 'DONE' ? 'toolDone' : 'toolRunning'
}

function buildDiagram() {
  const c = (id) => nodeClass(id)
  const t = (id) => toolClass(id)

  const toolLines = []
  if (toolStatuses.value['CrmTool']) {
    toolLines.push(`  n2 -.-> tCrm[[CrmTool]]`)
    toolLines.push(`  class tCrm ${t('CrmTool')}`)
  }
  if (toolStatuses.value['PricingTool']) {
    toolLines.push(`  n3 -.-> tPrice[[PricingTool]]`)
    toolLines.push(`  class tPrice ${t('PricingTool')}`)
  }
  if (toolStatuses.value['CalendarTool']) {
    toolLines.push(`  n7 -.-> tCal[[CalendarTool]]`)
    toolLines.push(`  class tCal ${t('CalendarTool')}`)
  }

  return `flowchart TD
  n0([Start])
  n0 --> n1[Anfrage analysieren]
  n1 --> n2[CRM-Abfrage]
  n2 --> n3[Kalkulation]
  n3 --> n4[Angebot erstellen]
  n4 --> n5[E-Mail verfassen]
  n5 --> n6{Freigabe}
  n6 -->|Genehmigt| n7[Meeting planen]
  n6 -->|Abgelehnt| n8x([Abgebrochen])
  n7 --> n9([Ende])
${toolLines.join('\n')}

  classDef pending fill:#1e293b,stroke:#334155,color:#94a3b8
  classDef running fill:#1d3461,stroke:#3b82f6,color:#fff,stroke-width:2px
  classDef done fill:#14532d,stroke:#22c55e,color:#fff
  classDef feedback fill:#431407,stroke:#f97316,color:#fff,stroke-width:2px
  classDef waiting fill:#451a03,stroke:#f59e0b,color:#fff,stroke-width:2px
  classDef rejected fill:#450a0a,stroke:#ef4444,color:#fff
  classDef terminal fill:#0f172a,stroke:#818cf8,color:#818cf8,stroke-width:2px
  classDef toolRunning fill:#2d1b69,stroke:#a855f7,color:#e9d5ff,stroke-width:2px
  classDef toolDone fill:#1a1f2e,stroke:#a855f7,color:#c084fc

  class n0 terminal
  class n9 terminal
  class n8x rejected
  class n1 ${c('analyze_request')}
  class n2 ${c('crm_lookup')}
  class n3 ${c('pricing')}
  class n4 ${c('generate_offer')}
  class n5 ${c('draft_email')}
  class n6 ${c('approval_gate')}
  class n7 ${c('schedule_meeting')}`
}

async function renderDiagram() {
  if (!diagramEl.value) return
  try {
    const id = 'action-diagram-' + (++renderCounter)
    const { svg } = await mermaid.render(id, buildDiagram())
    diagramEl.value.innerHTML = svg
  } catch (e) {
    console.warn('Mermaid render error:', e)
  }
}

watch([nodeStatuses, toolStatuses], async () => {
  await nextTick()
  renderDiagram()
}, { deep: true })

const STEP_NODE_MAP = {
  analyze_request: 'analyze_request',
  crm_lookup: 'crm_lookup',
  pricing: 'pricing',
  generate_offer: 'generate_offer',
  draft_email: 'draft_email',
  approval_gate: 'approval_gate',
  schedule_meeting: 'schedule_meeting',
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
  result.value = null
  waitingForApproval.value = false
  approvalEmailSubject.value = ''
  approvalEmailTo.value = ''

  Object.keys(nodeStatuses.value).forEach(k => nodeStatuses.value[k] = 'pending')
  toolStatuses.value = {}
  await nextTick()
  renderDiagram()

  try {
    const res = await fetch('/action/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        customerName: form.value.customerName,
        projectType: form.value.projectType,
        durationMonths: form.value.durationMonths,
        teamSize: form.value.teamSize,
        description: form.value.description,
      }),
    })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    jobId.value = data.jobId

    await nextTick()
    renderDiagram()

    currentSource = new EventSource(`/action/stream/${data.jobId}`)
    currentSource.onmessage = (e) => {
      const event = JSON.parse(e.data)
      if (event.type === 'step') {
        handleStepEvent(event)
      } else if (event.type === 'tool') {
        toolStatuses.value = { ...toolStatuses.value, [event.toolId]: event.status }
      } else if (event.type === 'done') {
        result.value = event.result
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
  await fetch(`/action/${jobId.value}/${endpoint}`, { method: 'POST' })
  waitingForApproval.value = false
}

function reset() {
  currentSource?.close()
  jobId.value = null
  steps.value = []
  result.value = null
  loading.value = false
  waitingForApproval.value = false
  approvalEmailSubject.value = ''
  approvalEmailTo.value = ''
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
.approval-email-subject { font-size: 12px; color: var(--text-muted); font-style: italic; }
.approval-email-to { font-size: 12px; color: var(--text-muted); }
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
.amount-badge {
  margin-left: auto; padding: 6px 16px; border-radius: 99px;
  font-size: 13px; font-weight: 700;
  background: rgba(34,197,94,0.15); color: #4ade80; border: 1px solid #166534;
}
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
