import { OpenAIProvider } from './openai.provider';
import { LLMRequest } from './llm-provider.interface';

// Mock the openai module
jest.mock('openai', () => {
  const mockCreate = jest.fn();
  const mockListModels = jest.fn();
  return {
    __esModule: true,
    default: jest.fn().mockImplementation(() => ({
      chat: {
        completions: {
          create: mockCreate,
        },
      },
      models: {
        list: mockListModels,
      },
    })),
    _mockCreate: mockCreate,
    _mockListModels: mockListModels,
  };
});

const { _mockCreate: mockCreate, _mockListModels: mockListModels } =
  jest.requireMock('openai');

describe('OpenAIProvider', () => {
  let provider: OpenAIProvider;

  const sampleRequest: LLMRequest = {
    systemPrompt: 'You are a helpful assistant.',
    userPrompt: 'Hello, how are you?',
    model: 'gpt-4o',
    temperature: 0.7,
    maxTokens: 4096,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    provider = new OpenAIProvider('test-api-key', 'https://api.openai.com/v1');
  });

  it('should generate a response with correct token counts', async () => {
    mockCreate.mockResolvedValue({
      choices: [{ message: { content: 'Hello! I am fine.' } }],
      usage: { prompt_tokens: 20, completion_tokens: 10 },
      model: 'gpt-4o',
    });

    const result = await provider.generate(sampleRequest);

    expect(result.content).toBe('Hello! I am fine.');
    expect(result.inputTokens).toBe(20);
    expect(result.outputTokens).toBe(10);
    expect(result.model).toBe('gpt-4o');

    expect(mockCreate).toHaveBeenCalledWith({
      model: 'gpt-4o',
      messages: [
        { role: 'system', content: 'You are a helpful assistant.' },
        { role: 'user', content: 'Hello, how are you?' },
      ],
      temperature: 0.7,
      max_tokens: 4096,
    });
  });

  it('should handle missing usage data gracefully', async () => {
    mockCreate.mockResolvedValue({
      choices: [{ message: { content: 'response' } }],
      usage: undefined,
      model: 'gpt-4o',
    });

    const result = await provider.generate(sampleRequest);

    expect(result.inputTokens).toBe(0);
    expect(result.outputTokens).toBe(0);
  });

  it('should list models filtered to gpt and o1 prefix', async () => {
    mockListModels.mockResolvedValue({
      data: [
        { id: 'gpt-4o' },
        { id: 'gpt-3.5-turbo' },
        { id: 'o1-mini' },
        { id: 'dall-e-3' },
        { id: 'text-embedding-ada-002' },
      ],
    });

    const models = await provider.listModels();

    expect(models).toEqual(['gpt-3.5-turbo', 'gpt-4o', 'o1-mini']);
    expect(models).not.toContain('dall-e-3');
  });

  it('should return true on successful connection test', async () => {
    mockListModels.mockResolvedValue({ data: [{ id: 'gpt-4o' }] });

    const result = await provider.testConnection();

    expect(result).toBe(true);
  });

  it('should return false on failed connection test', async () => {
    mockListModels.mockRejectedValue(new Error('Unauthorized'));

    const result = await provider.testConnection();

    expect(result).toBe(false);
  });
});
