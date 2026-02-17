import { addons } from 'storybook/manager-api';
import { themes } from 'storybook/theming';

addons.setConfig({
  theme: {
    ...themes.dark,
    brandTitle: 'Portal Design System',
    brandUrl: '/',

    // UI
    appBg: '#08090a',
    appContentBg: '#0e0f10',
    appPreviewBg: '#08090a',
    appBorderColor: '#26282b',
    appBorderRadius: 6,

    // Typography
    fontBase: '"Inter Variable", "Inter", -apple-system, BlinkMacSystemFont, sans-serif',
    fontCode: '"JetBrains Mono", "Fira Code", monospace',

    // Text colors
    textColor: '#f7f8f8',
    textInverseColor: '#08090a',
    textMutedColor: '#8a8f98',

    // Toolbar
    barTextColor: '#8a8f98',
    barHoverColor: '#f7f8f8',
    barSelectedColor: '#5e6ad2',
    barBg: '#0e0f10',

    // Form colors
    buttonBg: '#5e6ad2',
    buttonBorder: '#5e6ad2',
    booleanBg: '#1b1c1e',
    booleanSelectedBg: '#5e6ad2',
    inputBg: '#1b1c1e',
    inputBorder: '#26282b',
    inputTextColor: '#f7f8f8',
    inputBorderRadius: 4,

    // Brand colors
    colorPrimary: '#5e6ad2',
    colorSecondary: '#5e6ad2',
  },
});
