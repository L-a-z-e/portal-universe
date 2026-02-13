import { AIService } from './ai.service';
import { ProviderService } from '../provider/provider.service';
import { ProviderType } from '../provider/provider.entity';
import { BusinessException } from '../../common/filters/business.exception';
import { LLMRequest } from './providers/llm-provider.interface';

// Mock the AI providers
jest.mock('./providers/openai.provider', () => ({
  OpenAIProvider: jest.fn().mockImplementation(() => ({
    generate: jest.fn().mockResolvedValue({
      content: 'AI response',
      inputTokens: 10,
      outputTokens: 5,
      model: 'gpt-4o',
    }),
    testConnection: jest.fn().mockResolvedValue(true),
    listModels: jest.fn().mockResolvedValue(['gpt-4o', 'gpt-3.5-turbo']),
  })),
}));

jest.mock('./providers/anthropic.provider', () => ({
  AnthropicProvider: jest.fn().mockImplementation(() => ({
    generate: jest.fn().mockResolvedValue({
      content: 'Claude response',
      inputTokens: 8,
      outputTokens: 4,
      model: 'claude-sonnet-4-20250514',
    }),
    testConnection: jest.fn().mockResolvedValue(true),
    listModels: jest.fn().mockResolvedValue(['claude-sonnet-4-20250514']),
  })),
}));

jest.mock('./providers/ollama.provider', () => ({
  OllamaProvider: jest.fn().mockImplementation(() => ({
    generate: jest.fn(),
    testConnection: jest.fn(),
    listModels: jest.fn(),
  })),
}));

describe('AIService', () => {
  let service: AIService;
  let providerService: jest.Mocked<
    Pick<ProviderService, 'getProviderWithApiKey'>
  >;

  const sampleRequest: LLMRequest = {
    systemPrompt: 'You are helpful.',
    userPrompt: 'Hello',
    model: 'gpt-4o',
    temperature: 0.7,
    maxTokens: 4096,
  };

  beforeEach(() => {
    providerService = {
      getProviderWithApiKey: jest.fn(),
    };

    service = new AIService(providerService as unknown as ProviderService);
  });

  it('should generate using the correct provider', async () => {
    providerService.getProviderWithApiKey.mockResolvedValue({
      provider: {
        id: 1,
        providerType: ProviderType.OPENAI,
        isActive: true,
        baseUrl: null,
      } as any,
      apiKey: 'sk-test',
    });

    const result = await service.generate('user-1', 1, sampleRequest);

    expect(result.content).toBe('AI response');
    expect(providerService.getProviderWithApiKey).toHaveBeenCalledWith(
      'user-1',
      1,
    );
  });

  it('should throw when provider is inactive', async () => {
    providerService.getProviderWithApiKey.mockResolvedValue({
      provider: {
        id: 1,
        providerType: ProviderType.OPENAI,
        isActive: false,
        baseUrl: null,
      } as any,
      apiKey: 'sk-test',
    });

    await expect(service.generate('user-1', 1, sampleRequest)).rejects.toThrow(
      BusinessException,
    );
  });

  it('should delegate testConnection to the LLM provider', async () => {
    providerService.getProviderWithApiKey.mockResolvedValue({
      provider: {
        id: 1,
        providerType: ProviderType.OPENAI,
        isActive: true,
        baseUrl: null,
      } as any,
      apiKey: 'sk-test',
    });

    const result = await service.testConnection('user-1', 1);

    expect(result).toBe(true);
  });

  it('should delegate listModels to the LLM provider', async () => {
    providerService.getProviderWithApiKey.mockResolvedValue({
      provider: {
        id: 1,
        providerType: ProviderType.OPENAI,
        isActive: true,
        baseUrl: null,
      } as any,
      apiKey: 'sk-test',
    });

    const models = await service.listModels('user-1', 1);

    expect(models).toEqual(['gpt-4o', 'gpt-3.5-turbo']);
  });

  it('should wrap AI generation error as BusinessException', async () => {
    // Use a fresh mock that throws
    providerService.getProviderWithApiKey.mockResolvedValue({
      provider: {
        id: 1,
        providerType: ProviderType.ANTHROPIC,
        isActive: true,
        baseUrl: null,
      } as any,
      apiKey: 'sk-test',
    });

    // Override the mocked AnthropicProvider's generate to throw
    const { AnthropicProvider } = jest.requireMock(
      './providers/anthropic.provider',
    );
    AnthropicProvider.mockImplementationOnce(() => ({
      generate: jest.fn().mockRejectedValue(new Error('Rate limit exceeded')),
      testConnection: jest.fn(),
      listModels: jest.fn(),
    }));

    // Re-create service to pick up the override
    service = new AIService(providerService as unknown as ProviderService);

    await expect(service.generate('user-1', 1, sampleRequest)).rejects.toThrow(
      BusinessException,
    );
  });
});
