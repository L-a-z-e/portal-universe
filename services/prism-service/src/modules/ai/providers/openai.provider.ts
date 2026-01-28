import OpenAI from 'openai';
import { LLMProvider, LLMRequest, LLMResponse } from './llm-provider.interface';

export class OpenAIProvider implements LLMProvider {
  private client: OpenAI;

  constructor(apiKey: string, baseUrl?: string | null) {
    this.client = new OpenAI({
      apiKey,
      baseURL: baseUrl || undefined,
    });
  }

  async generate(request: LLMRequest): Promise<LLMResponse> {
    const response = await this.client.chat.completions.create({
      model: request.model,
      messages: [
        { role: 'system', content: request.systemPrompt },
        { role: 'user', content: request.userPrompt },
      ],
      temperature: request.temperature,
      max_tokens: request.maxTokens,
    });

    const choice = response.choices[0];
    const content = choice?.message?.content || '';

    return {
      content,
      inputTokens: response.usage?.prompt_tokens || 0,
      outputTokens: response.usage?.completion_tokens || 0,
      model: response.model,
    };
  }

  async listModels(): Promise<string[]> {
    const response = await this.client.models.list();
    return response.data
      .filter((m) => m.id.startsWith('gpt') || m.id.startsWith('o1'))
      .map((m) => m.id)
      .sort();
  }

  async testConnection(): Promise<boolean> {
    try {
      await this.client.models.list();
      return true;
    } catch {
      return false;
    }
  }
}
