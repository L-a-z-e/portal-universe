import OpenAI from 'openai';
import { LLMProvider, LLMRequest, LLMResponse } from './llm-provider.interface';

interface OllamaModel {
  name: string;
  model: string;
  modified_at: string;
  size: number;
}

interface OllamaTagsResponse {
  models: OllamaModel[];
}

export class OllamaProvider implements LLMProvider {
  private client: OpenAI;
  private baseUrl: string;

  constructor(baseUrl?: string | null) {
    this.baseUrl = baseUrl || 'http://localhost:11434';
    this.client = new OpenAI({
      apiKey: 'ollama',
      baseURL: `${this.baseUrl}/v1`,
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
    const response = await fetch(`${this.baseUrl}/api/tags`);
    if (!response.ok) {
      throw new Error(`Ollama API error: ${response.status}`);
    }
    const data: unknown = await response.json();
    if (!this.isOllamaTagsResponse(data)) {
      throw new Error('Unexpected Ollama API response format');
    }
    return data.models.map((m) => m.name).sort();
  }

  private isOllamaTagsResponse(data: unknown): data is OllamaTagsResponse {
    return (
      typeof data === 'object' &&
      data !== null &&
      'models' in data &&
      Array.isArray((data as OllamaTagsResponse).models)
    );
  }

  async testConnection(): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseUrl}/api/tags`);
      return response.ok;
    } catch {
      return false;
    }
  }
}
