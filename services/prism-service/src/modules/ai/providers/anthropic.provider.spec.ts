import { AnthropicProvider } from './anthropic.provider';
import { LLMRequest } from './llm-provider.interface';

// Mock the @anthropic-ai/sdk module
jest.mock('@anthropic-ai/sdk', () => {
  const mockCreate = jest.fn();
  return {
    __esModule: true,
    default: jest.fn().mockImplementation(() => ({
      messages: {
        create: mockCreate,
      },
    })),
    _mockCreate: mockCreate,
  };
});

const { _mockCreate: mockCreate } = jest.requireMock('@anthropic-ai/sdk');

describe('AnthropicProvider', () => {
  let provider: AnthropicProvider;

  const sampleRequest: LLMRequest = {
    systemPrompt: 'You are Claude.',
    userPrompt: 'Hello!',
    model: 'claude-sonnet-4-20250514',
    temperature: 0.7,
    maxTokens: 4096,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    provider = new AnthropicProvider('test-api-key');
  });

  it('should generate a response with correct token counts', async () => {
    mockCreate.mockResolvedValue({
      content: [{ type: 'text', text: 'Hello! I am Claude.' }],
      usage: { input_tokens: 15, output_tokens: 8 },
      model: 'claude-sonnet-4-20250514',
    });

    const result = await provider.generate(sampleRequest);

    expect(result.content).toBe('Hello! I am Claude.');
    expect(result.inputTokens).toBe(15);
    expect(result.outputTokens).toBe(8);
    expect(result.model).toBe('claude-sonnet-4-20250514');

    expect(mockCreate).toHaveBeenCalledWith({
      model: 'claude-sonnet-4-20250514',
      system: 'You are Claude.',
      messages: [{ role: 'user', content: 'Hello!' }],
      temperature: 0.7,
      max_tokens: 4096,
    });
  });

  it('should handle non-text content blocks', async () => {
    mockCreate.mockResolvedValue({
      content: [{ type: 'tool_use', id: 'call_1' }],
      usage: { input_tokens: 10, output_tokens: 5 },
      model: 'claude-sonnet-4-20250514',
    });

    const result = await provider.generate(sampleRequest);

    expect(result.content).toBe('');
  });

  it('should return hardcoded model list', async () => {
    const models = await provider.listModels();

    expect(models).toEqual([
      'claude-opus-4-20250514',
      'claude-sonnet-4-20250514',
      'claude-3-7-sonnet-20250219',
      'claude-3-5-haiku-20241022',
    ]);
  });

  it('should return true on successful connection test', async () => {
    mockCreate.mockResolvedValue({
      content: [{ type: 'text', text: 'Hi' }],
      usage: { input_tokens: 1, output_tokens: 1 },
      model: 'claude-3-5-haiku-20241022',
    });

    const result = await provider.testConnection();

    expect(result).toBe(true);
    expect(mockCreate).toHaveBeenCalledWith({
      model: 'claude-3-5-haiku-20241022',
      max_tokens: 10,
      messages: [{ role: 'user', content: 'Hi' }],
    });
  });

  it('should return false on connection test failure', async () => {
    mockCreate.mockRejectedValue(new Error('Invalid API key'));

    const result = await provider.testConnection();

    expect(result).toBe(false);
  });
});
