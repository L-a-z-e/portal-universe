import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { Execution, ExecutionStatus } from './execution.entity';
import { Task } from '../task/task.entity';
import { TaskService } from '../task/task.service';
import { AgentService } from '../agent/agent.service';
import { AIService } from '../ai/ai.service';
import { KafkaProducer } from '../event/kafka.producer';
import { SseService } from '../sse/sse.service';
import { ExecutionResponseDto } from './dto/execution-response.dto';
import { BusinessException } from '../../common/filters/business.exception';

@Injectable()
export class ExecutionService {
  private readonly logger = new Logger(ExecutionService.name);

  constructor(
    @InjectRepository(Execution)
    private readonly executionRepository: Repository<Execution>,
    @InjectRepository(Task)
    private readonly taskRepository: Repository<Task>,
    private readonly taskService: TaskService,
    private readonly agentService: AgentService,
    private readonly aiService: AIService,
    private readonly kafkaProducer: KafkaProducer,
    private readonly sseService: SseService,
  ) {}

  async executeTask(
    userId: string,
    taskId: number,
  ): Promise<ExecutionResponseDto> {
    const task = await this.taskService.getTaskEntity(userId, taskId);

    if (!task.agentId) {
      throw BusinessException.agentNotAssigned();
    }

    const agent = await this.agentService.getAgentEntity(userId, task.agentId);

    const executionCount = await this.executionRepository.count({
      where: { taskId },
    });

    // 참조 Task 결과 조회
    let referencedResults:
      | Array<{ taskTitle: string; outputResult: string }>
      | undefined;
    if (task.referencedTaskIds && task.referencedTaskIds.length > 0) {
      referencedResults = await this.getReferencedResults(
        task.referencedTaskIds,
        task.id,
      );
    }

    // Get previous execution's feedback (for retry after reject)
    let previousFeedback: string | undefined;
    if (executionCount > 0) {
      const lastExecution = await this.executionRepository.findOne({
        where: { taskId },
        order: { executionNumber: 'DESC' },
      });
      if (lastExecution?.userFeedback) {
        previousFeedback = lastExecution.userFeedback;
      }
    }

    const execution = this.executionRepository.create({
      taskId,
      agentId: agent.id,
      executionNumber: executionCount + 1,
      status: ExecutionStatus.PENDING,
      inputPrompt: this.buildPrompt(
        task.title,
        task.description,
        referencedResults,
        previousFeedback,
      ),
    });

    const saved = await this.executionRepository.save(execution);

    // Execute asynchronously (don't block the response)
    this.runExecution(
      userId,
      saved.id,
      agent.providerId,
      {
        taskId: task.id,
        boardId: task.boardId,
        title: task.title,
        agentName: agent.name,
      },
      {
        systemPrompt: agent.systemPrompt,
        userPrompt: saved.inputPrompt,
        model: agent.model,
        temperature: Number(agent.temperature),
        maxTokens: agent.maxTokens,
      },
    ).catch((error) => {
      this.logger.error(`Execution ${saved.id} failed:`, error);
    });

    return ExecutionResponseDto.from(saved);
  }

  async findByTask(
    userId: string,
    taskId: number,
  ): Promise<ExecutionResponseDto[]> {
    await this.taskService.getTaskEntity(userId, taskId);

    const executions = await this.executionRepository.find({
      where: { taskId },
      order: { executionNumber: 'DESC' },
    });

    return executions.map((e) => ExecutionResponseDto.from(e));
  }

  async findOne(userId: string, id: number): Promise<ExecutionResponseDto> {
    const execution = await this.findByIdAndUser(userId, id);
    return ExecutionResponseDto.from(execution);
  }

