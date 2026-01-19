import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Radio } from './index';

const meta: Meta<typeof Radio> = {
  title: 'Components/Form/Radio',
  component: Radio,
  tags: ['autodocs'],
  argTypes: {
    direction: {
      control: 'select',
      options: ['horizontal', 'vertical'],
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    disabled: {
      control: 'boolean',
    },
    error: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Radio>;

const defaultOptions = [
  { label: 'Option A', value: 'a' },
  { label: 'Option B', value: 'b' },
  { label: 'Option C', value: 'c' },
];

export const Default: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const selected = ref('a');
      return { selected, options: defaultOptions };
    },
    template: '<Radio v-model="selected" :options="options" name="default" />',
  }),
};

export const Horizontal: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const selected = ref('a');
      return { selected, options: defaultOptions };
    },
    template: '<Radio v-model="selected" :options="options" name="horizontal" direction="horizontal" />',
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const sm = ref('a');
      const md = ref('a');
      const lg = ref('a');
      return { sm, md, lg, options: defaultOptions };
    },
    template: `
      <div class="flex flex-col gap-6">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Small</h3>
          <Radio v-model="sm" :options="options" name="sm" size="sm" direction="horizontal" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Medium</h3>
          <Radio v-model="md" :options="options" name="md" size="md" direction="horizontal" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Large</h3>
          <Radio v-model="lg" :options="options" name="lg" size="lg" direction="horizontal" />
        </div>
      </div>
    `,
  }),
};

export const WithDisabledOption: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const selected = ref('a');
      const options = [
        { label: 'Available', value: 'a' },
        { label: 'Unavailable', value: 'b', disabled: true },
        { label: 'Premium', value: 'c' },
      ];
      return { selected, options };
    },
    template: '<Radio v-model="selected" :options="options" name="disabled-option" />',
  }),
};

export const AllDisabled: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const selected = ref('a');
      return { selected, options: defaultOptions };
    },
    template: '<Radio v-model="selected" :options="options" name="all-disabled" disabled />',
  }),
};

export const WithError: Story = {
  render: () => ({
    components: { Radio },
    setup() {
      const selected = ref(null);
      return { selected, options: defaultOptions };
    },
    template: '<Radio v-model="selected" :options="options" name="error" error error-message="Please select an option" />',
  }),
};
