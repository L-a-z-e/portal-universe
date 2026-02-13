#!/usr/bin/env node

/**
 * Event Contract Validation Script
 *
 * Validates that Java event records and TypeScript interfaces
 * match the JSON Schema definitions in services/event-contracts/schemas/.
 *
 * Usage: node scripts/validate-event-contracts.js
 *
 * Related: ADR-038 (Polyglot Event Contract Management)
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SCHEMAS_DIR = path.join(ROOT, 'services/event-contracts/schemas');

// ─── Contract Mapping Table ─────────────────────────────────────────────────
// Add new entries here when creating event contracts for other domains.

const CONTRACTS = [
  {
    schema: 'prism.task.completed.schema.json',
    java: 'services/prism-events/src/main/java/com/portal/universe/event/prism/PrismTaskCompletedEvent.java',
    typescript: 'services/prism-service/src/modules/event/kafka.producer.ts',
    tsInterfaceName: 'TaskEvent',
  },
  {
    schema: 'prism.task.failed.schema.json',
    java: 'services/prism-events/src/main/java/com/portal/universe/event/prism/PrismTaskFailedEvent.java',
    typescript: 'services/prism-service/src/modules/event/kafka.producer.ts',
    tsInterfaceName: 'TaskFailedEvent',
  },
];

// ─── Parsers ────────────────────────────────────────────────────────────────

/**
 * Extract field names from a Java record definition.
 * Handles: public record Foo(Integer taskId, String userId, ...) {}
 */
function parseJavaRecord(filePath) {
  const content = fs.readFileSync(path.join(ROOT, filePath), 'utf-8');
  const recordMatch = content.match(/public\s+record\s+\w+\s*\(([\s\S]*?)\)\s*\{/);
  if (!recordMatch) {
    throw new Error(`No record definition found in ${filePath}`);
  }

  const params = recordMatch[1];
  const fields = [];

  for (const line of params.split(',')) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    // Match: Type fieldName  or  @Annotation Type fieldName
    const fieldMatch = trimmed.match(/(\w+)\s*$/);
    if (fieldMatch) {
      fields.push(fieldMatch[1]);
    }
  }

  return fields;
}

/**
 * Extract field names from a TypeScript interface definition.
 * Handles both standalone and extended interfaces:
 *   interface TaskEvent { ... }
 *   interface TaskFailedEvent extends TaskEvent { ... }
 */
