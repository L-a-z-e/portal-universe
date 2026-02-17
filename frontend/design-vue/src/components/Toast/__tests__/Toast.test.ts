import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import Toast from '../Toast.vue';

describe('Toast', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const defaultProps = {
    id: 'toast-1',
    message: 'Test message',
  };

  it('renders message', () => {
    const wrapper = mount(Toast, {
      props: defaultProps,
    });
    expect(wrapper.text()).toContain('Test message');
  });

  it('renders title when provided', () => {
    const wrapper = mount(Toast, {
      props: { ...defaultProps, title: 'Toast Title' },
    });
    expect(wrapper.text()).toContain('Toast Title');
  });

  describe('variants', () => {
    it.each(['info', 'success', 'warning', 'error'] as const)('applies %s variant border color', (variant) => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, variant },
      });

      const colorMap = {
        info: 'border-blue-500',
        success: 'border-green-500',
        warning: 'border-yellow-500',
        error: 'border-red-500',
      };

      // Find the inner div with role="alert"
      const alertDiv = wrapper.find('[role="alert"]');
      expect(alertDiv.classes()).toContain(colorMap[variant]);
    });

    it('defaults to info variant', () => {
      const wrapper = mount(Toast, {
        props: defaultProps,
      });
      const alertDiv = wrapper.find('[role="alert"]');
      expect(alertDiv.classes()).toContain('border-blue-500');
    });

    it('shows variant-specific icon', () => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, variant: 'success' },
      });
      expect(wrapper.find('svg').exists()).toBe(true);
    });
  });

  describe('dismissible', () => {
    it('shows dismiss button by default', () => {
      const wrapper = mount(Toast, {
        props: defaultProps,
      });
      expect(wrapper.find('button[aria-label="Dismiss"]').exists()).toBe(true);
    });

    it('hides dismiss button when not dismissible', () => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, dismissible: false },
      });
      expect(wrapper.find('button[aria-label="Dismiss"]').exists()).toBe(false);
    });

    it('emits dismiss on close button click after transition', async () => {
      const wrapper = mount(Toast, {
        props: defaultProps,
      });
      await wrapper.find('button[aria-label="Dismiss"]').trigger('click');

      // Wait for the setTimeout in dismiss function (150ms)
      vi.advanceTimersByTime(150);

      expect(wrapper.emitted('dismiss')?.[0]).toEqual(['toast-1']);
    });
  });

  describe('auto-dismiss', () => {
    it('emits dismiss after duration plus transition delay', async () => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, duration: 3000 },
      });

      // Wait for duration
      vi.advanceTimersByTime(3000);
      // Wait for transition delay
      vi.advanceTimersByTime(150);

      expect(wrapper.emitted('dismiss')?.[0]).toEqual(['toast-1']);
    });

    it('does not auto-dismiss when duration is 0', () => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, duration: 0 },
      });

      vi.advanceTimersByTime(10000);

      expect(wrapper.emitted('dismiss')).toBeFalsy();
    });
  });

  describe('accessibility', () => {
    it('has role="alert" on inner container', () => {
      const wrapper = mount(Toast, {
        props: defaultProps,
      });
      expect(wrapper.find('[role="alert"]').exists()).toBe(true);
    });

    it('has aria-live="polite" on inner container', () => {
      const wrapper = mount(Toast, {
        props: defaultProps,
      });
      expect(wrapper.find('[aria-live="polite"]').exists()).toBe(true);
    });
  });

  describe('mouse interaction', () => {
    it('pauses timer on mouse enter', async () => {
      const wrapper = mount(Toast, {
        props: { ...defaultProps, duration: 3000 },
      });

      // Wait some time
      vi.advanceTimersByTime(1000);

      // Mouse enter should pause timer
      await wrapper.find('[role="alert"]').trigger('mouseenter');

      // Advance past the original timeout
      vi.advanceTimersByTime(3000);

      // Should not have emitted dismiss because timer was paused
      expect(wrapper.emitted('dismiss')).toBeFalsy();
    });
  });

  describe('action button', () => {
    it('renders action button when action is provided', () => {
      const onClick = vi.fn();
      const wrapper = mount(Toast, {
        props: {
          ...defaultProps,
          action: { label: 'Undo', onClick },
        },
      });

      expect(wrapper.text()).toContain('Undo');
    });

    it('calls action onClick when clicked', async () => {
      const onClick = vi.fn();
      const wrapper = mount(Toast, {
        props: {
          ...defaultProps,
          action: { label: 'Undo', onClick },
        },
      });

      const actionButton = wrapper.findAll('button').find(b => b.text() === 'Undo');
      await actionButton?.trigger('click');

      expect(onClick).toHaveBeenCalled();
    });
  });
});

// Note: ToastContainer uses Teleport and useToast composable,
// making it complex to test in isolation. The useToast composable
// is tested in src/composables/__tests__/useToast.test.ts
// and the Toast component is tested above.
// ToastContainer integration tests should be done in e2e tests.
