import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
  OneToMany,
  Index,
} from 'typeorm';
import { Board } from '../board/board.entity';
import { Agent } from '../agent/agent.entity';
import { Execution } from '../execution/execution.entity';

export enum TaskStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  IN_REVIEW = 'IN_REVIEW',
  DONE = 'DONE',
  CANCELLED = 'CANCELLED',
}

export enum TaskPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

@Entity('tasks')
@Index(['boardId', 'status'])
export class Task {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'board_id', type: 'int' })
  boardId: number;

  @ManyToOne(() => Board, (board) => board.tasks)
  @JoinColumn({ name: 'board_id' })
  board: Board;

  @Column({ name: 'agent_id', type: 'int', nullable: true })
  agentId: number | null;

  @ManyToOne(() => Agent, (agent) => agent.tasks, { nullable: true })
  @JoinColumn({ name: 'agent_id' })
  agent: Agent | null;

  @Column({ length: 200 })
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column({
    type: 'enum',
    enum: TaskStatus,
    default: TaskStatus.TODO,
  })
  status: TaskStatus;

  @Column({
    type: 'enum',
    enum: TaskPriority,
    default: TaskPriority.MEDIUM,
  })
  priority: TaskPriority;

  @Column({ type: 'int', default: 0 })
  position: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @OneToMany(() => Execution, (execution) => execution.task)
  executions: Execution[];
}
