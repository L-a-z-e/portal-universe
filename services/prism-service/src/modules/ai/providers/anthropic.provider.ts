import Anthropic from '@anthropic-ai/sdk';
import { LLMProvider, LLMRequest, LLMResponse } from './llm-provider.interface';

export class AnthropicProvider implements LLMProvider {
  private client: Anthropic;

  constructor(apiKey: string, baseUrl?: string | null) {
    this.client = new Anthropic({
      apiKey,
      baseURL: baseUrl || undefined,
    });
  }

  async generate(request: LLMRequest): Promise<LLMResponse> {
    const response = await this.client.messages.create({
      model: request.model,
      system: request.systemPrompt,
      messages: [{ role: 'user', content: request.userPrompt }],
      temperature: request.temperature,
      max_tokens: request.maxTokens,
    });

    const content =
      response.content[0]?.type === 'text' ? response.content[0].text : '';

    return {
      content,
      inputTokens: response.usage.input_tokens,
      outputTokens: response.usage.output_tokens,
      model: response.model,
    };
  }

  listModels(): Promise<string[]> {
    // Anthropic doesn't have a models endpoint, return hardcoded list
    return Promise.resolve([
      'claude-opus-4-20250514',
      'claude-sonnet-4-20250514',
      'claude-3-7-sonnet-20250219',
      'claude-3-5-haiku-20241022',
    ]);
  }

  async testConnection(): Promise<boolean> {
    try {
      // Send a minimal request to test connection
      await this.client.messages.create({
        model: 'claude-3-5-haiku-20241022',
        max_tokens: 10,
        messages: [{ role: 'user', content: 'Hi' }],
      });
      return true;
    } catch {
      return false;
    }
  }
}
