import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Alert from '../Alert.vue';

describe('Alert', () => {
  it('renders slot content', () => {
    const wrapper = mount(Alert, {
      slots: {
        default: 'Alert message',
      },
    });
    expect(wrapper.text()).toContain('Alert message');
  });

  it('renders title when provided', () => {
    const wrapper = mount(Alert, {
      props: { title: 'Alert Title' },
    });
    expect(wrapper.text()).toContain('Alert Title');
  });

  describe('variants', () => {
    it.each(['info', 'success', 'warning', 'error'] as const)('applies %s variant classes', (variant) => {
      const wrapper = mount(Alert, {
        props: { variant },
      });

      const colorMap = {
        info: 'bg-blue-50',
        success: 'bg-green-50',
        warning: 'bg-yellow-50',
        error: 'bg-red-50',
      };

      expect(wrapper.find('[role="alert"]').classes()).toContain(colorMap[variant]);
    });

    it('renders variant-specific icon', () => {
      const wrapper = mount(Alert, {
        props: { variant: 'success', showIcon: true },
      });
      expect(wrapper.find('svg').exists()).toBe(true);
    });
  });

  describe('showIcon', () => {
    it('shows icon by default', () => {
      const wrapper = mount(Alert);
      expect(wrapper.find('svg').exists()).toBe(true);
    });

    it('hides icon when showIcon is false', () => {
      const wrapper = mount(Alert, {
        props: { showIcon: false },
      });
      // The alert itself shouldn't have a nested svg as its direct icon
      const iconContainer = wrapper.find('.flex-shrink-0');
      expect(iconContainer.exists()).toBe(false);
    });
  });

  describe('bordered', () => {
    it('applies border classes when bordered is true', () => {
      const wrapper = mount(Alert, {
        props: { bordered: true },
      });
      expect(wrapper.find('[role="alert"]').classes()).toContain('border');
    });

    it('does not apply border classes by default', () => {
      const wrapper = mount(Alert);
      expect(wrapper.find('[role="alert"]').classes()).not.toContain('border');
    });
  });

  describe('dismissible', () => {
    it('shows dismiss button when dismissible', () => {
      const wrapper = mount(Alert, {
        props: { dismissible: true },
      });
      expect(wrapper.find('button[aria-label="Dismiss"]').exists()).toBe(true);
    });

    it('hides dismiss button by default', () => {
      const wrapper = mount(Alert);
      expect(wrapper.find('button[aria-label="Dismiss"]').exists()).toBe(false);
    });

    it('emits dismiss event on button click', async () => {
      const wrapper = mount(Alert, {
        props: { dismissible: true },
      });
      await wrapper.find('button[aria-label="Dismiss"]').trigger('click');
      expect(wrapper.emitted('dismiss')).toBeTruthy();
    });

    it('hides alert after dismiss', async () => {
      const wrapper = mount(Alert, {
        props: { dismissible: true },
      });
      await wrapper.find('button[aria-label="Dismiss"]').trigger('click');
      expect(wrapper.find('[role="alert"]').exists()).toBe(false);
    });
  });

  describe('slots', () => {
    it('renders action slot', () => {
      const wrapper = mount(Alert, {
        slots: {
          action: '<button>Action</button>',
        },
      });
      expect(wrapper.text()).toContain('Action');
    });

    it('renders custom icon slot', () => {
      const wrapper = mount(Alert, {
        slots: {
          icon: '<span data-testid="custom-icon">Custom</span>',
        },
      });
      expect(wrapper.find('[data-testid="custom-icon"]').exists()).toBe(true);
    });
  });

  describe('accessibility', () => {
    it('has role="alert"', () => {
      const wrapper = mount(Alert);
      expect(wrapper.find('[role="alert"]').exists()).toBe(true);
    });

    it('has aria-live="polite"', () => {
      const wrapper = mount(Alert);
      expect(wrapper.find('[aria-live="polite"]').exists()).toBe(true);
    });
  });
});
