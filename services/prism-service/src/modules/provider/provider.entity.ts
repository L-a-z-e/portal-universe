import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  OneToMany,
  Index,
} from 'typeorm';
import { Agent } from '../agent/agent.entity';

export enum ProviderType {
  OPENAI = 'OPENAI',
  ANTHROPIC = 'ANTHROPIC',
  OLLAMA = 'OLLAMA',
  AZURE_OPENAI = 'AZURE_OPENAI',
  LOCAL = 'LOCAL',
}

@Entity('ai_providers')
@Index(['userId', 'id'])
export class AIProvider {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id', type: 'varchar', length: 36 })
  userId: string;

  @Column({
    name: 'provider_type',
    type: 'enum',
    enum: ProviderType,
  })
  providerType: ProviderType;

  @Column({ length: 100 })
  name: string;

  @Column({ name: 'api_key_encrypted', type: 'text' })
  apiKeyEncrypted: string;

  @Column({ name: 'base_url', type: 'varchar', length: 255, nullable: true })
  baseUrl: string | null;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  @Column({ type: 'jsonb', nullable: true })
  models: string[] | null;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @OneToMany(() => Agent, (agent) => agent.provider)
  agents: Agent[];
}
