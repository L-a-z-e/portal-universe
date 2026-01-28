import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Task, TaskStatus, TaskPriority } from '../task.entity';
import { AgentRole } from '../../agent/agent.entity';
import { TaskStateMachine } from '../task-state-machine';

export class TaskAgentDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 'PM Agent' })
  name: string;

  @ApiProperty({ enum: AgentRole })
  role: AgentRole;
}

export class TaskResponseDto {
  @ApiProperty({ example: 1 })
  id: number;

  @ApiProperty({ example: 1 })
  boardId: number;

  @ApiPropertyOptional({ example: 1 })
  agentId: number | null;

  @ApiPropertyOptional({ type: TaskAgentDto })
  agent?: TaskAgentDto;

  @ApiProperty({ example: 'Implement user authentication' })
  title: string;

  @ApiPropertyOptional({ example: 'Add JWT-based authentication' })
  description: string | null;

  @ApiProperty({ enum: TaskStatus, example: TaskStatus.TODO })
  status: TaskStatus;

  @ApiProperty({ enum: TaskPriority, example: TaskPriority.MEDIUM })
  priority: TaskPriority;

  @ApiProperty({ example: 0 })
  position: number;

  @ApiProperty({
    example: ['execute', 'cancel'],
    description: 'Available actions based on current status',
  })
  availableActions: string[];

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  createdAt: Date;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  updatedAt: Date;

  static from(entity: Task): TaskResponseDto {
    const dto = new TaskResponseDto();
    dto.id = entity.id;
    dto.boardId = entity.boardId;
    dto.agentId = entity.agentId;
    dto.title = entity.title;
    dto.description = entity.description;
    dto.status = entity.status;
    dto.priority = entity.priority;
    dto.position = entity.position;
    dto.availableActions = TaskStateMachine.getAvailableActions(entity.status);
    dto.createdAt = entity.createdAt;
    dto.updatedAt = entity.updatedAt;

    if (entity.agent) {
      dto.agent = {
        id: entity.agent.id,
        name: entity.agent.name,
        role: entity.agent.role,
      };
    }

    return dto;
  }
}
