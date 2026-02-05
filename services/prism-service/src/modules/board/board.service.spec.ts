import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { BoardService } from './board.service';
import { Board } from './board.entity';
import { TaskStatus } from '../task/task.entity';
import { BusinessException } from '../../common/filters/business.exception';
import { PaginationDto } from '../../common/dto/pagination.dto';

describe('BoardService', () => {
  let service: BoardService;
  let boardRepo: Record<string, jest.Mock>;
  let mockQueryBuilder: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeBoard = (overrides?: Partial<Board>): Board =>
    ({
      id: 1,
      userId,
      name: 'Test Board',
      description: 'Test description',
      isArchived: false,
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
      tasks: [],
      ...overrides,
    }) as Board;

  beforeEach(async () => {
    mockQueryBuilder = {
      leftJoinAndSelect: jest.fn().mockReturnThis(),
      where: jest.fn().mockReturnThis(),
      andWhere: jest.fn().mockReturnThis(),
      orderBy: jest.fn().mockReturnThis(),
      skip: jest.fn().mockReturnThis(),
      take: jest.fn().mockReturnThis(),
      getOne: jest.fn(),
      getManyAndCount: jest.fn(),
    };

    boardRepo = {
      findOne: jest.fn(),
      create: jest.fn((data: Partial<Board>) => ({ ...makeBoard(), ...data })),
      save: jest.fn((entity: Board) =>
        Promise.resolve({ ...entity, id: entity.id || 1 }),
      ),
      createQueryBuilder: jest.fn().mockReturnValue(mockQueryBuilder),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        BoardService,
        { provide: getRepositoryToken(Board), useValue: boardRepo },
      ],
    }).compile();

    service = module.get<BoardService>(BoardService);
  });

  describe('create', () => {
    const dto = { name: 'New Board', description: 'Description' };

    it('should create board', async () => {
      boardRepo.findOne.mockResolvedValue(null); // no duplicate

      const result = await service.create(userId, dto);

      expect(boardRepo.save).toHaveBeenCalled();
      expect(result.name).toBe('New Board');
      expect(result.isArchived).toBe(false);
    });

    it('should throw duplicate on existing name (non-archived only)', async () => {
      boardRepo.findOne.mockResolvedValue(makeBoard()); // duplicate

      await expect(service.create(userId, dto)).rejects.toThrow(
        BusinessException,
      );

      expect(boardRepo.findOne).toHaveBeenCalledWith({
        where: { userId, name: dto.name, isArchived: false },
      });
    });

    it('should allow duplicate name if archived', async () => {
      // The service checks where: { isArchived: false } so archived boards don't count
      boardRepo.findOne.mockResolvedValue(null); // no active duplicate

      const result = await service.create(userId, dto);

      expect(result.name).toBe('New Board');
    });
  });

  describe('findAll', () => {
    it('should return boards excluding archived', async () => {
      const boards = [makeBoard()];
      mockQueryBuilder.getManyAndCount.mockResolvedValue([boards, 1]);

      await service.findAll(userId, false);

      expect(mockQueryBuilder.andWhere).toHaveBeenCalledWith(
        'board.isArchived = :isArchived',
        { isArchived: false },
      );
    });

    it('should return boards including archived', async () => {
      const boards = [makeBoard(), makeBoard({ id: 2, isArchived: true })];
      mockQueryBuilder.getManyAndCount.mockResolvedValue([boards, 2]);

      const result = await service.findAll(userId, true);

      // andWhere for isArchived should NOT be called
      expect(mockQueryBuilder.andWhere).not.toHaveBeenCalledWith(
        'board.isArchived = :isArchived',
        expect.anything(),
      );
      expect(result.items).toHaveLength(2);
    });

    it('should include task summary', async () => {
      const board = makeBoard({
        tasks: [
          { status: TaskStatus.TODO } as any,
          { status: TaskStatus.DONE } as any,
        ],
      });
      mockQueryBuilder.getManyAndCount.mockResolvedValue([[board], 1]);

      const result = await service.findAll(userId);

      expect(result.items[0].taskSummary).toBeDefined();
      expect(result.items[0].taskSummary!.total).toBe(2);
      expect(result.items[0].taskSummary!.byStatus[TaskStatus.TODO]).toBe(1);
      expect(result.items[0].taskSummary!.byStatus[TaskStatus.DONE]).toBe(1);
    });

    it('should return paginated boards', async () => {
      mockQueryBuilder.getManyAndCount.mockResolvedValue([
        [makeBoard()],
        25,
      ]);

      const pagination = new PaginationDto();
      pagination.page = 2;
      pagination.size = 10;

      const result = await service.findAll(userId, false, pagination);

      expect(result.page).toBe(2);
      expect(result.size).toBe(10);
      expect(result.totalPages).toBe(3);
      expect(mockQueryBuilder.skip).toHaveBeenCalledWith(10);
      expect(mockQueryBuilder.take).toHaveBeenCalledWith(10);
    });

    it('should filter by userId', async () => {
      mockQueryBuilder.getManyAndCount.mockResolvedValue([[], 0]);

      await service.findAll(userId);

      expect(mockQueryBuilder.where).toHaveBeenCalledWith(
        'board.userId = :userId',
        { userId },
      );
    });
  });

  describe('findOne', () => {
    it('should return single board with tasks', async () => {
      const board = makeBoard({
        tasks: [{ status: TaskStatus.TODO } as any],
      });
      mockQueryBuilder.getOne.mockResolvedValue(board);

      const result = await service.findOne(userId, 1);

      expect(result.id).toBe(1);
      expect(result.taskSummary).toBeDefined();
    });

    it('should throw notFound for invalid id', async () => {
      mockQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.findOne(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('update', () => {
    it('should update board name', async () => {
      const board = makeBoard();
      // 1st getOne: findByIdAndUser (no tasks)
      mockQueryBuilder.getOne.mockResolvedValueOnce(board);
      boardRepo.findOne.mockResolvedValue(null); // duplicate check
      // 2nd getOne: findByIdAndUser (with tasks) for result
      mockQueryBuilder.getOne.mockResolvedValueOnce(
        makeBoard({ name: 'Updated', tasks: [] }),
      );

      const result = await service.update(userId, 1, { name: 'Updated' });

      expect(result.name).toBe('Updated');
    });

    it('should update board description', async () => {
      const board = makeBoard();
      mockQueryBuilder.getOne
        .mockResolvedValueOnce(board)
        .mockResolvedValueOnce(
          makeBoard({ description: 'New description', tasks: [] }),
        );

      const result = await service.update(userId, 1, {
        description: 'New description',
      });

      expect(result.description).toBe('New description');
    });
  });

  describe('remove', () => {
    it('should archive board on remove (soft delete)', async () => {
      const board = makeBoard();
      mockQueryBuilder.getOne.mockResolvedValue(board);

      await service.remove(userId, 1);

      expect(board.isArchived).toBe(true);
      expect(boardRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({ isArchived: true }),
      );
    });

    it('should throw notFound on remove', async () => {
      mockQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.remove(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('getBoardEntity', () => {
    it('should return board entity', async () => {
      const board = makeBoard();
      mockQueryBuilder.getOne.mockResolvedValue(board);

      const result = await service.getBoardEntity(userId, 1);

      expect(result).toBe(board);
    });
  });
});
