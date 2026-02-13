import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
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
    // Get task and verify agent is assigned
    const task = await this.taskService.getTaskEntity(userId, taskId);

    if (!task.agentId) {
      throw BusinessException.agentNotAssigned();
    }

    // Get agent
    const agent = await this.agentService.getAgentEntity(userId, task.agentId);

    // Get execution count for this task
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

    // Create execution record
    const execution = this.executionRepository.create({
      taskId,
      agentId: agent.id,
      executionNumber: executionCount + 1,
      status: ExecutionStatus.PENDING,
      inputPrompt: this.buildPrompt(
        task.title,
        task.description,
        referencedResults,
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
    // Verify task ownership
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

    // Update status to RUNNING
    execution.status = ExecutionStatus.RUNNING;
    execution.startedAt = new Date();
    await this.executionRepository.save(execution);

    // Emit SSE event for execution started
    this.sseService.emitExecutionStarted(
      userId,
      taskInfo.boardId,
      taskInfo.taskId,
      executionId,
    );

    const startTime = Date.now();

    try {
      // Call AI service
      const response = await this.aiService.generate(
        userId,
        providerId,
        request,
      );

      // Update execution with result
      execution.status = ExecutionStatus.COMPLETED;
      execution.outputResult = response.content;
      execution.inputTokens = response.inputTokens;
      execution.outputTokens = response.outputTokens;
      execution.durationMs = Date.now() - startTime;
      execution.completedAt = new Date();

      await this.executionRepository.save(execution);

      // Update task status to IN_REVIEW
      await this.taskService.completeTask(execution.taskId);

      // Send Kafka event
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

      // Emit SSE event for execution completed
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
      // Update execution with error
      execution.status = ExecutionStatus.FAILED;
      const errorMessage =
        error instanceof Error ? error.message : 'Unknown error';
      execution.errorMessage = errorMessage;
      execution.durationMs = Date.now() - startTime;
      execution.completedAt = new Date();

      await this.executionRepository.save(execution);

      // Send Kafka event
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

      // Emit SSE event for execution failed
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

    const results: Array<{ taskTitle: string; outputResult: string }> = [];

    for (const taskId of taskIds) {
      // 순환 참조 방지
      if (taskId === currentTaskId) continue;

      // 각 참조 task의 최신 완료된 execution 조회
      const execution = await this.executionRepository.findOne({
        where: { taskId, status: ExecutionStatus.COMPLETED },
        order: { executionNumber: 'DESC' },
      });

      if (execution?.outputResult) {
        const task = await this.taskRepository.findOne({
          where: { id: taskId },
        });
        results.push({
          taskTitle: task?.title || `Task #${taskId}`,
          outputResult: execution.outputResult.substring(0, 3000), // 토큰 제한
        });
      }
    }

    return results;
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
