import { MigrationInterface, QueryRunner } from 'typeorm';

export class TimestampToTimestamptz1772155180000 implements MigrationInterface {
  name = 'TimestampToTimestamptz1772155180000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // ai_providers
    await queryRunner.query(`
      ALTER TABLE "ai_providers"
        ALTER COLUMN "created_at" TYPE TIMESTAMPTZ USING "created_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "updated_at" TYPE TIMESTAMPTZ USING "updated_at" AT TIME ZONE 'Asia/Seoul';
    `);

    // agents
    await queryRunner.query(`
      ALTER TABLE "agents"
        ALTER COLUMN "created_at" TYPE TIMESTAMPTZ USING "created_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "updated_at" TYPE TIMESTAMPTZ USING "updated_at" AT TIME ZONE 'Asia/Seoul';
    `);

    // boards
    await queryRunner.query(`
      ALTER TABLE "boards"
        ALTER COLUMN "created_at" TYPE TIMESTAMPTZ USING "created_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "updated_at" TYPE TIMESTAMPTZ USING "updated_at" AT TIME ZONE 'Asia/Seoul';
    `);

    // tasks
    await queryRunner.query(`
      ALTER TABLE "tasks"
        ALTER COLUMN "created_at" TYPE TIMESTAMPTZ USING "created_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "updated_at" TYPE TIMESTAMPTZ USING "updated_at" AT TIME ZONE 'Asia/Seoul';
    `);

    // executions
    await queryRunner.query(`
      ALTER TABLE "executions"
        ALTER COLUMN "started_at" TYPE TIMESTAMPTZ USING "started_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "completed_at" TYPE TIMESTAMPTZ USING "completed_at" AT TIME ZONE 'Asia/Seoul',
        ALTER COLUMN "created_at" TYPE TIMESTAMPTZ USING "created_at" AT TIME ZONE 'Asia/Seoul';
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    // executions
    await queryRunner.query(`
      ALTER TABLE "executions"
        ALTER COLUMN "started_at" TYPE TIMESTAMP USING "started_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "completed_at" TYPE TIMESTAMP USING "completed_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "created_at" TYPE TIMESTAMP USING "created_at" AT TIME ZONE 'UTC';
    `);

    // tasks
    await queryRunner.query(`
      ALTER TABLE "tasks"
        ALTER COLUMN "created_at" TYPE TIMESTAMP USING "created_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "updated_at" TYPE TIMESTAMP USING "updated_at" AT TIME ZONE 'UTC';
    `);

    // boards
    await queryRunner.query(`
      ALTER TABLE "boards"
        ALTER COLUMN "created_at" TYPE TIMESTAMP USING "created_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "updated_at" TYPE TIMESTAMP USING "updated_at" AT TIME ZONE 'UTC';
    `);

    // agents
    await queryRunner.query(`
      ALTER TABLE "agents"
        ALTER COLUMN "created_at" TYPE TIMESTAMP USING "created_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "updated_at" TYPE TIMESTAMP USING "updated_at" AT TIME ZONE 'UTC';
    `);

    // ai_providers
    await queryRunner.query(`
      ALTER TABLE "ai_providers"
        ALTER COLUMN "created_at" TYPE TIMESTAMP USING "created_at" AT TIME ZONE 'UTC',
        ALTER COLUMN "updated_at" TYPE TIMESTAMP USING "updated_at" AT TIME ZONE 'UTC';
    `);
  }
}
