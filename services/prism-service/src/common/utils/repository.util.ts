import { Repository, FindOneOptions, ObjectLiteral } from 'typeorm';
import { BusinessException } from '../filters/business.exception';

/**
 * Find a single entity or throw BusinessException.notFound
 */
export async function findOneOrThrow<T extends ObjectLiteral>(
  repo: Repository<T>,
  options: FindOneOptions<T>,
  resourceName: string,
): Promise<T> {
  const entity = await repo.findOne(options);
  if (!entity) {
    throw BusinessException.notFound(resourceName);
  }
  return entity;
}
