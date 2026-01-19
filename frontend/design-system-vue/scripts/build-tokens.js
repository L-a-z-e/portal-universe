import { readFileSync, writeFileSync } from 'fs';
import { join, resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const tokensDir = resolve(__dirname, '../src/tokens');
const outputFile = resolve(__dirname, '../src/styles/index.css');

/**
 * 토큰 값을 재귀적으로 CSS 변수로 변환
 */
function flattenTokens(tokens, prefix, cssVars, parentKey = '') {
    for (const [key, value] of Object.entries(tokens)) {
        if (key.startsWith('$')) continue;

        const fullKey = parentKey ? `${parentKey}-${key}` : key;
        const cssVarName = prefix ? `${prefix}-${fullKey}` : `--${fullKey}`;

        if (typeof value === 'object' && value !== null && !('$value' in value)) {
            flattenTokens(value, prefix, cssVars, fullKey);
        } else if (value && typeof value === 'object' && '$value' in value) {
            cssVars.set(cssVarName, value.$value);
        } else if (typeof value === 'string' || typeof value === 'number') {
            cssVars.set(cssVarName, value);
        }
    }
}

/**
 * 색상 참조 {color.xxx.yyy} 형식을 실제 값으로 해결
 */
function resolveColorReference(value, colorReferences) {
    if (typeof value !== 'string') return value;

    const refMatch = value.match(/^\{([^}]+)\}$/);
    if (!refMatch) return value;

    const refPath = refMatch[1];
    const resolved = colorReferences[refPath];

    if (resolved) {
        return resolved;
    } else {
        return value;
    }
}

/**
 * 테마 색상 객체를 CSS 변수로 변환 (darkMode 지원)
 * @param {Object} obj - 색상 객체
 * @param {Map} targetMap - 대상 CSS 변수 맵
 * @param {Object} colorReferences - 색상 참조 맵
 * @param {string} prefix - CSS 변수 접두사
 * @param {string} parentKey - 부모 키
 */
function processThemeColors(obj, targetMap, colorReferences, prefix = '--semantic', parentKey = '') {
    for (const [key, value] of Object.entries(obj)) {
        if (key.startsWith('$') || key === 'darkMode') continue;

        const fullKey = parentKey ? `${parentKey}-${key}` : key;
        const varName = `${prefix}-${fullKey}`;

        if (typeof value === 'object' && value !== null && !('$value' in value)) {
            processThemeColors(value, targetMap, colorReferences, prefix, fullKey);
        } else if (value && typeof value === 'object' && '$value' in value) {
            const resolved = resolveColorReference(value.$value, colorReferences);
            targetMap.set(varName, resolved);
        } else if (typeof value === 'string') {
            const resolved = resolveColorReference(value, colorReferences);
            targetMap.set(varName, resolved);
        }
    }
}

/**
 * 토큰 파일들을 읽고 CSS 변수로 변환
 */
