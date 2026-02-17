import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Select } from './index';

const meta: Meta<typeof Select> = {
  title: 'Components/Form/Select',
  component: Select,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    disabled: {
      control: 'boolean',
    },
    searchable: {
      control: 'boolean',
    },
    clearable: {
      control: 'boolean',
    },
    error: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Select>;

const defaultOptions = [
  { label: 'Apple', value: 'apple' },
  { label: 'Banana', value: 'banana' },
  { label: 'Cherry', value: 'cherry' },
  { label: 'Date', value: 'date' },
  { label: 'Elderberry', value: 'elderberry' },
];

export const Default: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref(null);
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" placeholder="Select a fruit" />',
  }),
};

export const WithLabel: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref(null);
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" label="Favorite Fruit" required />',
  }),
};

export const Searchable: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref(null);
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" searchable placeholder="Search fruits..." />',
  }),
};

export const Clearable: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref('apple');
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" clearable />',
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const sm = ref(null);
      const md = ref(null);
      const lg = ref(null);
      return { sm, md, lg, options: defaultOptions };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Select v-model="sm" :options="options" size="sm" placeholder="Small" />
        <Select v-model="md" :options="options" size="md" placeholder="Medium" />
        <Select v-model="lg" :options="options" size="lg" placeholder="Large" />
      </div>
    `,
  }),
};

export const WithError: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref(null);
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" error error-message="Please select an option" label="Required Field" required />',
  }),
};

export const Disabled: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref('apple');
      return { value, options: defaultOptions };
    },
    template: '<Select v-model="value" :options="options" disabled label="Disabled Select" />',
  }),
};

export const WithDisabledOptions: Story = {
  render: () => ({
    components: { Select },
    setup() {
      const value = ref(null);
      const options = [
        { label: 'Available', value: 'available' },
        { label: 'Unavailable', value: 'unavailable', disabled: true },
        { label: 'Premium Only', value: 'premium', disabled: true },
        { label: 'Free Option', value: 'free' },
      ];
      return { value, options };
    },
    template: '<Select v-model="value" :options="options" placeholder="Select an option" />',
  }),
};
