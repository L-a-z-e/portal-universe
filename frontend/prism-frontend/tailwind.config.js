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
    extend: {},
  },
  plugins: [],
}
