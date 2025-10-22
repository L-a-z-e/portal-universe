import { existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const tokensPath = resolve(__dirname, '../src/tokens');

if (existsSync(tokensPath)) {
  console.log('✅ Design tokens built successfully!');
} else {
  console.log('⚠️  No token files found');
}
