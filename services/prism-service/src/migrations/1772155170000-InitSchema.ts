import { MigrationInterface, QueryRunner } from 'typeorm';

export class InitSchema1772155170000 implements MigrationInterface {
  name = 'InitSchema1772155170000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // Enum types
    await queryRunner.query(`
      DO $$ BEGIN
        CREATE TYPE "provider_type_enum" AS ENUM('OPENAI', 'ANTHROPIC', 'OLLAMA', 'AZURE_OPENAI', 'LOCAL');
      EXCEPTION WHEN duplicate_object THEN NULL;
      END $$;
    `);
    await queryRunner.query(`
      DO $$ BEGIN
        CREATE TYPE "agent_role_enum" AS ENUM('PM', 'BACKEND', 'FRONTEND', 'DEVOPS', 'TESTER', 'CUSTOM');
      EXCEPTION WHEN duplicate_object THEN NULL;
      END $$;
    `);
    await queryRunner.query(`
      DO $$ BEGIN
        CREATE TYPE "task_status_enum" AS ENUM('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED');
      EXCEPTION WHEN duplicate_object THEN NULL;
      END $$;
    `);
    await queryRunner.query(`
      DO $$ BEGIN
        CREATE TYPE "task_priority_enum" AS ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT');
      EXCEPTION WHEN duplicate_object THEN NULL;
      END $$;
    `);
    await queryRunner.query(`
      DO $$ BEGIN
        CREATE TYPE "execution_status_enum" AS ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED');
      EXCEPTION WHEN duplicate_object THEN NULL;
      END $$;
    `);

    // ai_providers
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "ai_providers" (
        "id" SERIAL PRIMARY KEY,
        "user_id" varchar(36) NOT NULL,
        "provider_type" "provider_type_enum" NOT NULL,
        "name" varchar(100) NOT NULL,
        "api_key_encrypted" text NOT NULL,
        "base_url" varchar(255),
        "is_active" boolean NOT NULL DEFAULT true,
        "models" jsonb,
        "created_at" TIMESTAMP NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP NOT NULL DEFAULT now()
      );
    `);

    // agents
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "agents" (
        "id" SERIAL PRIMARY KEY,
        "user_id" varchar(36) NOT NULL,
        "provider_id" integer NOT NULL,
        "name" varchar(100) NOT NULL,
        "role" "agent_role_enum" NOT NULL DEFAULT 'CUSTOM',
        "description" text,
        "system_prompt" text NOT NULL,
        "model" varchar(100) NOT NULL,
        "temperature" decimal(3,2) NOT NULL DEFAULT 0.7,
        "max_tokens" integer NOT NULL DEFAULT 4096,
        "created_at" TIMESTAMP NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "fk_agents_provider" FOREIGN KEY ("provider_id")
          REFERENCES "ai_providers"("id") ON DELETE NO ACTION ON UPDATE NO ACTION
      );
    `);

    // boards
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "boards" (
        "id" SERIAL PRIMARY KEY,
        "user_id" varchar(36) NOT NULL,
        "name" varchar(100) NOT NULL,
        "description" text,
        "is_archived" boolean NOT NULL DEFAULT false,
        "created_at" TIMESTAMP NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP NOT NULL DEFAULT now()
      );
    `);

    // tasks
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "tasks" (
        "id" SERIAL PRIMARY KEY,
        "board_id" integer NOT NULL,
        "agent_id" integer,
        "title" varchar(200) NOT NULL,
        "description" text,
        "status" "task_status_enum" NOT NULL DEFAULT 'TODO',
        "priority" "task_priority_enum" NOT NULL DEFAULT 'MEDIUM',
        "position" integer NOT NULL DEFAULT 0,
        "due_date" date,
        "referenced_task_ids" text,
        "created_at" TIMESTAMP NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "fk_tasks_board" FOREIGN KEY ("board_id")
          REFERENCES "boards"("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT "fk_tasks_agent" FOREIGN KEY ("agent_id")
          REFERENCES "agents"("id") ON DELETE NO ACTION ON UPDATE NO ACTION
      );
    `);

    // executions
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "executions" (
        "id" SERIAL PRIMARY KEY,
        "task_id" integer NOT NULL,
        "agent_id" integer NOT NULL,
        "execution_number" integer NOT NULL,
        "status" "execution_status_enum" NOT NULL DEFAULT 'PENDING',
        "input_prompt" text NOT NULL,
        "output_result" text,
        "user_feedback" text,
        "input_tokens" integer,
        "output_tokens" integer,
        "duration_ms" integer,
        "error_message" text,
        "started_at" TIMESTAMP,
        "completed_at" TIMESTAMP,
        "created_at" TIMESTAMP NOT NULL DEFAULT now(),
        CONSTRAINT "fk_executions_task" FOREIGN KEY ("task_id")
          REFERENCES "tasks"("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT "fk_executions_agent" FOREIGN KEY ("agent_id")
          REFERENCES "agents"("id") ON DELETE NO ACTION ON UPDATE NO ACTION
      );
    `);

    // Indexes
    await queryRunner.query(`CREATE INDEX IF NOT EXISTS "IDX_ai_providers_user_id_id" ON "ai_providers" ("user_id", "id")`);
    await queryRunner.query(`CREATE INDEX IF NOT EXISTS "IDX_agents_user_id_id" ON "agents" ("user_id", "id")`);
    await queryRunner.query(`CREATE INDEX IF NOT EXISTS "IDX_boards_user_id_id" ON "boards" ("user_id", "id")`);
    await queryRunner.query(`CREATE INDEX IF NOT EXISTS "IDX_tasks_board_id_status" ON "tasks" ("board_id", "status")`);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "executions"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "tasks"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "boards"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "agents"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "ai_providers"`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_ai_providers_user_id_id"`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_agents_user_id_id"`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_boards_user_id_id"`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_tasks_board_id_status"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "execution_status_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "task_priority_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "task_status_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "agent_role_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "provider_type_enum"`);
  }
}
