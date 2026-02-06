import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ExecutionResponseDto } from '../../execution/dto/execution-response.dto';

export class ReferencedTaskDto {
  @ApiProperty({ example: 1 })
  taskId: number;

  @ApiProperty({ example: 'Research AI trends' })
  taskTitle: string;

  @ApiPropertyOptional({ type: ExecutionResponseDto })
  lastExecution: ExecutionResponseDto | null;
}

export class TaskContextResponseDto {
  @ApiProperty({ type: [ExecutionResponseDto] })
  previousExecutions: ExecutionResponseDto[];

  @ApiProperty({ type: [ReferencedTaskDto] })
  referencedTasks: ReferencedTaskDto[];
}
