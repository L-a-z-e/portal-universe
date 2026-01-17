/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        // Brand colors (Shopping uses green theme)
        'brand-primary': 'var(--color-brand-primary)',
        'brand-secondary': 'var(--color-brand-secondary)',
        'brand-accent': 'var(--color-brand-accent)',

        // Background colors
        'bg-page': 'var(--color-bg-page)',
        'bg-card': 'var(--color-bg-card)',
        'bg-subtle': 'var(--color-bg-subtle)',
        'bg-hover': 'var(--color-bg-hover)',
        'bg-input': 'var(--color-bg-input)',
        'bg-disabled': 'var(--color-bg-disabled)',

        // Text colors
        'text-heading': 'var(--color-text-heading)',
        'text-body': 'var(--color-text-body)',
        'text-meta': 'var(--color-text-meta)',
        'text-placeholder': 'var(--color-text-placeholder)',
        'text-disabled': 'var(--color-text-disabled)',

        // Border colors
        'border-default': 'var(--color-border-default)',
        'border-strong': 'var(--color-border-strong)',
        'border-subtle': 'var(--color-border-subtle)',

        // Status colors
        'status-success': 'var(--color-status-success)',
        'status-success-bg': 'var(--color-status-success-bg)',
        'status-warning': 'var(--color-status-warning)',
        'status-warning-bg': 'var(--color-status-warning-bg)',
        'status-error': 'var(--color-status-error)',
        'status-error-bg': 'var(--color-status-error-bg)',
        'status-info': 'var(--color-status-info)',
        'status-info-bg': 'var(--color-status-info-bg)',
      },
    },
  },
  plugins: [],
}
