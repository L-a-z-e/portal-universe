export interface LLMResponse {
  content: string;
  inputTokens: number;
  outputTokens: number;
  model: string;
}

export interface LLMRequest {
  systemPrompt: string;
  userPrompt: string;
  model: string;
  temperature: number;
  maxTokens: number;
}

export interface LLMProvider {
  generate(request: LLMRequest): Promise<LLMResponse>;
  listModels(): Promise<string[]>;
  testConnection(): Promise<boolean>;
}
