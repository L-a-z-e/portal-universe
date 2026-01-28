import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { join, resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const tokensDir = resolve(__dirname, '../src/tokens');
const distDir = resolve(__dirname, '../dist');

// Ensure dist directory exists
if (!existsSync(distDir)) {
    mkdirSync(distDir, { recursive: true });
}

/**
 * Flatten tokens recursively into CSS variables
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
 * Resolve color reference {color.xxx.yyy} to actual value
 */
function resolveColorReference(value, colorReferences) {
    if (typeof value !== 'string') return value;

    const refMatch = value.match(/^\{([^}]+)\}$/);
    if (!refMatch) return value;

    const refPath = refMatch[1];
    const resolved = colorReferences[refPath];

    return resolved || value;
}

/**
 * Process theme colors to CSS variables (with darkMode support)
 */
function processThemeColors(obj, targetMap, colorReferences, prefix = '--semantic', parentKey = '') {
    for (const [key, value] of Object.entries(obj)) {
        if (key.startsWith('$') || key === 'darkMode' || key === 'lightMode') continue;

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
 * Build tokens and output to multiple formats
 */
function buildTokens() {
    const cssVariables = new Map();
    const themes = new Map();
    const themeDarkModes = new Map();
    const themeLightModes = new Map();
    const colorReferences = {};
    const jsonOutput = {
        base: {},
        semantic: {},
        themes: {}
    };

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
                jsonOutput.base[tokenType] = tokens;
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
                        cssVariables.set(varName, resolved);
                    } else if (typeof value === 'string') {
                        const resolved = resolveColorReference(value, colorReferences);
                        cssVariables.set(varName, resolved);
                    }
                }
            }

            processSemantic(semanticContent);
            jsonOutput.semantic = semanticTokens;
            console.log('  ✅ semantic/colors.json loaded');
        } catch (err) {
            console.log(`  ⚠️  semantic/colors.json not found or invalid: ${err.message}`);
        }

        console.log('📖 Step 4: Reading theme tokens (with darkMode/lightMode support)...');
        const themeFiles = ['portal', 'blog', 'shopping', 'prism'];

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

                    themes.set(themeName, themeDarkVars);
                    if (themeLightVars.size > 0) {
                        themeLightModes.set(themeName, themeLightVars);
                    }
                } else {
                    // Other themes use light-first approach (darkMode section)
                    processThemeColors(
                        themeTokens.color || themeTokens,
                        themeLightVars,
                        colorReferences,
                        '--semantic'
                    );

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

                jsonOutput.themes[themeName] = themeTokens;
                console.log(`  ✅ themes/${themeName}.json loaded (${themeName === 'portal' ? 'dark-first' : 'light-first'})`);
            } catch (err) {
                console.log(`  ⚠️  themes/${themeName}.json not found or invalid: ${err.message}`);
            }
        });

        console.log('\n🎨 Step 5: Generating output files...');

        // Generate CSS
        let cssContent = `/* ============================================
   @portal/design-tokens - Auto-generated CSS Variables
   Linear-inspired theme - DO NOT EDIT MANUALLY
   ============================================ */

@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
`;

        const sortedVars = Array.from(cssVariables.entries()).sort();
        for (const [key, value] of sortedVars) {
            cssContent += `    ${key}: ${value};\n`;
        }

        cssContent += `}

/* Dark Mode Overrides (Global Default) */
[data-theme="dark"] {
    --semantic-brand-primary: #20C997;
    --semantic-brand-primaryHover: #12B886;
    --semantic-brand-secondary: #63E6BE;

    --semantic-text-heading: #F0F6FC;
    --semantic-text-body: #C9D1D9;
    --semantic-text-meta: #8B949E;
    --semantic-text-muted: #6E7681;
    --semantic-text-inverse: #111827;
    --semantic-text-link: #58A6FF;
    --semantic-text-linkHover: #A5D6FF;

    --semantic-bg-page: #0F1419;
    --semantic-bg-card: #1C2128;
    --semantic-bg-elevated: #2D333B;
    --semantic-bg-muted: #22272E;
    --semantic-bg-hover: #2D333B;

    --semantic-border-default: #444C56;
    --semantic-border-hover: #6E7681;
    --semantic-border-muted: #2C2C2C;
    --semantic-border-focus: #58A6FF;

    --semantic-status-success: #20C997;
    --semantic-status-successBg: #0A4034;
    --semantic-status-error: #FA5252;
    --semantic-status-errorBg: #4A1C1C;
    --semantic-status-warning: #FCC419;
    --semantic-status-warningBg: #4A3C0F;
    --semantic-status-info: #339AF0;
    --semantic-status-infoBg: #1A3A52;

    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.3);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4);
    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
    --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
}

/* Theme Overrides (Service-specific - Light Mode) */
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

        cssContent += `/* Theme Overrides (Service-specific - Dark Mode) */\n`;

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

        cssContent += `/* Theme Overrides (Dark-first themes - Light Mode) */\n`;

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

        cssContent += `/* Base Styles */
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

@layer utilities {
    .text-balance {
        text-wrap: balance;
    }

    .text-pretty {
        text-wrap: pretty;
    }
}
`;

        // Write CSS file
        writeFileSync(join(distDir, 'tokens.css'), cssContent, 'utf-8');
        console.log('  ✅ tokens.css generated');

        // Write JSON file
        writeFileSync(join(distDir, 'tokens.json'), JSON.stringify(jsonOutput, null, 2), 'utf-8');
        console.log('  ✅ tokens.json generated');

        // Write JavaScript module
        const jsContent = `// Auto-generated token exports
export const tokens = ${JSON.stringify(jsonOutput, null, 2)};

export const cssVariables = ${JSON.stringify(Object.fromEntries(cssVariables), null, 2)};

export default tokens;
`;
        writeFileSync(join(distDir, 'tokens.js'), jsContent, 'utf-8');
        console.log('  ✅ tokens.js generated');

        // Write CommonJS module
        const cjsContent = `// Auto-generated token exports (CommonJS)
const tokens = ${JSON.stringify(jsonOutput, null, 2)};

const cssVariables = ${JSON.stringify(Object.fromEntries(cssVariables), null, 2)};

module.exports = { tokens, cssVariables, default: tokens };
`;
        writeFileSync(join(distDir, 'tokens.cjs'), cjsContent, 'utf-8');
        console.log('  ✅ tokens.cjs generated');

        // Write TypeScript declarations
        const dtsContent = `// Auto-generated type declarations
export interface Tokens {
    base: {
        colors?: Record<string, unknown>;
        typography?: Record<string, unknown>;
        spacing?: Record<string, unknown>;
        border?: Record<string, unknown>;
        effects?: Record<string, unknown>;
    };
    semantic: Record<string, unknown>;
    themes: {
        portal?: Record<string, unknown>;
        blog?: Record<string, unknown>;
        shopping?: Record<string, unknown>;
    };
}

export declare const tokens: Tokens;
export declare const cssVariables: Record<string, string>;
export default tokens;
`;
        writeFileSync(join(distDir, 'tokens.d.ts'), dtsContent, 'utf-8');
        console.log('  ✅ tokens.d.ts generated');

        console.log(`\n📊 Summary:`);
        console.log(`   Total base variables: ${cssVariables.size}`);
        console.log(`   Service themes generated: ${themes.size}`);
        console.log(`   Dark mode overrides: ${themeDarkModes.size}`);
        console.log(`   Light mode overrides: ${themeLightModes.size}`);
        console.log('\n✨ Design tokens built successfully!');

    } catch (error) {
        console.error('❌ Fatal error:', error.message);
        process.exit(1);
    }
}

buildTokens();
