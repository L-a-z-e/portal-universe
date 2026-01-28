import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Board } from './board.entity';
import { CreateBoardDto } from './dto/create-board.dto';
import { UpdateBoardDto } from './dto/update-board.dto';
import { BoardResponseDto } from './dto/board-response.dto';
import { BusinessException } from '../../common/filters/business.exception';

@Injectable()
export class BoardService {
  constructor(
    @InjectRepository(Board)
    private readonly boardRepository: Repository<Board>,
  ) {}

  async create(userId: string, dto: CreateBoardDto): Promise<BoardResponseDto> {
    // Check for duplicate name
    const existing = await this.boardRepository.findOne({
      where: { userId, name: dto.name, isArchived: false },
    });
    if (existing) {
      throw BusinessException.duplicateResource('Board with this name');
    }

    const board = this.boardRepository.create({
      userId,
      name: dto.name,
      description: dto.description,
      isArchived: false,
    });

    const saved = await this.boardRepository.save(board);
    return BoardResponseDto.from(saved);
  }

  async findAll(
    userId: string,
    includeArchived = false,
  ): Promise<BoardResponseDto[]> {
    const queryBuilder = this.boardRepository
      .createQueryBuilder('board')
      .leftJoinAndSelect('board.tasks', 'task')
      .where('board.userId = :userId', { userId })
      .orderBy('board.createdAt', 'DESC');

    if (!includeArchived) {
      queryBuilder.andWhere('board.isArchived = :isArchived', {
        isArchived: false,
      });
    }

    const boards = await queryBuilder.getMany();
    return boards.map((b) => BoardResponseDto.from(b, true));
  }

  async findOne(userId: string, id: number): Promise<BoardResponseDto> {
    const board = await this.findByIdAndUser(userId, id, true);
    return BoardResponseDto.from(board, true);
  }

  async update(
    userId: string,
    id: number,
    dto: UpdateBoardDto,
  ): Promise<BoardResponseDto> {
    const board = await this.findByIdAndUser(userId, id);

    if (dto.name !== undefined) {
      const existing = await this.boardRepository.findOne({
        where: { userId, name: dto.name, isArchived: false },
      });
      if (existing && existing.id !== id) {
        throw BusinessException.duplicateResource('Board with this name');
      }
      board.name = dto.name;
    }

    if (dto.description !== undefined) board.description = dto.description;
    if (dto.isArchived !== undefined) board.isArchived = dto.isArchived;

    await this.boardRepository.save(board);

    const updated = await this.findByIdAndUser(userId, id, true);
    return BoardResponseDto.from(updated, true);
  }

  async remove(userId: string, id: number): Promise<void> {
    const board = await this.findByIdAndUser(userId, id);
    // Soft delete by archiving
    board.isArchived = true;
    await this.boardRepository.save(board);
  }

  /**
   * Get board entity (for internal use by other services)
   */
  async getBoardEntity(userId: string, id: number): Promise<Board> {
    return this.findByIdAndUser(userId, id);
  }

  private async findByIdAndUser(
    userId: string,
    id: number,
    withTasks = false,
  ): Promise<Board> {
    const queryBuilder = this.boardRepository
      .createQueryBuilder('board')
      .where('board.id = :id', { id })
      .andWhere('board.userId = :userId', { userId });

    if (withTasks) {
      queryBuilder.leftJoinAndSelect('board.tasks', 'task');
    }

    const board = await queryBuilder.getOne();
    if (!board) {
      throw BusinessException.notFound('Board');
    }
    return board;
  }
}
