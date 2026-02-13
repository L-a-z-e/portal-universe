import {
  IsEnum,
  IsInt,
  IsNumber,
  IsOptional,
  IsPositive,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { AgentRole } from '../agent.entity';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class UpdateAgentDto {
  @ApiPropertyOptional({ example: 1 })
  @IsOptional()
  @IsInt()
  @IsPositive()
  providerId?: number;

  @ApiPropertyOptional({ example: 'Updated Agent Name' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  @NoXss()
  name?: string;

  @ApiPropertyOptional({ enum: AgentRole })
  @IsOptional()
  @IsEnum(AgentRole)
  role?: AgentRole;

  @ApiPropertyOptional({ example: 'Updated description' })
  @IsOptional()
  @IsString()
  @NoXss()
  description?: string;

  @ApiPropertyOptional({ example: 'Updated system prompt' })
  @IsOptional()
  @IsString()
  @NoXss()
  systemPrompt?: string;

  @ApiPropertyOptional({ example: 'gpt-4-turbo' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  model?: string;

  @ApiPropertyOptional({ example: 0.8 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(2)
  temperature?: number;

  @ApiPropertyOptional({ example: 8192 })
  @IsOptional()
  @IsInt()
  @IsPositive()
  @Max(128000)
  maxTokens?: number;
}
