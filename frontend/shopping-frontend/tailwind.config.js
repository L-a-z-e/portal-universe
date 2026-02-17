// frontend/shopping-frontend/tailwind.config.js
import designSystemPreset from '@portal/design-core/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [designSystemPreset],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../design-core/src/variants/**/*.ts",
    "../design-core/src/styles/**/*.css",
    "../design-react/src/**/*.{js,ts,jsx,tsx}",
  ],
  plugins: [],
}
