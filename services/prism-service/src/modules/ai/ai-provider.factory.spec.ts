import { AIProviderFactory } from './ai-provider.factory';
import { ProviderType } from '../provider/provider.entity';
import { OpenAIProvider } from './providers/openai.provider';
import { AnthropicProvider } from './providers/anthropic.provider';
import { OllamaProvider } from './providers/ollama.provider';

describe('AIProviderFactory', () => {
  const apiKey = 'test-api-key';
  const baseUrl = 'https://custom.api.com';

  it('should create OpenAIProvider for OPENAI type', () => {
    const provider = AIProviderFactory.create(ProviderType.OPENAI, apiKey, baseUrl);
    expect(provider).toBeInstanceOf(OpenAIProvider);
  });

  it('should create AnthropicProvider for ANTHROPIC type', () => {
    const provider = AIProviderFactory.create(
      ProviderType.ANTHROPIC,
      apiKey,
      baseUrl,
    );
    expect(provider).toBeInstanceOf(AnthropicProvider);
  });

  it('should create OpenAIProvider for AZURE_OPENAI type', () => {
    const provider = AIProviderFactory.create(
      ProviderType.AZURE_OPENAI,
      apiKey,
      baseUrl,
    );
    expect(provider).toBeInstanceOf(OpenAIProvider);
  });

  it('should create OllamaProvider for OLLAMA type', () => {
    const provider = AIProviderFactory.create(ProviderType.OLLAMA, apiKey, baseUrl);
    expect(provider).toBeInstanceOf(OllamaProvider);
  });

  it('should create OllamaProvider for LOCAL type', () => {
    const provider = AIProviderFactory.create(ProviderType.LOCAL, apiKey, baseUrl);
    expect(provider).toBeInstanceOf(OllamaProvider);
  });

  it('should create provider without baseUrl (null)', () => {
    const provider = AIProviderFactory.create(ProviderType.OPENAI, apiKey, null);
    expect(provider).toBeInstanceOf(OpenAIProvider);
  });

  it('should create provider without baseUrl (undefined)', () => {
    const provider = AIProviderFactory.create(ProviderType.OPENAI, apiKey);
    expect(provider).toBeInstanceOf(OpenAIProvider);
  });

  it('should throw for unsupported provider type', () => {
    expect(() =>
      AIProviderFactory.create('UNKNOWN' as ProviderType, apiKey),
    ).toThrow();
  });
});
