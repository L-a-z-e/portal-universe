import { findOneOrThrow } from './repository.util';
import { Repository } from 'typeorm';
import { BusinessException } from '../filters/business.exception';

describe('findOneOrThrow', () => {
  let mockRepo: jest.Mocked<Pick<Repository<{ id: number }>, 'findOne'>>;

  beforeEach(() => {
    mockRepo = {
      findOne: jest.fn(),
    };
  });

  it('should return entity when found', async () => {
    const entity = { id: 1 };
    mockRepo.findOne.mockResolvedValue(entity);

    const result = await findOneOrThrow(
      mockRepo as unknown as Repository<{ id: number }>,
      { where: { id: 1 } } as any,
      'Task',
    );

    expect(result).toBe(entity);
  });

  it('should throw BusinessException when entity is null', async () => {
    mockRepo.findOne.mockResolvedValue(null);

    await expect(
      findOneOrThrow(
        mockRepo as unknown as Repository<{ id: number }>,
        { where: { id: 999 } } as any,
        'Task',
      ),
    ).rejects.toThrow(BusinessException);
  });

  it('should include resource name in error message', async () => {
    mockRepo.findOne.mockResolvedValue(null);

    await expect(
      findOneOrThrow(
        mockRepo as unknown as Repository<{ id: number }>,
        { where: { id: 1 } } as any,
        'Provider',
      ),
    ).rejects.toThrow('Provider not found');
  });

  it('should pass options to repository.findOne', async () => {
    const entity = { id: 1 };
    mockRepo.findOne.mockResolvedValue(entity);
    const options = { where: { id: 1 }, relations: ['board'] } as any;

    await findOneOrThrow(
      mockRepo as unknown as Repository<{ id: number }>,
      options,
      'Task',
    );

    expect(mockRepo.findOne).toHaveBeenCalledWith(options);
  });

  it('should throw NOT_FOUND error code', async () => {
    mockRepo.findOne.mockResolvedValue(null);

    try {
      await findOneOrThrow(
        mockRepo as unknown as Repository<{ id: number }>,
        { where: { id: 1 } } as any,
        'Agent',
      );
      fail('Should have thrown');
    } catch (error) {
      expect(error).toBeInstanceOf(BusinessException);
      expect((error as BusinessException).code).toBe('P001');
    }
  });
});
