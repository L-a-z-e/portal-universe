import { ProviderType } from '../provider/provider.entity';
import { LLMProvider } from './providers/llm-provider.interface';
import { OpenAIProvider } from './providers/openai.provider';
import { AnthropicProvider } from './providers/anthropic.provider';
import { OllamaProvider } from './providers/ollama.provider';
import { BusinessException } from '../../common/filters/business.exception';

export class AIProviderFactory {
  static create(
    type: ProviderType,
    apiKey: string,
    baseUrl?: string | null,
  ): LLMProvider {
    switch (type) {
      case ProviderType.OPENAI:
        return new OpenAIProvider(apiKey, baseUrl);
      case ProviderType.ANTHROPIC:
        return new AnthropicProvider(apiKey, baseUrl);
      case ProviderType.AZURE_OPENAI:
        // Azure uses OpenAI SDK with different base URL
        return new OpenAIProvider(apiKey, baseUrl);
      case ProviderType.OLLAMA:
      case ProviderType.LOCAL:
        return new OllamaProvider(baseUrl);
      default: {
        const exhaustiveCheck: never = type;
        throw BusinessException.providerConnectionFailed(
          `Unknown provider type: ${String(exhaustiveCheck)}`,
        );
      }
    }
  }
}
