// frontend/shopping-frontend/tailwind.config.js
import designSystemPreset from '@portal/design-tokens/tailwind';

console.log('[DEBUG] Shopping Tailwind Config Loaded!');

/** @type {import('tailwindcss').Config} */
export default {
  presets: [designSystemPreset],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  plugins: [],
}
