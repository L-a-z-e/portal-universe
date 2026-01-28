// frontend/prism-frontend/tailwind.config.js
import designSystemPreset from '@portal/design-tokens/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [designSystemPreset],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Prism 서비스 전용 색상 (brand-primary를 override)
        prism: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        }
      }
    },
  },
  plugins: [],
}