function parseTypeScriptInterface(filePath, interfaceName) {
  const content = fs.readFileSync(path.join(ROOT, filePath), 'utf-8');
  const fields = [];

  // Match: export interface Name (extends Parent)? { ... }
  const interfaceRegex = new RegExp(
    `export\\s+interface\\s+${interfaceName}(?:\\s+extends\\s+(\\w+))?\\s*\\{([^}]*)\\}`,
    's'
  );
  const match = content.match(interfaceRegex);
  if (!match) {
    throw new Error(`Interface '${interfaceName}' not found in ${filePath}`);
  }

  const parentName = match[1];
  const body = match[2];

  // If extends another interface, get parent fields first
  if (parentName) {
    const parentFields = parseTypeScriptInterface(filePath, parentName);
    fields.push(...parentFields);
  }

  // Parse own fields: fieldName: type; or fieldName?: type;
  const fieldRegex = /(\w+)\??:\s*[\w\[\]|"' ]+;/g;
  let fieldMatch;
  while ((fieldMatch = fieldRegex.exec(body)) !== null) {
    fields.push(fieldMatch[1]);
  }

  return fields;
}

/**
 * Extract property names from a JSON Schema file.
 */
function parseJsonSchema(schemaFile) {
  const content = fs.readFileSync(path.join(SCHEMAS_DIR, schemaFile), 'utf-8');
  const schema = JSON.parse(content);

  if (!schema.properties) {
    throw new Error(`No properties in schema ${schemaFile}`);
  }

  return {
    allFields: Object.keys(schema.properties),
    requiredFields: schema.required || [],
    title: schema.title || schemaFile,
  };
}

// ─── Validation ─────────────────────────────────────────────────────────────

function validateContract(contract) {
  const errors = [];
  const schemaInfo = parseJsonSchema(contract.schema);
  const schemaFields = new Set(schemaInfo.allFields);

  // Validate Java
  try {
    const javaFields = parseJavaRecord(contract.java);
    const javaSet = new Set(javaFields);

    const missingInJava = schemaInfo.allFields.filter((f) => !javaSet.has(f));
    const extraInJava = javaFields.filter((f) => !schemaFields.has(f));

    if (missingInJava.length > 0) {
      errors.push(
        `  Java ${path.basename(contract.java)}: missing fields: [${missingInJava.join(', ')}]`
      );
    }
    if (extraInJava.length > 0) {
      errors.push(
        `  Java ${path.basename(contract.java)}: extra fields not in schema: [${extraInJava.join(', ')}]`
      );
    }
  } catch (e) {
    errors.push(`  Java parse error: ${e.message}`);
  }

  // Validate TypeScript
  try {
    const tsFields = parseTypeScriptInterface(
      contract.typescript,
      contract.tsInterfaceName
    );
    const tsSet = new Set(tsFields);

    const missingInTs = schemaInfo.allFields.filter((f) => !tsSet.has(f));
    const extraInTs = tsFields.filter((f) => !schemaFields.has(f));

    if (missingInTs.length > 0) {
      errors.push(
        `  TS ${contract.tsInterfaceName}: missing fields: [${missingInTs.join(', ')}]`
      );
    }
    if (extraInTs.length > 0) {
      errors.push(
        `  TS ${contract.tsInterfaceName}: extra fields not in schema: [${extraInTs.join(', ')}]`
      );
    }
  } catch (e) {
    errors.push(`  TS parse error: ${e.message}`);
  }

  return errors;
}

// ─── Topic Constants Validation ─────────────────────────────────────────────

function validateTopicConstants() {
  const errors = [];

  // Parse Java PrismTopics
  const javaTopicsPath = path.join(
    ROOT,
    'services/prism-events/src/main/java/com/portal/universe/event/prism/PrismTopics.java'
  );
  const javaContent = fs.readFileSync(javaTopicsPath, 'utf-8');
  const javaTopics = {};
  const javaTopicRegex = /public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]+)"/g;
  let jMatch;
  while ((jMatch = javaTopicRegex.exec(javaContent)) !== null) {
    javaTopics[jMatch[1]] = jMatch[2];
  }

  // Parse TypeScript PrismTopics
  const tsTopicsPath = path.join(
    ROOT,
    'services/prism-service/src/modules/event/prism-topics.ts'
  );
  const tsContent = fs.readFileSync(tsTopicsPath, 'utf-8');
  const tsTopics = {};
  const tsTopicRegex = /(\w+):\s*'([^']+)'/g;
  let tMatch;
  while ((tMatch = tsTopicRegex.exec(tsContent)) !== null) {
    tsTopics[tMatch[1]] = tMatch[2];
  }

  // Compare
  for (const [key, value] of Object.entries(javaTopics)) {
    if (!tsTopics[key]) {
      errors.push(`  Topic '${key}' exists in Java but missing in TypeScript`);
    } else if (tsTopics[key] !== value) {
      errors.push(
        `  Topic '${key}' mismatch: Java="${value}" vs TS="${tsTopics[key]}"`
      );
    }
  }

  for (const key of Object.keys(tsTopics)) {
    if (!javaTopics[key]) {
      errors.push(`  Topic '${key}' exists in TypeScript but missing in Java`);
    }
  }

  return errors;
}

// ─── Main ───────────────────────────────────────────────────────────────────

function main() {
  console.log('Event Contract Validation');
  console.log('='.repeat(60));

  let hasErrors = false;

  // 1. Validate topic constants
  console.log('\n[Topics] Checking PrismTopics (Java ↔ TypeScript)...');
  const topicErrors = validateTopicConstants();
  if (topicErrors.length > 0) {
    hasErrors = true;
    console.log('  FAIL:');
    topicErrors.forEach((e) => console.log(e));
  } else {
    console.log('  PASS: All topic constants match');
  }

  // 2. Validate event schemas
  for (const contract of CONTRACTS) {
    console.log(`\n[Schema] ${contract.schema}`);
    const errors = validateContract(contract);
    if (errors.length > 0) {
      hasErrors = true;
      console.log('  FAIL:');
      errors.forEach((e) => console.log(e));
    } else {
      console.log('  PASS: Java and TypeScript fields match schema');
    }
  }

  console.log('\n' + '='.repeat(60));
  if (hasErrors) {
    console.log('RESULT: FAIL - Event contract violations detected');
    process.exit(1);
  } else {
    console.log('RESULT: PASS - All event contracts valid');
    process.exit(0);
  }
}

main();
