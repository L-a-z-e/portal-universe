import path from 'node:path';
import { fileURLToPath } from 'node:url';
import plugin from 'tailwindcss/plugin';

/**
 * @portal/design-core - Tailwind CSS Preset
 * Linear-inspired dark-first design system preset
 * @type {import('tailwindcss').Config}
 */
export default {
    // Dark mode first - use 'dark' class for dark (default), 'light' class for light
    darkMode: ['class', '[data-theme="dark"]'],

    // Include design-core variant files in Tailwind content scan
    // so Tailwind classes defined in variants/*.ts are compiled
    content: [
        path.join(path.dirname(fileURLToPath(import.meta.url)), 'src/variants/**/*.ts'),
        path.join(path.dirname(fileURLToPath(import.meta.url)), 'src/styles/**/*.css'),
    ],

    theme: {
        extend: {
            // 1. Colors - Semantic tokens
            colors: {
                'brand': {
                    'primary': 'var(--semantic-brand-primary)',
                    'primaryHover': 'var(--semantic-brand-primaryHover)',
                    'secondary': 'var(--semantic-brand-secondary)',
                },
                'text': {
                    'heading': 'var(--semantic-text-heading)',
                    'body': 'var(--semantic-text-body)',
                    'meta': 'var(--semantic-text-meta)',
                    'muted': 'var(--semantic-text-muted)',
                    'inverse': 'var(--semantic-text-inverse)',
                    'link': 'var(--semantic-text-link)',
                    'linkHover': 'var(--semantic-text-linkHover)',
                    'placeholder': 'var(--semantic-text-muted)',
                    'disabled': 'var(--semantic-text-muted)',
                },
                'bg': {
                    'page': 'var(--semantic-bg-page)',
                    'card': 'var(--semantic-bg-card)',
                    'elevated': 'var(--semantic-bg-elevated)',
                    'muted': 'var(--semantic-bg-muted)',
                    'hover': 'var(--semantic-bg-hover)',
                    'subtle': 'var(--semantic-bg-muted)',
                    'input': 'var(--semantic-bg-card)',
                    'disabled': 'var(--semantic-bg-muted)',
                    'overlay': 'var(--semantic-bg-overlay)',
                    'sidebar': 'var(--semantic-bg-sidebar)',
                },
                'border': {
                    'default': 'var(--semantic-border-default)',
                    'hover': 'var(--semantic-border-hover)',
                    'focus': 'var(--semantic-border-focus)',
                    'muted': 'var(--semantic-border-muted)',
                    'subtle': 'var(--semantic-border-subtle)',
                    'strong': 'var(--semantic-border-hover)',
                },
                'status': {
                    'success': 'var(--semantic-status-success)',
                    'successBg': 'var(--semantic-status-successBg)',
                    'success-bg': 'var(--semantic-status-successBg)',
                    'error': 'var(--semantic-status-error)',
                    'errorBg': 'var(--semantic-status-errorBg)',
                    'error-bg': 'var(--semantic-status-errorBg)',
                    'warning': 'var(--semantic-status-warning)',
                    'warningBg': 'var(--semantic-status-warningBg)',
                    'warning-bg': 'var(--semantic-status-warningBg)',
                    'info': 'var(--semantic-status-info)',
                    'infoBg': 'var(--semantic-status-infoBg)',
                    'info-bg': 'var(--semantic-status-infoBg)',
                },
                // Linear color palette direct access
                'linear': {
                    '50': '#f7f8f8',
                    '100': '#ebeced',
                    '200': '#d0d6e0',
                    '300': '#8a8f98',
                    '400': '#6c717a',
                    '500': '#5c6169',
                    '600': '#3e3e44',
                    '700': '#26282b',
                    '800': '#1b1c1e',
                    '850': '#141516',
                    '900': '#0e0f10',
                    '950': '#08090a',
                },
                'indigo': {
                    '400': '#5e6ad2',
                    '500': '#4754c9',
                    '600': '#3f4ab8',
                },
                'orange': {
                    '400': '#FB923C',
                    '500': '#F97316',
                    '600': '#EA580C',
                },
                'cyan': {
                    '400': '#22D3EE',
                    '500': '#06B6D4',
                    '600': '#0891B2',
                },
                'violet': {
                    '400': '#A78BFA',
                    '500': '#8B5CF6',
                    '600': '#7C3AED',
                },
                'teal': {
                    '400': '#38D9A9',
                    '500': '#20C997',
                    '600': '#12B886',
                },
                'nightfall': {
                    '50': '#f3e8ff',
                    '100': '#e0aaff',
                    '200': '#c77dff',
                    '300': '#9d4edd',
                    '400': '#7b2cbf',
                    '500': '#5a189a',
                    '600': '#3c096c',
                    '700': '#240046',
                    '800': '#10002b',
                    '900': '#0a0018',
                },
            },

            // 2. Typography (Inter Variable optimized)
            fontFamily: {
                'sans': ['Inter Variable', 'Inter', '-apple-system', 'BlinkMacSystemFont', 'Pretendard Variable', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'Noto Sans KR', 'sans-serif'],
                'mono': ['JetBrains Mono', 'Fira Code', 'SF Mono', 'Consolas', 'Courier New', 'monospace'],
            },
            fontSize: {
                'micro': ['0.625rem', { lineHeight: '1' }],
                'xs': ['0.6875rem', { lineHeight: '1.2' }],
                'sm': ['0.8125rem', { lineHeight: '1.4' }],
                'base': ['0.875rem', { lineHeight: '1.5' }],
                'lg': ['1rem', { lineHeight: '1.5' }],
                'xl': ['1.125rem', { lineHeight: '1.4' }],
                '2xl': ['1.25rem', { lineHeight: '1.3' }],
                '3xl': ['1.5rem', { lineHeight: '1.3' }],
                '4xl': ['1.875rem', { lineHeight: '1.2' }],
                '5xl': ['2.25rem', { lineHeight: '1.2' }],
                '6xl': ['3rem', { lineHeight: '1.1' }],
                '7xl': ['3.75rem', { lineHeight: '1.1' }],
                '8xl': ['4.5rem', { lineHeight: '1' }],
                '9xl': ['6rem', { lineHeight: '1' }],
            },
            lineHeight: {
                'none': '1',
                'tight': '1.2',
                'snug': '1.375',
                'normal': '1.5',
                'relaxed': '1.625',
                'loose': '1.75',
            },
            fontWeight: {
                'light': '300',
                'normal': '400',
                'medium': '510',
                'semibold': '590',
                'bold': '680',
                'extrabold': '800',
            },

            // 3. Layout & Effects
            spacing: {
                'xs': 'var(--spacing-xs)',
                'sm': 'var(--spacing-sm)',
                'md': 'var(--spacing-md)',
                'lg': 'var(--spacing-lg)',
                'xl': 'var(--spacing-xl)',
                '2xl': 'var(--spacing-2xl)',
            },
            borderRadius: {
                'none': '0',
                'sm': '0.25rem',
                'DEFAULT': '0.375rem',
                'md': '0.5rem',
                'lg': '0.75rem',
                'xl': '1rem',
                '2xl': '1.5rem',
                'full': '9999px',
            },
            boxShadow: {
                'sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
                'DEFAULT': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
                'md': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
                'lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
                'xl': '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
                '2xl': '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
                'inner': 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.05)',
                'glow': '0 0 20px rgba(157, 78, 221, 0.3)',
                'glow-lg': '0 0 40px rgba(157, 78, 221, 0.4)',
            },
            maxWidth: {
                'content': '768px',
                'container': '1280px',
            },
            width: {
                'sidebar': '16rem',
                'sidebar-xl': '18rem',
            },

            // 4. Animation & Transitions (Linear-style)
            transitionDuration: {
                'fast': '100ms',
                'normal': '160ms',
                'slow': '250ms',
                'slower': '400ms',
            },
            transitionTimingFunction: {
                'linear-ease': 'cubic-bezier(0.25, 0.1, 0.25, 1)',
                'out-quart': 'cubic-bezier(0.165, 0.84, 0.44, 1)',
                'spring': 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
            },

            // 5. Backdrop blur (glassmorphism)
            backdropBlur: {
                'xs': '2px',
                'sm': '4px',
                'DEFAULT': '12px',
                'md': '16px',
                'lg': '24px',
                'xl': '40px',
                'glass': '12px',
            },

            // 6. Animation keyframes
            keyframes: {
                'fade-in': {
                    '0%': { opacity: '0' },
                    '100%': { opacity: '1' },
                },
                'fade-out': {
                    '0%': { opacity: '1' },
                    '100%': { opacity: '0' },
                },
                'slide-up': {
                    '0%': { transform: 'translateY(10px)', opacity: '0' },
                    '100%': { transform: 'translateY(0)', opacity: '1' },
                },
                'slide-down': {
                    '0%': { transform: 'translateY(-10px)', opacity: '0' },
                    '100%': { transform: 'translateY(0)', opacity: '1' },
                },
                'scale-in': {
                    '0%': { transform: 'scale(0.95)', opacity: '0' },
                    '100%': { transform: 'scale(1)', opacity: '1' },
                },
            },
            animation: {
                'fade-in': 'fade-in 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
                'fade-out': 'fade-out 100ms cubic-bezier(0.25, 0.1, 0.25, 1)',
                'slide-up': 'slide-up 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
                'slide-down': 'slide-down 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
                'scale-in': 'scale-in 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
            },
        }
    },

    plugins: [
        require('@tailwindcss/typography'),
        // Dark-first design: 'light:' variant for light mode overrides
        plugin(function({ addVariant }) {
            // Support both .light class and data-theme="light" attribute
            addVariant('light', ['[data-theme="light"] &', '.light &']);
        }),
    ]
};
