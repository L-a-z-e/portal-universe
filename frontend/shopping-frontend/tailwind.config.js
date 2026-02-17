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
  theme: {
    extend: {
      colors: {
        accent: {
          seafoam: 'var(--color-accent-seafoam, #cbf3f0)',
          teal: 'var(--color-accent-teal, #2ec4b6)',
        },
      },
    },
  },
  plugins: [],
}
