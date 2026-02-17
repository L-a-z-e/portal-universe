import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { BoardService } from './board.service';
import { CreateBoardDto } from './dto/create-board.dto';
import { UpdateBoardDto } from './dto/update-board.dto';
import { BoardResponseDto } from './dto/board-response.dto';
import { BoardListQueryDto } from './dto/board-list-query.dto';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';
import { PaginatedResult } from '../../common/dto/pagination.dto';

@ApiTags('Boards')
@ApiBearerAuth()
@Controller('boards')
export class BoardController {
  constructor(private readonly boardService: BoardService) {}

  @Post()
  @ApiOperation({ summary: 'Create a new board' })
  @ApiResponse({ status: 201, type: BoardResponseDto })
  async create(
    @CurrentUserId() userId: string,
    @Body() dto: CreateBoardDto,
  ): Promise<BoardResponseDto> {
    return this.boardService.create(userId, dto);
  }

  @Get()
  @ApiOperation({ summary: 'List all boards for the current user' })
  @ApiResponse({ status: 200, type: [BoardResponseDto] })
  async findAll(
    @CurrentUserId() userId: string,
    @Query() query: BoardListQueryDto,
  ): Promise<PaginatedResult<BoardResponseDto>> {
    return this.boardService.findAll(userId, query.includeArchived, query);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get board details with task summary' })
  @ApiResponse({ status: 200, type: BoardResponseDto })
  async findOne(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<BoardResponseDto> {
    return this.boardService.findOne(userId, id);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Update a board' })
  @ApiResponse({ status: 200, type: BoardResponseDto })
  async update(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateBoardDto,
  ): Promise<BoardResponseDto> {
    return this.boardService.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Archive a board (soft delete)' })
  @ApiResponse({ status: 204, description: 'Board archived' })
  async remove(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.boardService.remove(userId, id);
  }
}
