import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AgentView from '../views/AgentView.vue'
import KnowledgeView from '../views/KnowledgeView.vue'
import MemoryView from '../views/MemoryView.vue'
import ResearchView from '../views/ResearchView.vue'
import CollaborationView from '../views/CollaborationView.vue'
import ActionView from '../views/ActionView.vue'

const routes = [
  { path: '/', component: HomeView },
  { path: '/agent', component: AgentView },
  { path: '/knowledge', component: KnowledgeView },
  { path: '/memory', component: MemoryView },
  { path: '/research', component: ResearchView },
  { path: '/collaboration', component: CollaborationView },
  { path: '/action', component: ActionView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
