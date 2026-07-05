<template>
  <div class="app-shell">
    <nav class="navbar">
      <div class="nav-brand">
        <span class="brand-dot"></span>
        AI Agent Demo
      </div>
      <div class="nav-links">
        <RouterLink to="/knowledge">Knowledge</RouterLink>
        <RouterLink to="/memory">Memory</RouterLink>
        <RouterLink to="/research">Research</RouterLink>
        <RouterLink to="/agent">Agent</RouterLink>
      </div>
    </nav>
    <RouterView />
    <footer class="footer">
      <span>demo-1 &mdash; Spring Boot 3.5 + Embabel + Vue 3</span>
    </footer>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 56px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.brand-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 8px var(--primary);
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-links a {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  transition: all 0.15s;
}

.nav-links a:hover {
  color: var(--text);
  background: var(--bg-elevated);
}

.nav-links a.router-link-active {
  color: var(--primary-light);
  background: rgba(99, 102, 241, 0.12);
}

.footer {
  text-align: center;
  padding: 20px;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid var(--border);
  margin-top: auto;
}
</style>
