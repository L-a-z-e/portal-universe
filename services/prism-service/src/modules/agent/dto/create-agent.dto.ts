import {
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsPositive,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { AgentRole } from '../agent.entity';

export class CreateAgentDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  @IsPositive()
  providerId: number;

  @ApiProperty({ example: 'PM Agent' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name: string;

  @ApiProperty({ enum: AgentRole, example: AgentRole.PM })
  @IsEnum(AgentRole)
  role: AgentRole;

  @ApiPropertyOptional({
    example: 'Project management and task coordination agent',
  })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({
    example:
      'You are a professional project manager. Help users plan and coordinate tasks.',
  })
  @IsString()
  @IsNotEmpty()
  systemPrompt: string;

  @ApiProperty({ example: 'gpt-4o' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  model: string;

  @ApiPropertyOptional({ example: 0.7, default: 0.7 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(2)
  temperature?: number;

  @ApiPropertyOptional({ example: 4096, default: 4096 })
  @IsOptional()
  @IsInt()
  @IsPositive()
  @Max(128000)
  maxTokens?: number;
}