  private async runExecution(
    userId: string,
    executionId: number,
    providerId: number,
    taskInfo: {
      taskId: number;
      boardId: number;
      title: string;
      agentName: string;
    },
    request: {
      systemPrompt: string;
      userPrompt: string;
      model: string;
      temperature: number;
      maxTokens: number;
    },
  ): Promise<void> {
    const execution = await this.executionRepository.findOne({
      where: { id: executionId },
    });

    if (!execution) {
      throw BusinessException.notFound('Execution');
    }

    execution.status = ExecutionStatus.RUNNING;
    execution.startedAt = new Date();
    await this.executionRepository.save(execution);

    this.sseService.emitExecutionStarted(
      userId,
      taskInfo.boardId,
      taskInfo.taskId,
      executionId,
    );

    const startTime = Date.now();

    try {
      const response = await this.aiService.generate(
        userId,
        providerId,
        request,
      );

      execution.status = ExecutionStatus.COMPLETED;
      execution.outputResult = response.content;
      execution.inputTokens = response.inputTokens;
      execution.outputTokens = response.outputTokens;
      execution.durationMs = Date.now() - startTime;
      execution.completedAt = new Date();

      await this.executionRepository.save(execution);

      await this.taskService.completeTask(execution.taskId);

      try {
        await this.kafkaProducer.sendTaskCompleted({
          taskId: taskInfo.taskId,
          boardId: taskInfo.boardId,
          userId,
          title: taskInfo.title,
          status: 'IN_REVIEW',
          agentName: taskInfo.agentName,
          executionId,
          timestamp: new Date().toISOString(),
        });
      } catch (kafkaError) {
        this.logger.error(
          `Failed to publish task completed event: executionId=${executionId}`,
          kafkaError,
        );
      }

      this.sseService.emitExecutionCompleted(
        userId,
        taskInfo.boardId,
        taskInfo.taskId,
        executionId,
        response.content,
      );

      this.logger.log(
        `Execution ${executionId} completed: ${response.inputTokens}+${response.outputTokens} tokens`,
      );
    } catch (error) {
      execution.status = ExecutionStatus.FAILED;
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      execution.errorMessage = errorMessage;
      execution.durationMs = Date.now() - startTime;
      execution.completedAt = new Date();

      await this.executionRepository.save(execution);

      try {
        await this.kafkaProducer.sendTaskFailed({
          taskId: taskInfo.taskId,
          boardId: taskInfo.boardId,
          userId,
          title: taskInfo.title,
          status: 'FAILED',
          agentName: taskInfo.agentName,
          executionId,
          errorMessage,
          timestamp: new Date().toISOString(),
        });
      } catch (kafkaError) {
        this.logger.error(
          `Failed to publish task failed event: executionId=${executionId}`,
          kafkaError,
        );
      }

      this.sseService.emitExecutionFailed(
        userId,
        taskInfo.boardId,
        taskInfo.taskId,
        executionId,
        errorMessage,
      );

      this.logger.error(`Execution ${executionId} failed: ${errorMessage}`);
    }
  }

  private buildPrompt(
    title: string,
    description: string | null,
    referencedResults?: Array<{ taskTitle: string; outputResult: string }>,
    previousFeedback?: string,
  ): string {
    let prompt = `Task: ${title}`;
    if (description) {
      prompt += `\n\nDescription:\n${description}`;
    }

    // 참조된 Task 결과 추가
    if (referencedResults && referencedResults.length > 0) {
      prompt += `\n\n---\nReferenced Task Results:\n`;
      for (const ref of referencedResults) {
        prompt += `\n[${ref.taskTitle}]\n${ref.outputResult}\n`;
      }
    }

    // 이전 실행에 대한 사용자 피드백 추가 (reject 후 재실행 시)
    if (previousFeedback) {
      prompt += `\n\n---\nPrevious Feedback (please address this):\n${previousFeedback}`;
    }

    return prompt;
  }

  /**
   * 참조된 Task들의 최신 실행 결과를 조회
   * @param taskIds 참조할 Task ID 배열
   * @param currentTaskId 현재 Task ID (순환 참조 방지)
   */
  private async getReferencedResults(
    taskIds: number[],
    currentTaskId: number,
  ): Promise<Array<{ taskTitle: string; outputResult: string }>> {
    if (!taskIds || taskIds.length === 0) return [];

    const filteredIds = taskIds.filter((id) => id !== currentTaskId);
    if (filteredIds.length === 0) return [];

    // IN 조회 2회 (N×2 → 2 고정)
    const executions = await this.executionRepository.find({
      where: { taskId: In(filteredIds), status: ExecutionStatus.COMPLETED },
      order: { executionNumber: 'DESC' },
    });

    // taskId별 최신 execution 추출
    const latestByTaskId = new Map<number, Execution>();
    for (const exec of executions) {
      if (!latestByTaskId.has(exec.taskId)) {
        latestByTaskId.set(exec.taskId, exec);
      }
    }

    const taskIdsWithResult = [...latestByTaskId.keys()];
    if (taskIdsWithResult.length === 0) return [];

    const tasks = await this.taskRepository.find({
      where: { id: In(taskIdsWithResult) },
    });
    const taskMap = new Map(tasks.map((t) => [t.id, t]));

    return taskIdsWithResult
      .map((taskId) => {
        const exec = latestByTaskId.get(taskId);
        if (!exec?.outputResult) return null;
        const task = taskMap.get(taskId);
        return {
          taskTitle: task?.title || `Task #${taskId}`,
          outputResult: exec.outputResult.substring(0, 3000),
        };
      })
      .filter((r): r is NonNullable<typeof r> => r !== null);
  }

  private async findByIdAndUser(
    userId: string,
    id: number,
  ): Promise<Execution> {
    const execution = await this.executionRepository
      .createQueryBuilder('execution')
      .leftJoin('execution.task', 'task')
      .leftJoin('task.board', 'board')
      .where('execution.id = :id', { id })
      .andWhere('board.userId = :userId', { userId })
      .getOne();

    if (!execution) {
      throw BusinessException.notFound('Execution');
    }
    return execution;
  }
}
