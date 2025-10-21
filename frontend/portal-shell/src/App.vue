<script setup lang="ts">
import { useAuthStore } from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";

const authStore = useAuthStore();
</script>

<template>
  <div id="portal-shell-container">
    <header>
      <h1>Portal Universe</h1>
      <nav>
        <router-link to="/">Home</router-link> |
        <router-link to="/blog">Blog</router-link>
      </nav>
      <div class="auth-status">
        <template v-if="authStore.isAuthenticated">
          <span class="welcome">
            Welcome, <strong>{{ authStore.displayName }}</strong>!
            <span v-if="authStore.isAdmin" class="badge-admin">ADMIN</span>
          </span>
          <button @click="logout" class="btn-logout">Logout</button>
        </template>
        <template v-else>
          <button @click="login" class="btn-login">Login</button>
        </template>
      </div>
    </header>

    <main>
      <Suspense>
        <template #default>
          <router-view :key="$route.path" v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </template>
        <template #fallback>
          Loading Page...
        </template>
      </Suspense>
    </main>

    <footer>
      <p>Portal Universe</p>
    </footer>
  </div>
</template>

<style scoped>
#portal-shell-container {
  max-width: 1200px;
  margin: 0 auto;
  font-family: sans-serif;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #ccc;
}

nav a {
  margin: 0 1rem;
  font-weight: bold;
  text-decoration: none;
}

nav a.router-link-exact-active {
  color: #42b983;
}

.auth-status {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.welcome {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.badge-admin {
  background: #f44336;
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

.btn-login,
.btn-logout {
  padding: 0.5rem 1rem;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.btn-login:hover,
.btn-logout:hover {
  background: #f0f0f0;
}

main {
  padding: 1rem;
  min-height: 400px;
}

footer {
  padding: 1rem;
  text-align: center;
  border-top: 1px solid #ccc;
}
</style>