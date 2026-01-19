import preset from '@portal/design-tokens/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [preset],
  content: [
    './src/**/*.{js,ts,jsx,tsx}',
  ],
};
