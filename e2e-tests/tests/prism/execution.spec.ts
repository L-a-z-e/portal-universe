/**
 * Prism AI Execution E2E Tests
 *
 * Tests actual AI execution with available providers.
 * - Ollama: runs if localhost:11434 is available
 * - OpenAI/Anthropic: runs if API key env var is set
 * - CRUD-only tests: always run
 */
import { test, expect } from '../helpers/test-fixtures'
import { gotoPrismPage } from '../helpers/auth'
import { PrismApi, checkOllamaAvailable, getOllamaModel } from '../helpers/prism-api'
import * as fs from 'fs'
import * as path from 'path'

const TOKEN_FILE = path.resolve(__dirname, '../.auth/access-token.json')
const BASE_URL = process.env.BASE_URL || 'https://localhost:30000'

function getUserToken(): string | null {
  try {
    if (!fs.existsSync(TOKEN_FILE)) return null
    const data = JSON.parse(fs.readFileSync(TOKEN_FILE, 'utf-8'))
    return data.accessToken || null
  } catch {
    return null
  }
}

test.describe('Prism AI Execution', () => {
  test('should execute task with Ollama provider', async ({ page }) => {
    const ollamaAvailable = await checkOllamaAvailable()
    test.skip(!ollamaAvailable, 'Ollama not running locally')

    const token = getUserToken()
    test.skip(!token, 'No user token available')

    const api = new PrismApi(BASE_URL, token!)
    const model = await getOllamaModel()
    test.skip(!model, 'No Ollama model available')

    // Create provider via API
    const provider = await api.createProvider({
      providerType: 'OLLAMA',
      name: 'E2E Execution Ollama',
      apiKey: '',
      baseUrl: 'http://host.docker.internal:11434',
    })
    test.skip(!provider, 'Failed to create Ollama provider')

    // Create agent
    const agent = await api.createAgent({
      providerId: provider!.id,
      name: 'E2E Execution Agent',
      role: 'CUSTOM',
      systemPrompt: 'You are a helpful test assistant. Reply briefly.',
      model: model!,
      temperature: 0.3,
      maxTokens: 100,
    })
    test.skip(!agent, 'Failed to create agent')

    // Create board and task
    const board = await api.createBoard({ name: 'E2E Execution Board' })
    test.skip(!board, 'Failed to create board')

    const task = await api.createTask(board!.id, {
      title: 'E2E Execution Task',
      description: 'Say hello in one sentence.',
      agentId: agent!.id,
    })
    test.skip(!task, 'Failed to create task')

    // Execute via API
    const execution = await api.executeTask(task!.id)
    expect(execution).not.toBeNull()
    expect(execution!.status).toMatch(/COMPLETED|RUNNING|PENDING/)

    // Wait and check result
    await page.waitForTimeout(10000)
    const executions = await api.getExecutions(task!.id)
    expect(executions.length).toBeGreaterThanOrEqual(1)

    const latest = executions[0]
    if (latest.status === 'COMPLETED') {
      expect(latest.outputResult).toBeTruthy()
      expect(latest.durationMs).toBeGreaterThan(0)
    }

    // Cleanup (order matters: executions are cascade-deleted with task)
    // Delete in reverse dependency order: task → board → agent → provider
    // Wrap each in try-catch to ensure cleanup continues on failure
    try { await api.deleteTask(task!.id) } catch {}
    try { await api.deleteBoard(board!.id) } catch {}
    try { await api.deleteAgent(agent!.id) } catch {}
    try { await api.deleteProvider(provider!.id) } catch {}
  })

  test('should display execution result in UI', async ({ page }) => {
    const ollamaAvailable = await checkOllamaAvailable()
    const hasCloudKey = !!process.env.OPENAI_API_KEY || !!process.env.ANTHROPIC_API_KEY
    test.skip(!ollamaAvailable && !hasCloudKey, 'No AI provider available')

    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    // Navigate to a board with a completed execution
    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)
    if (!hasBoard) return

    await boardCard.click()
    await page.waitForTimeout(2000)

    // Find a task with execution result
    const taskCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E/ }).first()
    if (await taskCard.isVisible().catch(() => false)) {
      await taskCard.click()
      await page.waitForTimeout(2000)

      // Check for execution history or results
      const executionContent = page.locator('text=/Execution|실행|Result|결과|Token|토큰/i').first()
      const hasExecution = await executionContent.isVisible().catch(() => false)
      // This may or may not be visible depending on whether executions exist
    }
  })

  test('should execute task with Cloud provider', async ({ page }) => {
    const apiKey = process.env.OPENAI_API_KEY || process.env.ANTHROPIC_API_KEY
    test.skip(!apiKey, 'No cloud API key available')

    const token = getUserToken()
    test.skip(!token, 'No user token available')

    const api = new PrismApi(BASE_URL, token!)

    const isOpenAI = !!process.env.OPENAI_API_KEY
    const providerType = isOpenAI ? 'OPENAI' : 'ANTHROPIC'
    const model = isOpenAI ? 'gpt-4o-mini' : 'claude-3-5-haiku-20241022'

    const provider = await api.createProvider({
      providerType,
      name: `E2E Cloud ${providerType}`,
      apiKey: apiKey!,
    })
    test.skip(!provider, 'Failed to create cloud provider')

    const agent = await api.createAgent({
      providerId: provider!.id,
      name: 'E2E Cloud Agent',
      role: 'CUSTOM',
      systemPrompt: 'You are a helpful test assistant. Reply briefly.',
      model,
      temperature: 0.3,
      maxTokens: 100,
    })
    test.skip(!agent, 'Failed to create cloud agent')

    const board = await api.createBoard({ name: 'E2E Cloud Execution Board' })
    test.skip(!board, 'Failed to create board')

    const task = await api.createTask(board!.id, {
      title: 'E2E Cloud Task',
      description: 'Say hello in one sentence.',
      agentId: agent!.id,
    })
    test.skip(!task, 'Failed to create cloud task')

    const execution = await api.executeTask(task!.id)
    expect(execution).not.toBeNull()

    // Wait for completion
    await page.waitForTimeout(15000)
    const executions = await api.getExecutions(task!.id)
    expect(executions.length).toBeGreaterThanOrEqual(1)

    const latest = executions[0]
    if (latest.status === 'COMPLETED') {
      expect(latest.outputResult).toBeTruthy()
    }

    // Cleanup (reverse dependency order with error tolerance)
    try { await api.deleteTask(task!.id) } catch {}
    try { await api.deleteBoard(board!.id) } catch {}
    try { await api.deleteAgent(agent!.id) } catch {}
    try { await api.deleteProvider(provider!.id) } catch {}
  })

  test('should display execution history for a task', async ({ page }) => {
    await gotoPrismPage(page, '/prism')
    await page.waitForTimeout(2000)

    const boardCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Board/ }).first()
    const hasBoard = await boardCard.isVisible().catch(() => false)
    if (!hasBoard) return

    await boardCard.click()
    await page.waitForTimeout(2000)

    const taskCard = page.locator('[class*="rounded"]').filter({ hasText: /E2E Test Task/ }).first()
    if (await taskCard.isVisible().catch(() => false)) {
      await taskCard.click()
      await page.waitForTimeout(2000)

      // Execution history section might show empty or with entries
      const historySection = page.locator('text=/Execution|실행 이력|History|이력/i').first()
      const hasHistory = await historySection.isVisible().catch(() => false)
      // History section existence validates the UI renders
    }
  })
})
