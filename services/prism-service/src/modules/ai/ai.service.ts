import { Injectable } from '@nestjs/common';
import { ProviderService } from '../provider/provider.service';
import { AIProviderFactory } from './ai-provider.factory';
import { LLMRequest, LLMResponse } from './providers/llm-provider.interface';
import { BusinessException } from '../../common/filters/business.exception';

@Injectable()
export class AIService {
  constructor(private readonly providerService: ProviderService) {}

  async generate(
    userId: string,
    providerId: number,
    request: LLMRequest,
  ): Promise<LLMResponse> {
    const { provider, apiKey } =
      await this.providerService.getProviderWithApiKey(userId, providerId);

    if (!provider.isActive) {
      throw BusinessException.providerConnectionFailed('Provider is inactive');
    }

    const llmProvider = AIProviderFactory.create(
      provider.providerType,
      apiKey,
      provider.baseUrl,
    );

    try {
      return await llmProvider.generate(request);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'AI generation failed';
      throw BusinessException.aiExecutionFailed(message);
    }
  }

  async testConnection(userId: string, providerId: number): Promise<boolean> {
    const { provider, apiKey } =
      await this.providerService.getProviderWithApiKey(userId, providerId);

    const llmProvider = AIProviderFactory.create(
      provider.providerType,
      apiKey,
      provider.baseUrl,
    );

    return llmProvider.testConnection();
  }

  async listModels(userId: string, providerId: number): Promise<string[]> {
    const { provider, apiKey } =
      await this.providerService.getProviderWithApiKey(userId, providerId);

    const llmProvider = AIProviderFactory.create(
      provider.providerType,
      apiKey,
      provider.baseUrl,
    );

    return llmProvider.listModels();
  }
}
