import preset from '@portal/design-core/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [preset],
  content: [
    './src/**/*.{js,ts,jsx,tsx}',
    '../design-core/src/variants/**/*.ts',
    '../design-core/src/styles/**/*.css',
  ],
};
