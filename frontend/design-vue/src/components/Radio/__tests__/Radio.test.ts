import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Radio from '../Radio.vue';

const defaultOptions = [
  { label: 'Option 1', value: 'opt1' },
  { label: 'Option 2', value: 'opt2' },
  { label: 'Option 3', value: 'opt3', disabled: true },
];

describe('Radio', () => {
  it('renders all options', () => {
    const wrapper = mount(Radio, {
      props: {
        options: defaultOptions,
        name: 'test-radio',
      },
    });
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(3);
  });

  it('displays option labels', () => {
    const wrapper = mount(Radio, {
      props: {
        options: defaultOptions,
        name: 'test-radio',
      },
    });
    expect(wrapper.text()).toContain('Option 1');
    expect(wrapper.text()).toContain('Option 2');
    expect(wrapper.text()).toContain('Option 3');
  });

  describe('selection', () => {
    it('checks the selected option', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          modelValue: 'opt1',
        },
      });
      const inputs = wrapper.findAll('input[type="radio"]');
      expect((inputs[0].element as HTMLInputElement).checked).toBe(true);
      expect((inputs[1].element as HTMLInputElement).checked).toBe(false);
    });

    it('emits update:modelValue on change', async () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
        },
      });
      await wrapper.findAll('input[type="radio"]')[1].trigger('change');
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['opt2']);
    });

    it('emits change event on change', async () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
        },
      });
      await wrapper.findAll('input[type="radio"]')[0].trigger('change');
      expect(wrapper.emitted('change')?.[0]).toEqual(['opt1']);
    });
  });

  describe('direction', () => {
    it('applies vertical direction by default', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
        },
      });
      expect(wrapper.find('.flex').classes()).toContain('flex-col');
    });

    it('applies horizontal direction when specified', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          direction: 'horizontal',
        },
      });
      expect(wrapper.find('.flex').classes()).toContain('flex-row');
    });
  });

  describe('disabled state', () => {
    it('disables all options when disabled is true', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          disabled: true,
        },
      });
      wrapper.findAll('input[type="radio"]').forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });
    });

    it('disables individual options', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
        },
      });
      const inputs = wrapper.findAll('input[type="radio"]');
      expect(inputs[0].attributes('disabled')).toBeUndefined();
      expect(inputs[2].attributes('disabled')).toBeDefined();
    });
  });

  describe('error state', () => {
    it('shows error message when error is true', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          error: true,
          errorMessage: 'Required field',
        },
      });
      expect(wrapper.text()).toContain('Required field');
    });

    it('sets aria-invalid on container', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          error: true,
        },
      });
      expect(wrapper.find('[role="radiogroup"]').attributes('aria-invalid')).toBe('true');
    });
  });

  describe('sizes', () => {
    it.each(['sm', 'md', 'lg'] as const)('applies %s size classes', (size) => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          size,
        },
      });
      const sizeMap = { sm: 'w-4', md: 'w-5', lg: 'w-6' };
      const radioCircle = wrapper.find('span.relative');
      expect(radioCircle.classes()).toContain(sizeMap[size]);
    });
  });

  describe('accessibility', () => {
    it('has role="radiogroup" on container', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
        },
      });
      expect(wrapper.find('[role="radiogroup"]').exists()).toBe(true);
    });

    it('all inputs share the same name', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'my-radio-group',
        },
      });
      wrapper.findAll('input[type="radio"]').forEach(input => {
        expect(input.attributes('name')).toBe('my-radio-group');
      });
    });

    it('links error with aria-describedby', () => {
      const wrapper = mount(Radio, {
        props: {
          options: defaultOptions,
          name: 'test-radio',
          error: true,
          errorMessage: 'Error text',
        },
      });
      const container = wrapper.find('[role="radiogroup"]');
      const errorP = wrapper.find('p');
      expect(container.attributes('aria-describedby')).toBe(errorP.attributes('id'));
    });
  });
});
