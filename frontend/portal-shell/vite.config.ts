import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'portal',
      filename: 'shellEntry.js',
      remotes: {
        blog_remote: "http://localhost:5082/assets/remoteEntry.js",
      },
      exposes: {
        './authStore': './src/store/auth.ts'
      },
      shared: ['vue', 'pinia'],
    })
  ],
  server: {
    port: 50000
  },
  build: {
    minify:false,
    target: 'esnext'
  }
})
