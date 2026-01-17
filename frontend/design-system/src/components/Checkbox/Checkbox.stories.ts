import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Checkbox } from './index';

const meta: Meta<typeof Checkbox> = {
  title: 'Components/Form/Checkbox',
  component: Checkbox,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    disabled: {
      control: 'boolean',
    },
    indeterminate: {
      control: 'boolean',
    },
    error: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Checkbox>;

export const Default: Story = {
  render: () => ({
    components: { Checkbox },
    setup() {
      const checked = ref(false);
      return { checked };
    },
    template: '<Checkbox v-model="checked" label="Accept terms and conditions" />',
  }),
};

export const Checked: Story = {
  render: () => ({
    components: { Checkbox },
    setup() {
      const checked = ref(true);
      return { checked };
    },
    template: '<Checkbox v-model="checked" label="I agree to the terms" />',
  }),
};

export const Indeterminate: Story = {
  args: {
    indeterminate: true,
    label: 'Select all',
  },
  render: (args) => ({
    components: { Checkbox },
    setup() {
      const checked = ref(false);
      return { checked, args };
    },
    template: '<Checkbox v-model="checked" v-bind="args" />',
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Checkbox },
    setup() {
      const sm = ref(true);
      const md = ref(true);
      const lg = ref(true);
      return { sm, md, lg };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Checkbox v-model="sm" size="sm" label="Small checkbox" />
        <Checkbox v-model="md" size="md" label="Medium checkbox" />
        <Checkbox v-model="lg" size="lg" label="Large checkbox" />
      </div>
    `,
  }),
};

export const Disabled: Story = {
  render: () => ({
    components: { Checkbox },
    setup() {
      const checked = ref(true);
      return { checked };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Checkbox v-model="checked" disabled label="Disabled checked" />
        <Checkbox :model-value="false" disabled label="Disabled unchecked" />
      </div>
    `,
  }),
};

export const WithError: Story = {
  render: () => ({
    components: { Checkbox },
    setup() {
      const checked = ref(false);
      return { checked };
    },
    template: '<Checkbox v-model="checked" error error-message="You must accept the terms" label="Accept terms" />',
  }),
};
