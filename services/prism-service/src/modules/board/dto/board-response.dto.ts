import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Board } from '../board.entity';
import { TaskStatus } from '../../task/task.entity';

export class TaskSummaryDto {
  @ApiProperty({ example: 5 })
  total!: number;

  @ApiProperty({
    example: { TODO: 2, IN_PROGRESS: 1, IN_REVIEW: 1, DONE: 1, CANCELLED: 0 },
  })
  byStatus!: Record<TaskStatus, number>;
}

export class BoardResponseDto {
  @ApiProperty({ example: 1 })
  id!: number;

  @ApiProperty({ example: 1 })
  userId!: string;

  @ApiProperty({ example: 'Project Alpha' })
  name!: string;

  @ApiPropertyOptional({ example: 'Main project board' })
  description!: string | null;

  @ApiProperty({ example: false })
  isArchived!: boolean;

  @ApiPropertyOptional({ type: TaskSummaryDto })
  taskSummary?: TaskSummaryDto;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  createdAt!: Date;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  updatedAt!: Date;

  static from(entity: Board, includeSummary = false): BoardResponseDto {
    const dto = new BoardResponseDto();
    dto.id = entity.id;
    dto.userId = entity.userId;
    dto.name = entity.name;
    dto.description = entity.description;
    dto.isArchived = entity.isArchived;
    dto.createdAt = entity.createdAt;
    dto.updatedAt = entity.updatedAt;

    if (includeSummary && entity.tasks) {
      const byStatus: Record<TaskStatus, number> = {
        [TaskStatus.TODO]: 0,
        [TaskStatus.IN_PROGRESS]: 0,
        [TaskStatus.IN_REVIEW]: 0,
        [TaskStatus.DONE]: 0,
        [TaskStatus.CANCELLED]: 0,
      };

      entity.tasks.forEach((task) => {
        byStatus[task.status]++;
      });

      dto.taskSummary = {
        total: entity.tasks.length,
        byStatus,
      };
    }

    return dto;
  }
}
