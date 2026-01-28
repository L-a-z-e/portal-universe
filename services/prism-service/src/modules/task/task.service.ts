import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Task, TaskStatus } from './task.entity';
import { Board } from '../board/board.entity';
import { Agent } from '../agent/agent.entity';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto, ChangePositionDto } from './dto/update-task.dto';
import { TaskResponseDto } from './dto/task-response.dto';
import { TaskStateMachine, TaskAction } from './task-state-machine';
import { BusinessException } from '../../common/filters/business.exception';
import { SseService } from '../sse/sse.service';

@Injectable()
export class TaskService {
  constructor(
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
    @InjectRepository(Board)
    private readonly boardRepository: Repository<Board>,
    @InjectRepository(Agent)
    private readonly agentRepository: Repository<Agent>,
    private readonly sseService: SseService,
  ) {}

  async create(
    userId: string,
    boardId: number,
    dto: CreateTaskDto,
  ): Promise<TaskResponseDto> {
    // Verify board exists and belongs to user
    const board = await this.boardRepository.findOne({
      where: { id: boardId, userId },
    });
    if (!board) {
      throw BusinessException.notFound('Board');
    }

    // Verify agent if provided
    if (dto.agentId) {
      const agent = await this.agentRepository.findOne({
        where: { id: dto.agentId, userId },
      });
      if (!agent) {
        throw BusinessException.notFound('Agent');
      }
    }

    // Get max position
    const maxPosition = await this.taskRepository
      .createQueryBuilder('task')
      .where('task.boardId = :boardId', { boardId })
      .andWhere('task.status = :status', { status: TaskStatus.TODO })
      .select('MAX(task.position)', 'maxPos')
      .getRawOne<{ maxPos: number | null }>();

    const task = this.taskRepository.create({
      boardId,
      title: dto.title,
      description: dto.description,
      priority: dto.priority,
      agentId: dto.agentId,
      status: TaskStatus.TODO,
      position: (maxPosition?.maxPos ?? -1) + 1,
    });

    const saved = await this.taskRepository.save(task);
    const result = await this.findOne(userId, saved.id);

    // Emit SSE event
    this.sseService.emitTaskCreated(
      userId,
      boardId,
      result as unknown as Record<string, unknown>,
    );

    return result;
  }

  async findAllByBoard(
    userId: string,
    boardId: number,
  ): Promise<TaskResponseDto[]> {
    // Verify board
    const board = await this.boardRepository.findOne({
      where: { id: boardId, userId },
    });
    if (!board) {
      throw BusinessException.notFound('Board');
    }

    const tasks = await this.taskRepository.find({
      where: { boardId },
      relations: ['agent'],
      order: { status: 'ASC', position: 'ASC' },
    });

    return tasks.map((t) => TaskResponseDto.from(t));
  }

  async findOne(userId: string, id: number): Promise<TaskResponseDto> {
    const task = await this.findByIdAndUser(userId, id);
    return TaskResponseDto.from(task);
  }

  async update(
    userId: string,
    id: number,
    dto: UpdateTaskDto,
  ): Promise<TaskResponseDto> {
    const task = await this.findByIdAndUser(userId, id);

    if (dto.agentId !== undefined) {
      if (dto.agentId === null) {
        task.agentId = null;
      } else {
        const agent = await this.agentRepository.findOne({
          where: { id: dto.agentId, userId },
        });
        if (!agent) {
          throw BusinessException.notFound('Agent');
        }
        task.agentId = dto.agentId;
      }
    }

    if (dto.title !== undefined) task.title = dto.title;
    if (dto.description !== undefined) task.description = dto.description;
    if (dto.priority !== undefined) task.priority = dto.priority;

    await this.taskRepository.save(task);
    const result = await this.findOne(userId, id);

    // Emit SSE event
    this.sseService.emitTaskUpdated(
      userId,
      task.boardId,
      result as unknown as Record<string, unknown>,
    );

    return result;
  }

  async remove(userId: string, id: number): Promise<void> {
    const task = await this.findByIdAndUser(userId, id);
    const boardId = task.boardId;
    const taskId = task.id;
    await this.taskRepository.remove(task);

    // Emit SSE event
    this.sseService.emitTaskDeleted(userId, boardId, taskId);
  }

  async changePosition(
    userId: string,
    id: number,
    dto: ChangePositionDto,
  ): Promise<TaskResponseDto> {
    const task = await this.findByIdAndUser(userId, id);
    const fromStatus = task.status;
    task.position = dto.position;
    await this.taskRepository.save(task);

    // Emit SSE event
    this.sseService.emitTaskMoved(
      userId,
      task.boardId,
      task.id,
      fromStatus,
      task.status,
      dto.position,
    );

    return this.findOne(userId, id);
  }

  // State transition methods
  async execute(userId: string, id: number): Promise<TaskResponseDto> {
    return this.performAction(userId, id, 'execute', true);
  }

  async approve(userId: string, id: number): Promise<TaskResponseDto> {
    return this.performAction(userId, id, 'approve');
  }

  async reject(
    userId: string,
    id: number,
    feedback?: string,
  ): Promise<TaskResponseDto> {
    const task = await this.findByIdAndUser(userId, id);

    // Store feedback if provided (will be used in execution)
    if (feedback) {
      task.description = `${task.description || ''}\n\n---\nFeedback: ${feedback}`;
    }

    return this.performAction(userId, id, 'retry');
  }

  async cancel(userId: string, id: number): Promise<TaskResponseDto> {
    return this.performAction(userId, id, 'cancel');
  }

  async reopen(userId: string, id: number): Promise<TaskResponseDto> {
    return this.performAction(userId, id, 'reopen');
  }

  /**
   * Get task entity (for internal use by execution service)
   */
  async getTaskEntity(userId: string, id: number): Promise<Task> {
    return this.findByIdAndUser(userId, id);
  }

  /**
   * Complete task (called by execution service when AI completes)
   */
  async completeTask(taskId: number): Promise<void> {
    const task = await this.taskRepository.findOne({ where: { id: taskId } });
    if (!task) {
      throw BusinessException.notFound('Task');
    }

    task.status = TaskStateMachine.transition(task.status, 'complete');
    await this.taskRepository.save(task);
  }

  private async performAction(
    userId: string,
    id: number,
    action: TaskAction,
    requireAgent = false,
  ): Promise<TaskResponseDto> {
    const task = await this.findByIdAndUser(userId, id);

    if (requireAgent && !task.agentId) {
      throw BusinessException.agentNotAssigned();
    }

    task.status = TaskStateMachine.transition(task.status, action);
    await this.taskRepository.save(task);

    return this.findOne(userId, id);
  }

  private async findByIdAndUser(userId: string, id: number): Promise<Task> {
    const task = await this.taskRepository
      .createQueryBuilder('task')
      .leftJoinAndSelect('task.agent', 'agent')
      .leftJoin('task.board', 'board')
      .where('task.id = :id', { id })
      .andWhere('board.userId = :userId', { userId })
      .getOne();

    if (!task) {
      throw BusinessException.notFound('Task');
    }
    return task;
  }
}
