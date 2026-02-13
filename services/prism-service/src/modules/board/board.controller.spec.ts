import { Test, TestingModule } from '@nestjs/testing';
import { BoardController } from './board.controller';
import { BoardService } from './board.service';
import { BoardResponseDto } from './dto/board-response.dto';
import { TaskStatus } from '../task/task.entity';
import {
  PaginationDto,
  PaginatedResult,
} from '../../common/dto/pagination.dto';

describe('BoardController', () => {
  let controller: BoardController;
  let boardService: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeBoardResponse = (
    overrides?: Partial<BoardResponseDto>,
  ): BoardResponseDto => {
    const dto = new BoardResponseDto();
    dto.id = 1;
    dto.userId = userId;
    dto.name = 'Test Board';
    dto.description = 'Test description';
    dto.isArchived = false;
    dto.createdAt = new Date('2024-01-01');
    dto.updatedAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  beforeEach(async () => {
    boardService = {
      create: jest.fn(),
      findAll: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [BoardController],
      providers: [{ provide: BoardService, useValue: boardService }],
    }).compile();

    controller = module.get<BoardController>(BoardController);
  });

  describe('create', () => {
    it('should call service.create and return result', async () => {
      const dto = { name: 'New Board', description: 'Description' };
      const expected = makeBoardResponse({ name: 'New Board' });
      boardService.create.mockResolvedValue(expected);

      const result = await controller.create(userId, dto);

      expect(boardService.create).toHaveBeenCalledWith(userId, dto);
      expect(result).toBe(expected);
    });
  });

  describe('findAll', () => {
    it('should call service.findAll without includeArchived by default', async () => {
      const pagination = new PaginationDto();
      const expected: PaginatedResult<BoardResponseDto> = {
        items: [makeBoardResponse()],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      };
      boardService.findAll.mockResolvedValue(expected);

      const result = await controller.findAll(userId, undefined, pagination);

      expect(boardService.findAll).toHaveBeenCalledWith(
        userId,
        false,
        pagination,
      );
      expect(result).toBe(expected);
    });

    it('should pass includeArchived=true to service', async () => {
      boardService.findAll.mockResolvedValue({
        items: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      });

      await controller.findAll(userId, true);

      expect(boardService.findAll).toHaveBeenCalledWith(
        userId,
        true,
        undefined,
      );
    });
  });

  describe('findOne', () => {
    it('should call service.findOne with correct params', async () => {
      const expected = makeBoardResponse({
        taskSummary: {
          total: 3,
          byStatus: {
            [TaskStatus.TODO]: 1,
            [TaskStatus.IN_PROGRESS]: 1,
            [TaskStatus.IN_REVIEW]: 0,
            [TaskStatus.DONE]: 1,
            [TaskStatus.CANCELLED]: 0,
          },
        },
      });
      boardService.findOne.mockResolvedValue(expected);

      const result = await controller.findOne(userId, 1);

      expect(boardService.findOne).toHaveBeenCalledWith(userId, 1);
      expect(result.taskSummary).toBeDefined();
      expect(result.taskSummary!.total).toBe(3);
    });
  });

  describe('update', () => {
    it('should call service.update with correct params', async () => {
      const dto = { name: 'Updated Board', description: 'New desc' };
      const expected = makeBoardResponse({ name: 'Updated Board' });
      boardService.update.mockResolvedValue(expected);

      const result = await controller.update(userId, 1, dto);

      expect(boardService.update).toHaveBeenCalledWith(userId, 1, dto);
      expect(result).toBe(expected);
    });
  });

  describe('remove', () => {
    it('should call service.remove with correct params', async () => {
      boardService.remove.mockResolvedValue(undefined);

      await controller.remove(userId, 1);

      expect(boardService.remove).toHaveBeenCalledWith(userId, 1);
    });
  });

  describe('service delegation', () => {
    it('should pass userId from decorator to service for findOne', async () => {
      boardService.findOne.mockResolvedValue(makeBoardResponse());
      await controller.findOne('other-user', 42);
      expect(boardService.findOne).toHaveBeenCalledWith('other-user', 42);
    });

    it('should delegate create with all dto fields', async () => {
      const dto = { name: 'Board' };
      boardService.create.mockResolvedValue(makeBoardResponse());
      await controller.create(userId, dto);
      expect(boardService.create).toHaveBeenCalledWith(userId, dto);
    });

    it('should delegate remove and return void', async () => {
      boardService.remove.mockResolvedValue(undefined);
      const result = await controller.remove(userId, 1);
      expect(result).toBeUndefined();
    });
  });
});
