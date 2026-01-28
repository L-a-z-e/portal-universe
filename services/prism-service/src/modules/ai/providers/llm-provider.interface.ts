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
  /**
   * Generate a completion from the LLM
   */
  generate(request: LLMRequest): Promise<LLMResponse>;

  /**
   * List available models
   */
  listModels(): Promise<string[]>;

  /**
   * Test connection to the provider
   */
  testConnection(): Promise<boolean>;
}
