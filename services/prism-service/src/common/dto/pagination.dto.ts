import { IsOptional, IsInt, Min, Max } from 'class-validator';
import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class PaginationDto {
  @ApiPropertyOptional({ default: 1, minimum: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page: number = 1;

  @ApiPropertyOptional({ default: 20, minimum: 1, maximum: 100 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  size: number = 20;

  get skip(): number {
    return (this.page - 1) * this.size;
  }

  get take(): number {
    return this.size;
  }
}

export interface PaginatedResult<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function toPaginatedResult<T>(
  items: T[],
  total: number,
  pagination?: PaginationDto,
): PaginatedResult<T> {
  const size = pagination?.size ?? 20;
  return {
    items,
    page: pagination?.page ?? 1,
    size,
    totalElements: total,
    totalPages: Math.ceil(total / size),
  };
}
