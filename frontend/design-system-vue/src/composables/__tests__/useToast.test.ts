import { describe, it, expect, beforeEach } from 'vitest';
import { useToast } from '../useToast';

describe('useToast', () => {
  let toast: ReturnType<typeof useToast>;

  beforeEach(() => {
    toast = useToast();
    // Clear any existing toasts
    toast.clear();
  });

  it('returns expected interface', () => {
    expect(toast.toasts).toBeDefined();
    expect(toast.add).toBeInstanceOf(Function);
    expect(toast.remove).toBeInstanceOf(Function);
    expect(toast.clear).toBeInstanceOf(Function);
    expect(toast.success).toBeInstanceOf(Function);
    expect(toast.error).toBeInstanceOf(Function);
    expect(toast.warning).toBeInstanceOf(Function);
    expect(toast.info).toBeInstanceOf(Function);
  });

  describe('add', () => {
    it('adds a toast and returns id', () => {
      const id = toast.add({ message: 'Test message' });
      expect(id).toBeDefined();
      expect(id).toMatch(/^toast-/);
    });

    it('adds toast to toasts array', () => {
      toast.add({ message: 'Test message' });
      expect(toast.toasts.value).toHaveLength(1);
    });

    it('applies default values', () => {
      toast.add({ message: 'Test message' });
      const addedToast = toast.toasts.value[0];
      expect(addedToast.variant).toBe('info');
      expect(addedToast.duration).toBe(5000);
      expect(addedToast.dismissible).toBe(true);
    });

    it('allows overriding defaults', () => {
      toast.add({
        message: 'Test message',
        variant: 'success',
        duration: 3000,
        dismissible: false,
      });
      const addedToast = toast.toasts.value[0];
      expect(addedToast.variant).toBe('success');
      expect(addedToast.duration).toBe(3000);
      expect(addedToast.dismissible).toBe(false);
    });

    it('can add multiple toasts', () => {
      toast.add({ message: 'First' });
      toast.add({ message: 'Second' });
      toast.add({ message: 'Third' });
      expect(toast.toasts.value).toHaveLength(3);
    });

    it('supports title', () => {
      toast.add({ message: 'Body', title: 'Title' });
      expect(toast.toasts.value[0].title).toBe('Title');
    });
  });

  describe('remove', () => {
    it('removes toast by id', () => {
      const id = toast.add({ message: 'Test' });
      expect(toast.toasts.value).toHaveLength(1);
      toast.remove(id);
      expect(toast.toasts.value).toHaveLength(0);
    });

    it('does nothing for non-existent id', () => {
      toast.add({ message: 'Test' });
      toast.remove('non-existent-id');
      expect(toast.toasts.value).toHaveLength(1);
    });

    it('only removes matching toast', () => {
      toast.add({ message: 'First' });
      const secondId = toast.add({ message: 'Second' });
      toast.add({ message: 'Third' });

      toast.remove(secondId);

      expect(toast.toasts.value).toHaveLength(2);
      expect(toast.toasts.value[0].message).toBe('First');
      expect(toast.toasts.value[1].message).toBe('Third');
    });
  });

  describe('clear', () => {
    it('removes all toasts', () => {
      toast.add({ message: 'First' });
      toast.add({ message: 'Second' });
      toast.add({ message: 'Third' });

      expect(toast.toasts.value).toHaveLength(3);
      toast.clear();
      expect(toast.toasts.value).toHaveLength(0);
    });

    it('works on empty toasts', () => {
      toast.clear();
      expect(toast.toasts.value).toHaveLength(0);
    });
  });

  describe('success', () => {
    it('adds toast with success variant', () => {
      toast.success('Success message');
      expect(toast.toasts.value[0].variant).toBe('success');
      expect(toast.toasts.value[0].message).toBe('Success message');
    });

    it('returns id', () => {
      const id = toast.success('Success');
      expect(id).toMatch(/^toast-/);
    });

    it('accepts options', () => {
      toast.success('Success', { title: 'Done!', duration: 2000 });
      expect(toast.toasts.value[0].title).toBe('Done!');
      expect(toast.toasts.value[0].duration).toBe(2000);
    });
  });

  describe('error', () => {
    it('adds toast with error variant', () => {
      toast.error('Error message');
      expect(toast.toasts.value[0].variant).toBe('error');
      expect(toast.toasts.value[0].message).toBe('Error message');
    });

    it('accepts options', () => {
      toast.error('Error', { dismissible: false });
      expect(toast.toasts.value[0].dismissible).toBe(false);
    });
  });

  describe('warning', () => {
    it('adds toast with warning variant', () => {
      toast.warning('Warning message');
      expect(toast.toasts.value[0].variant).toBe('warning');
      expect(toast.toasts.value[0].message).toBe('Warning message');
    });
  });

  describe('info', () => {
    it('adds toast with info variant', () => {
      toast.info('Info message');
      expect(toast.toasts.value[0].variant).toBe('info');
      expect(toast.toasts.value[0].message).toBe('Info message');
    });
  });

  describe('shared state', () => {
    it('shares state between useToast calls', () => {
      const toast1 = useToast();
      const toast2 = useToast();

      toast1.add({ message: 'From toast1' });

      expect(toast2.toasts.value).toHaveLength(1);
      expect(toast2.toasts.value[0].message).toBe('From toast1');
    });
  });

  describe('unique ids', () => {
    it('generates unique ids for each toast', () => {
      const ids = [
        toast.add({ message: 'First' }),
        toast.add({ message: 'Second' }),
        toast.add({ message: 'Third' }),
      ];

      const uniqueIds = new Set(ids);
      expect(uniqueIds.size).toBe(3);
    });
  });
});
