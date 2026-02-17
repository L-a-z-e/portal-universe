import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { NoXss } from '../../../common/validators/no-xss.validator';

export class CreateBoardDto {
  @ApiProperty({ example: 'Project Alpha' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  @NoXss()
  name!: string;

  @ApiPropertyOptional({
    example: 'Main project board for feature development',
  })
  @IsOptional()
  @IsString()
  @NoXss()
  description?: string;
}
