import {
  IsArray,
  IsDateString,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsPositive,
  IsString,
  MaxLength,
} from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { TaskPriority } from '../task.entity';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class CreateTaskDto {
  @ApiProperty({ example: 'Implement user authentication' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(200)
  @NoXss()
  title: string;

  @ApiPropertyOptional({
    example: 'Add JWT-based authentication with refresh token support',
  })
  @IsOptional()
  @IsString()
  @NoXss()
  description?: string;

  @ApiPropertyOptional({ enum: TaskPriority, default: TaskPriority.MEDIUM })
  @IsOptional()
  @IsEnum(TaskPriority)
  priority?: TaskPriority;

  @ApiPropertyOptional({ example: 1, description: 'Agent ID to assign' })
  @IsOptional()
  @IsInt()
  @IsPositive()
  agentId?: number;

  @ApiPropertyOptional({ example: '2026-02-28', description: 'Due date' })
  @IsOptional()
  @IsDateString()
  dueDate?: string;

  @ApiPropertyOptional({
    example: [1, 2],
    description: 'IDs of tasks to reference',
  })
  @IsOptional()
  @IsArray()
  @IsInt({ each: true })
  referencedTaskIds?: number[];
}
