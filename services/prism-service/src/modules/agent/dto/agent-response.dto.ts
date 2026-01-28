import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Agent, AgentRole } from '../agent.entity';
import { ProviderType } from '../../provider/provider.entity';

export class AgentProviderDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 'My OpenAI Account' })
  name: string;

  @ApiProperty({ enum: ProviderType })
  providerType: ProviderType;
}

export class AgentResponseDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 1 })
  userId: string;

  @ApiProperty({ example: 1 })
  providerId: number;

  @ApiPropertyOptional({ type: AgentProviderDto })
  provider?: AgentProviderDto;

  @ApiProperty({ example: 'PM Agent' })
  name: string;

  @ApiProperty({ enum: AgentRole, example: AgentRole.PM })
  role: AgentRole;

  @ApiPropertyOptional({ example: 'Project management agent' })
  description: string | null;

  @ApiProperty({ example: 'You are a professional project manager...' })
  systemPrompt: string;

  @ApiProperty({ example: 'gpt-4o' })
  model: string;

  @ApiProperty({ example: 0.7 })
  temperature: number;

  @ApiProperty({ example: 4096 })
  maxTokens: number;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  createdAt: Date;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  updatedAt: Date;

  static from(entity: Agent): AgentResponseDto {
    const dto = new AgentResponseDto();
    dto.id = entity.id;
    dto.userId = entity.userId;
    dto.providerId = entity.providerId;
    dto.name = entity.name;
    dto.role = entity.role;
    dto.description = entity.description;
    dto.systemPrompt = entity.systemPrompt;
    dto.model = entity.model;
    dto.temperature = Number(entity.temperature);
    dto.maxTokens = entity.maxTokens;
    dto.createdAt = entity.createdAt;
    dto.updatedAt = entity.updatedAt;

    if (entity.provider) {
      dto.provider = {
        id: entity.provider.id,
        name: entity.provider.name,
        providerType: entity.provider.providerType,
      };
    }

    return dto;
  }
}
