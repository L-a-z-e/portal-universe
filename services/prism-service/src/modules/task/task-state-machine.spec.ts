import { TaskStateMachine } from './task-state-machine';
import { TaskStatus } from './task.entity';
import { BusinessException } from '../../common/filters/business.exception';

describe('TaskStateMachine', () => {
  describe('valid state transitions', () => {
    const validTransitions: [TaskStatus, string, TaskStatus][] = [
      [TaskStatus.TODO, 'execute', TaskStatus.IN_PROGRESS],
      [TaskStatus.TODO, 'cancel', TaskStatus.CANCELLED],
      [TaskStatus.IN_PROGRESS, 'complete', TaskStatus.IN_REVIEW],
      [TaskStatus.IN_PROGRESS, 'cancel', TaskStatus.CANCELLED],
      [TaskStatus.IN_REVIEW, 'approve', TaskStatus.DONE],
      [TaskStatus.IN_REVIEW, 'retry', TaskStatus.IN_PROGRESS],
      [TaskStatus.IN_REVIEW, 'cancel', TaskStatus.CANCELLED],
      [TaskStatus.DONE, 'reopen', TaskStatus.TODO],
      [TaskStatus.CANCELLED, 'reopen', TaskStatus.TODO],
    ];

    it.each(validTransitions)(
      'should transition from %s via "%s" to %s',
      (from, action, expected) => {
        expect(TaskStateMachine.transition(from, action)).toBe(expected);
      },
    );
  });

  describe('invalid state transitions', () => {
    it('should throw BusinessException for TODO + approve', () => {
      expect(() =>
        TaskStateMachine.transition(TaskStatus.TODO, 'approve'),
      ).toThrow(BusinessException);
    });

    it('should throw BusinessException for DONE + complete', () => {
      expect(() =>
        TaskStateMachine.transition(TaskStatus.DONE, 'complete'),
      ).toThrow(BusinessException);
    });

    it('should throw for IN_PROGRESS + approve', () => {
      expect(() =>
        TaskStateMachine.transition(TaskStatus.IN_PROGRESS, 'approve'),
      ).toThrow(BusinessException);
    });

    it('should include available actions in error message', () => {
      expect(() =>
        TaskStateMachine.transition(TaskStatus.TODO, 'approve'),
      ).toThrow(/available:/);
    });
  });

  describe('getAvailableActions', () => {
    it('should return [execute, cancel] for TODO', () => {
      expect(TaskStateMachine.getAvailableActions(TaskStatus.TODO)).toEqual([
        'execute',
        'cancel',
      ]);
    });

    it('should return [complete, cancel] for IN_PROGRESS', () => {
      expect(
        TaskStateMachine.getAvailableActions(TaskStatus.IN_PROGRESS),
      ).toEqual(['complete', 'cancel']);
    });

    it('should return [approve, retry, cancel] for IN_REVIEW', () => {
      expect(
        TaskStateMachine.getAvailableActions(TaskStatus.IN_REVIEW),
      ).toEqual(['approve', 'retry', 'cancel']);
    });

    it('should return [reopen] for DONE', () => {
      expect(TaskStateMachine.getAvailableActions(TaskStatus.DONE)).toEqual([
        'reopen',
      ]);
    });

    it('should return [reopen] for CANCELLED', () => {
      expect(
        TaskStateMachine.getAvailableActions(TaskStatus.CANCELLED),
      ).toEqual(['reopen']);
    });
  });

  describe('canTransition', () => {
    it('should return true for valid transition', () => {
      expect(TaskStateMachine.canTransition(TaskStatus.TODO, 'execute')).toBe(
        true,
      );
    });

    it('should return false for invalid transition', () => {
      expect(TaskStateMachine.canTransition(TaskStatus.TODO, 'approve')).toBe(
        false,
      );
    });
  });

  describe('getTargetStatus', () => {
    it('should return target status for valid transition', () => {
      expect(TaskStateMachine.getTargetStatus(TaskStatus.TODO, 'execute')).toBe(
        TaskStatus.IN_PROGRESS,
      );
    });

    it('should throw BusinessException for invalid transition', () => {
      expect(() =>
        TaskStateMachine.getTargetStatus(TaskStatus.DONE, 'execute'),
      ).toThrow(BusinessException);
    });
  });
});
