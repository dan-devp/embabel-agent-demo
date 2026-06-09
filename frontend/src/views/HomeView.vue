<template>
  <div class="home">
    <div class="hero">
      <h1>AI Agent <span class="gradient">Demo</span></h1>
      <p class="hero-sub">Spring Boot 3.5 + Embabel — verschiedene Agenten-Patterns live erleben.</p>
    </div>

    <div class="card-grid">
      <RouterLink v-for="card in cards" :key="card.path" :to="card.path" class="card">
        <div class="card-icon">{{ card.icon }}</div>
        <div class="card-content">
          <div class="card-title">{{ card.title }}</div>
          <div class="card-desc">{{ card.desc }}</div>
          <div class="chips">
            <span v-for="chip in card.chips" :key="chip" class="chip">{{ chip }}</span>
          </div>
        </div>
        <span class="card-arrow">→</span>
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
const cards = [
  {
    path: '/knowledge',
    icon: '🧠',
    title: 'Knowledge',
    desc: 'RAG-basierter Agent beantwortet Fragen über ein Dokumenten-Corpus mit Vektorsuche.',
    chips: ['RAG', 'Vektorsuche', 'Dokumente'],
  },
  {
    path: '/memory',
    icon: '💾',
    title: 'Memory',
    desc: 'Agent mit persistentem Gedächtnis — lernt Präferenzen über mehrere Sessions.',
    chips: ['Session Memory', 'Personalisierung'],
  },
  {
    path: '/research',
    icon: '🔍',
    title: 'Research',
    desc: 'Mehrstufiger Recherche-Agent zerlegt Fragen, sucht parallel und synthetisiert Berichte.',
    chips: ['Multi-Step', 'Parallele Suche', 'Synthese'],
  },
  {
    path: '/collaboration',
    icon: '🤝',
    title: 'Collaboration',
    desc: '6 spezialisierte Agenten arbeiten zusammen — GOAP-Orchestrierung, Feedback-Loop, Human-in-the-Loop.',
    chips: ['Multi-Agent', 'GOAP', 'Feedback Loop', 'HITL'],
  },
  {
    path: '/action',
    icon: '⚡',
    title: 'Action',
    desc: 'Tool-Using Agent mit 5 spezialisierten Tools — CRM, Kalkulation, Angebotserstellung, E-Mail und Terminplanung mit HITL-Freigabe.',
    chips: ['Tool-Using', 'CRM', 'HITL', 'Live SSE'],
  },
  {
    path: '/agent',
    icon: '💬',
    title: 'Agent',
    desc: 'Einfacher Chat-Agent — Basis-Pattern für LLM-gestützte Konversation.',
    chips: ['Chat', 'LLM', 'Basis'],
  },
]
</script>

<style scoped>
.home {
  flex: 1;
  padding: 60px 24px 40px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.hero {
  text-align: center;
  margin-bottom: 56px;
}

h1 {
  font-size: clamp(2rem, 5vw, 3.2rem);
  font-weight: 700;
  margin-bottom: 16px;
}

.gradient {
  background: linear-gradient(135deg, var(--primary-light), #c084fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  color: var(--text-muted);
  font-size: 1.1rem;
  max-width: 520px;
  margin: 0 auto;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, background 0.15s;
  cursor: pointer;
}

.card:hover {
  border-color: var(--primary-light);
  background: var(--bg-elevated);
}

.card-icon {
  font-size: 2rem;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 2px;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.card-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.chip {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  padding: 2px 8px;
  border-radius: 99px;
  font-size: 11px;
  color: var(--text-muted);
}

.card-arrow {
  font-size: 18px;
  color: var(--text-muted);
  flex-shrink: 0;
  align-self: center;
  transition: transform 0.15s, color 0.15s;
}

.card:hover .card-arrow {
  transform: translateX(4px);
  color: var(--primary-light);
}
</style>
