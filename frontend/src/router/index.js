import { createRouter, createWebHistory } from 'vue-router'
import AgentView from '../views/AgentView.vue'
import KnowledgeView from '../views/KnowledgeView.vue'
import MemoryView from '../views/MemoryView.vue'
import ResearchView from '../views/ResearchView.vue'

const routes = [
  { path: '/', redirect: '/knowledge' },
  { path: '/agent', component: AgentView },
  { path: '/knowledge', component: KnowledgeView },
  { path: '/memory', component: MemoryView },
  { path: '/research', component: ResearchView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
