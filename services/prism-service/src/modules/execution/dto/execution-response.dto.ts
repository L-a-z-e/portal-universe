import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Execution, ExecutionStatus } from '../execution.entity';

export class ExecutionResponseDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 1 })
  taskId: number;

  @ApiProperty({ example: 1 })
  agentId: number;

  @ApiPropertyOptional({ example: 'Code Assistant', nullable: true })
  agentName: string | null;

  @ApiProperty({ example: 1 })
  executionNumber: number;

  @ApiProperty({ enum: ExecutionStatus, example: ExecutionStatus.COMPLETED })
  status: ExecutionStatus;

  @ApiProperty({ example: 'Implement user authentication...' })
  inputPrompt: string;

  @ApiPropertyOptional({ example: 'Here is the implementation...' })
  outputResult: string | null;

  @ApiPropertyOptional({ example: 'Please add error handling' })
  userFeedback: string | null;

  @ApiPropertyOptional({ example: 150 })
  inputTokens: number | null;

  @ApiPropertyOptional({ example: 500 })
  outputTokens: number | null;

  @ApiPropertyOptional({ example: 3500 })
  durationMs: number | null;

  @ApiPropertyOptional({ example: null })
  errorMessage: string | null;

  @ApiPropertyOptional({ example: '2024-01-01T00:00:00.000Z' })
  startedAt: Date | null;

  @ApiPropertyOptional({ example: '2024-01-01T00:00:05.000Z' })
  completedAt: Date | null;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  createdAt: Date;

  static from(entity: Execution): ExecutionResponseDto {
    const dto = new ExecutionResponseDto();
    dto.id = entity.id;
    dto.taskId = entity.taskId;
    dto.agentId = entity.agentId;
    dto.agentName = entity.agent?.name || null;
    dto.executionNumber = entity.executionNumber;
    dto.status = entity.status;
    dto.inputPrompt = entity.inputPrompt;
    dto.outputResult = entity.outputResult;
    dto.userFeedback = entity.userFeedback;
    dto.inputTokens = entity.inputTokens;
    dto.outputTokens = entity.outputTokens;
    dto.durationMs = entity.durationMs;
    dto.errorMessage = entity.errorMessage;
    dto.startedAt = entity.startedAt;
    dto.completedAt = entity.completedAt;
    dto.createdAt = entity.createdAt;
    return dto;
  }
}
