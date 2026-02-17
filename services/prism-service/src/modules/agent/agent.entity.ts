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
import { AIProvider } from '../provider/provider.entity';
import { Task } from '../task/task.entity';
import { Execution } from '../execution/execution.entity';

export enum AgentRole {
  PM = 'PM',
  BACKEND = 'BACKEND',
  FRONTEND = 'FRONTEND',
  DEVOPS = 'DEVOPS',
  TESTER = 'TESTER',
  CUSTOM = 'CUSTOM',
}

@Entity('agents')
@Index(['userId', 'id'])
export class Agent {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ name: 'user_id', type: 'varchar', length: 36 })
  userId!: string;

  @Column({ name: 'provider_id', type: 'int' })
  providerId!: number;

  @ManyToOne(() => AIProvider, (provider) => provider.agents)
  @JoinColumn({ name: 'provider_id' })
  provider!: AIProvider;

  @Column({ length: 100 })
  name!: string;

  @Column({
    type: 'enum',
    enum: AgentRole,
    default: AgentRole.CUSTOM,
  })
  role!: AgentRole;

  @Column({ type: 'text', nullable: true })
  description!: string | null;

  @Column({ name: 'system_prompt', type: 'text' })
  systemPrompt!: string;

  @Column({ length: 100 })
  model!: string;

  @Column({ type: 'decimal', precision: 3, scale: 2, default: 0.7 })
  temperature!: number;

  @Column({ name: 'max_tokens', type: 'int', default: 4096 })
  maxTokens!: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;

  @OneToMany(() => Task, (task) => task.agent)
  tasks!: Task[];

  @OneToMany(() => Execution, (execution) => execution.agent)
  executions!: Execution[];
}
