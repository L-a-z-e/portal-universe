import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { AIProvider, ProviderType } from '../provider.entity';

export class ProviderResponseDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 1 })
  userId: string;

  @ApiProperty({ enum: ProviderType, example: ProviderType.OPENAI })
  providerType: ProviderType;

  @ApiProperty({ example: 'My OpenAI Account' })
  name: string;

  @ApiProperty({ example: 'sk-...xxxx', description: 'Masked API key' })
  apiKeyMasked: string;

  @ApiPropertyOptional({ example: 'https://api.openai.com/v1' })
  baseUrl: string | null;

  @ApiProperty({ example: true })
  isActive: boolean;

  @ApiPropertyOptional({ example: ['gpt-4', 'gpt-3.5-turbo'] })
  models: string[] | null;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  createdAt: Date;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  updatedAt: Date;

  static from(entity: AIProvider, maskedKey: string): ProviderResponseDto {
    const dto = new ProviderResponseDto();
    dto.id = entity.id;
    dto.userId = entity.userId;
    dto.providerType = entity.providerType;
    dto.name = entity.name;
    dto.apiKeyMasked = maskedKey;
    dto.baseUrl = entity.baseUrl;
    dto.isActive = entity.isActive;
    dto.models = entity.models;
    dto.createdAt = entity.createdAt;
    dto.updatedAt = entity.updatedAt;
    return dto;
  }
}

export class VerifyProviderResponseDto {
  @ApiProperty({ example: true })
  success: boolean;

  @ApiPropertyOptional({ example: 'Connection successful' })
  message?: string;

  @ApiPropertyOptional({ example: ['gpt-4', 'gpt-4-turbo', 'gpt-3.5-turbo'] })
  models?: string[];
}
