import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { Task } from '../task/task.entity';
import { Agent } from '../agent/agent.entity';

export enum ExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

@Entity('executions')
export class Execution {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ name: 'task_id', type: 'int' })
  taskId!: number;

  @ManyToOne(() => Task, (task) => task.executions)
  @JoinColumn({ name: 'task_id' })
  task!: Task;

  @Column({ name: 'agent_id', type: 'int' })
  agentId!: number;

  @ManyToOne(() => Agent, (agent) => agent.executions)
  @JoinColumn({ name: 'agent_id' })
  agent!: Agent;

  @Column({ name: 'execution_number', type: 'int' })
  executionNumber!: number;

  @Column({
    type: 'enum',
    enum: ExecutionStatus,
    default: ExecutionStatus.PENDING,
  })
  status!: ExecutionStatus;

  @Column({ name: 'input_prompt', type: 'text' })
  inputPrompt!: string;

  @Column({ name: 'output_result', type: 'text', nullable: true })
  outputResult!: string | null;

  @Column({ name: 'user_feedback', type: 'text', nullable: true })
  userFeedback!: string | null;

  @Column({ name: 'input_tokens', type: 'int', nullable: true })
  inputTokens!: number | null;

  @Column({ name: 'output_tokens', type: 'int', nullable: true })
  outputTokens!: number | null;

  @Column({ name: 'duration_ms', type: 'int', nullable: true })
  durationMs!: number | null;

  @Column({ name: 'error_message', type: 'text', nullable: true })
  errorMessage!: string | null;

  @Column({ name: 'started_at', type: 'timestamp', nullable: true })
  startedAt!: Date | null;

  @Column({ name: 'completed_at', type: 'timestamp', nullable: true })
  completedAt!: Date | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