function buildTokens() {
    const cssVariables = new Map();
    const themes = new Map();
    const themeDarkModes = new Map();
    const colorReferences = {};
    const unresolvedReferences = new Set();

    try {
        console.log('📖 Step 1: Building color reference map...');

        try {
            const colorPath = join(tokensDir, 'base', 'colors.json');
            const colorContent = readFileSync(colorPath, 'utf-8');
            const colorTokens = JSON.parse(colorContent);

            function buildColorMap(obj, prefix = '') {
                for (const [key, value] of Object.entries(obj)) {
                    if (key.startsWith('$')) continue;

                    const fullKey = prefix ? `${prefix}.${key}` : key;

                    if (typeof value === 'object' && value !== null && !('$value' in value)) {
                        buildColorMap(value, fullKey);
                    } else if (value && typeof value === 'object' && '$value' in value) {
                        colorReferences[fullKey] = value.$value;
                    } else if (typeof value === 'string') {
                        colorReferences[fullKey] = value;
                    }
                }
            }

            buildColorMap(colorTokens.color || colorTokens, 'color');

            console.log(`  ✅ Color reference map built (${Object.keys(colorReferences).length} colors)`);

            const colorRefSample = Object.entries(colorReferences).slice(0, 5);
            colorRefSample.forEach(([key, val]) => {
                console.log(`     • ${key}: ${val}`);
            });
        } catch (err) {
            console.log(`  ⚠️  Error building color reference map: ${err.message}`);
        }

        console.log('📖 Step 2: Reading base tokens...');

        ['colors', 'typography', 'spacing', 'border', 'effects'].forEach(tokenType => {
            const filePath = join(tokensDir, 'base', `${tokenType}.json`);
            try {
                const content = readFileSync(filePath, 'utf-8');
                const tokens = JSON.parse(content);

                let prefix = '--token';
                let tokenContent = tokens;

                if (tokenType === 'colors') {
                    prefix = '--color';
                    tokenContent = tokens.color || tokens;
                } else if (tokenType === 'typography') {
                    prefix = '--typography';
                    tokenContent = tokens.typography || tokens;
                } else if (tokenType === 'spacing') {
                    prefix = '--spacing';
                    tokenContent = tokens.spacing || tokens;
                } else if (tokenType === 'border') {
                    prefix = '--border';
                    tokenContent = tokens.border || tokens;
                } else if (tokenType === 'effects') {
                    prefix = '--effects';
                    tokenContent = tokens.effects || tokens;
                }

                flattenTokens(tokenContent, prefix, cssVariables);
                console.log(`  ✅ ${tokenType}.json loaded`);
            } catch (err) {
                console.log(`  ⚠️  ${tokenType}.json not found or invalid`);
            }
        });

        console.log('📖 Step 3: Reading semantic tokens...');
        try {
            const semanticPath = join(tokensDir, 'semantic', 'colors.json');
            const content = readFileSync(semanticPath, 'utf-8');
            const semanticTokens = JSON.parse(content);

            let semanticContent = semanticTokens;
            if (semanticTokens.semantic?.colors) {
                semanticContent = semanticTokens.semantic.colors;
            } else if (semanticTokens.color) {
                semanticContent = semanticTokens.color;
            } else if (semanticTokens.colors) {
                semanticContent = semanticTokens.colors;
            }

            function processSemantic(obj, prefix = '--semantic', parentKey = '') {
                for (const [key, value] of Object.entries(obj)) {
                    if (key.startsWith('$')) continue;

                    const fullKey = parentKey ? `${parentKey}-${key}` : key;
                    const varName = `${prefix}-${fullKey}`;

                    if (typeof value === 'object' && value !== null && !('$value' in value)) {
                        processSemantic(value, prefix, fullKey);
                    } else if (value && typeof value === 'object' && '$value' in value) {
                        const resolved = resolveColorReference(value.$value, colorReferences);

                        if (resolved.startsWith('{')) {
                            const refPath = resolved.match(/^\{([^}]+)\}$/)?.[1];
                            if (refPath) unresolvedReferences.add(refPath);
                        }

                        cssVariables.set(varName, resolved);
                    } else if (typeof value === 'string') {
                        const resolved = resolveColorReference(value, colorReferences);

                        if (resolved.startsWith('{')) {
                            const refPath = resolved.match(/^\{([^}]+)\}$/)?.[1];
                            if (refPath) unresolvedReferences.add(refPath);
                        }

                        cssVariables.set(varName, resolved);
                    }
                }
            }

            processSemantic(semanticContent);
            console.log('  ✅ semantic/colors.json loaded');
        } catch (err) {
            console.log(`  ⚠️  semantic/colors.json not found or invalid: ${err.message}`);
        }

        console.log('📖 Step 4: Reading theme tokens (with darkMode/lightMode support)...');
        const themeFiles = ['portal', 'blog', 'shopping'];
        const themeLightModes = new Map(); // For dark-first themes like portal

        themeFiles.forEach(themeName => {
            const filePath = join(tokensDir, 'themes', `${themeName}.json`);
            try {
                const content = readFileSync(filePath, 'utf-8');
                const themeTokens = JSON.parse(content);

                const themeLightVars = new Map();
                const themeDarkVars = new Map();

                // Portal uses dark-first approach (lightMode section)
                if (themeName === 'portal') {
                    // Default (dark mode) processing
                    processThemeColors(
                        themeTokens.color || themeTokens,
                        themeDarkVars,
                        colorReferences,
                        '--semantic'
                    );

                    // Light mode processing (lightMode section)
                    if (themeTokens.lightMode) {
                        processThemeColors(
                            themeTokens.lightMode.color || themeTokens.lightMode,
                            themeLightVars,
                            colorReferences,
                            '--semantic'
                        );
                    }

                    // Store portal theme (dark as default)
                    themes.set(themeName, themeDarkVars);
                    if (themeLightVars.size > 0) {
                        themeLightModes.set(themeName, themeLightVars);
                    }
                } else {
                    // Other themes use light-first approach (darkMode section)
                    // Light mode processing (default)
                    processThemeColors(
                        themeTokens.color || themeTokens,
                        themeLightVars,
                        colorReferences,
                        '--semantic'
                    );

                    // Dark mode processing (darkMode section)
                    if (themeTokens.darkMode) {
                        processThemeColors(
                            themeTokens.darkMode.color || themeTokens.darkMode,
                            themeDarkVars,
                            colorReferences,
                            '--semantic'
                        );
                    }

                    themes.set(themeName, themeLightVars);
                    if (themeDarkVars.size > 0) {
                        themeDarkModes.set(themeName, themeDarkVars);
                    }
                }

                console.log(`  ✅ themes/${themeName}.json loaded (${themeName === 'portal' ? 'dark-first' : 'light-first'})`);
            } catch (err) {
                console.log(`  ⚠️  themes/${themeName}.json not found or invalid: ${err.message}`);
            }
        });

        if (unresolvedReferences.size > 0) {
            console.log('\n⚠️  UNRESOLVED REFERENCES:');
            Array.from(unresolvedReferences).sort().forEach(ref => {
                console.log(`   • {${ref}}`);
            });
            console.log('\n📋 Available colors in reference map:');
            const availableColors = Object.keys(colorReferences)
                .filter(k => k.startsWith('color.'))
                .sort();
            availableColors.slice(0, 30).forEach(color => {
                console.log(`   • ${color}`);
            });
            if (availableColors.length > 30) {
                console.log(`   ... and ${availableColors.length - 30} more`);
            }
        } else {
            console.log('\n✅ All color references resolved successfully!');
        }

        console.log('\n🎨 Step 5: Generating CSS with Linear theme support...');

        let cssContent = `/* Inter Variable Font */
@import '@fontsource-variable/inter';

@tailwind base;
@tailwind components;
@tailwind utilities;

/* ============================================
   Auto-generated Design System CSS Variables
   Linear-inspired theme - DO NOT EDIT MANUALLY
   Generated by build-tokens.js
   ============================================ */

:root {
`;

        const sortedVars = Array.from(cssVariables.entries()).sort();
        for (const [key, value] of sortedVars) {
            cssContent += `    ${key}: ${value};\n`;
        }

        cssContent += `}

/* ============================================
   Dark Mode Overrides (Global Default)
   ============================================ */
[data-theme="dark"] {
    /* Brand Colors */
    --semantic-brand-primary: #20C997;
    --semantic-brand-primaryHover: #12B886;
    --semantic-brand-secondary: #63E6BE;
    
    /* Text Colors  */
    --semantic-text-heading: #F0F6FC;
    --semantic-text-body: #C9D1D9;
    --semantic-text-meta: #8B949E;
    --semantic-text-muted: #6E7681;
    --semantic-text-inverse: #111827;
    --semantic-text-link: #58A6FF;
    --semantic-text-linkHover: #A5D6FF;
    
    /* Background Colors  */
    --semantic-bg-page: #0F1419;
    --semantic-bg-card: #1C2128;
    --semantic-bg-elevated: #2D333B;
    --semantic-bg-muted: #22272E;
    --semantic-bg-hover: #2D333B;
    
    /* Border Colors  */
    --semantic-border-default: #444C56;
    --semantic-border-hover: #6E7681;
    --semantic-border-muted: #2C2C2C;
    --semantic-border-focus: #58A6FF;
    
    /* Status Colors  */
    --semantic-status-success: #20C997;
    --semantic-status-successBg: #0A4034;
    --semantic-status-error: #FA5252;
    --semantic-status-errorBg: #4A1C1C;
    --semantic-status-warning: #FCC419;
    --semantic-status-warningBg: #4A3C0F;
    --semantic-status-info: #339AF0;
    --semantic-status-infoBg: #1A3A52;
    
    /* Shadows */
    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.3);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4);
    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
    --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
}

/* ============================================
   Theme Overrides (Service-specific - Light Mode)
   ============================================ */
`;

        for (const [themeName, themeVars] of themes) {
            if (themeVars.size > 0) {
                cssContent += `[data-service="${themeName}"] {\n`;
                const sortedThemeVars = Array.from(themeVars.entries()).sort();
                for (const [key, value] of sortedThemeVars) {
                    cssContent += `    ${key}: ${value};\n`;
                }
                cssContent += `}\n\n`;
            }
        }

        cssContent += `/* ============================================
   Theme Overrides (Service-specific - Dark Mode)
   ============================================ */
`;

        for (const [themeName, themeDarkVars] of themeDarkModes) {
            if (themeDarkVars.size > 0) {
                cssContent += `[data-service="${themeName}"][data-theme="dark"] {\n`;
                const sortedDarkVars = Array.from(themeDarkVars.entries()).sort();
                for (const [key, value] of sortedDarkVars) {
                    cssContent += `    ${key}: ${value};\n`;
                }
                cssContent += `}\n\n`;
            }
        }

        cssContent += `/* ============================================
   Theme Overrides (Dark-first themes - Light Mode)
   Portal uses dark as default, light as override
   ============================================ */
`;

        for (const [themeName, themeLightVars] of themeLightModes) {
            if (themeLightVars.size > 0) {
                cssContent += `[data-service="${themeName}"][data-theme="light"] {\n`;
                const sortedLightVars = Array.from(themeLightVars.entries()).sort();
                for (const [key, value] of sortedLightVars) {
                    cssContent += `    ${key}: ${value};\n`;
                }
                cssContent += `}\n\n`;
            }
        }

        cssContent += `/* ============================================
   Base Styles
   ============================================ */
@layer base {
    body {
        @apply bg-bg-page text-text-body;
        @apply font-sans antialiased;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
    }

    h1, h2, h3, h4, h5, h6 {
        @apply font-semibold;
        text-wrap: balance;
    }

    code {
        @apply font-mono text-sm;
    }

    a {
        @apply transition-colors duration-200;
    }
}

/* ============================================
   Utility Classes
   ============================================ */
@layer utilities {
    .text-balance {
        text-wrap: balance;
    }

    .text-pretty {
        text-wrap: pretty;
    }
}
`;

        writeFileSync(outputFile, cssContent, 'utf-8');
        console.log(`✅ CSS variables written to: ${outputFile}`);
        console.log(`📊 Total base variables: ${cssVariables.size}`);
        console.log(`🎨 Service themes generated: ${themes.size}`);
        console.log(`🌙 Dark mode overrides: ${themeDarkModes.size}`);
        console.log(`☀️  Light mode overrides: ${themeLightModes.size}`);
        console.log('\n✨ Design tokens built successfully with Linear theme support!');

    } catch (error) {
        console.error('❌ Fatal error:', error.message);
        process.exit(1);
    }
}

buildTokens();
