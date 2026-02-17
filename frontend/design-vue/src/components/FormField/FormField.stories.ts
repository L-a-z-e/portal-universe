import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { FormField } from './index';
import { Input } from '../Input';
import { Select } from '../Select';
import { Checkbox } from '../Checkbox';

const meta: Meta<typeof FormField> = {
  title: 'Components/Form/FormField',
  component: FormField,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    required: {
      control: 'boolean',
    },
    error: {
      control: 'boolean',
    },
    disabled: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof FormField>;

export const WithInput: Story = {
  render: () => ({
    components: { FormField, Input },
    setup() {
      const value = ref('');
      return { value };
    },
    template: `
      <FormField label="Email Address" helper-text="We'll never share your email.">
        <Input v-model="value" type="email" placeholder="you@example.com" />
      </FormField>
    `,
  }),
};

export const Required: Story = {
  render: () => ({
    components: { FormField, Input },
    setup() {
      const value = ref('');
      return { value };
    },
    template: `
      <FormField label="Username" required>
        <Input v-model="value" placeholder="Enter username" />
      </FormField>
    `,
  }),
};

export const WithError: Story = {
  render: () => ({
    components: { FormField, Input },
    setup() {
      const value = ref('invalid-email');
      return { value };
    },
    template: `
      <FormField label="Email" error error-message="Please enter a valid email address">
        <Input v-model="value" type="email" :error="true" />
      </FormField>
    `,
  }),
};

export const Disabled: Story = {
  render: () => ({
    components: { FormField, Input },
    setup() {
      const value = ref('Read only value');
      return { value };
    },
    template: `
      <FormField label="Locked Field" disabled>
        <Input v-model="value" disabled />
      </FormField>
    `,
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { FormField, Input },
    setup() {
      const sm = ref('');
      const md = ref('');
      const lg = ref('');
      return { sm, md, lg };
    },
    template: `
      <div class="space-y-6">
        <FormField label="Small" size="sm" helper-text="Small size field">
          <Input v-model="sm" size="sm" placeholder="Small input" />
        </FormField>
        <FormField label="Medium" size="md" helper-text="Medium size field">
          <Input v-model="md" size="md" placeholder="Medium input" />
        </FormField>
        <FormField label="Large" size="lg" helper-text="Large size field">
          <Input v-model="lg" size="lg" placeholder="Large input" />
        </FormField>
      </div>
    `,
  }),
};

export const WithSelect: Story = {
  render: () => ({
    components: { FormField, Select },
    setup() {
      const value = ref(null);
      const options = [
        { label: 'United States', value: 'us' },
        { label: 'Canada', value: 'ca' },
        { label: 'Mexico', value: 'mx' },
      ];
      return { value, options };
    },
    template: `
      <FormField label="Country" required helper-text="Select your country of residence">
        <Select v-model="value" :options="options" placeholder="Select country" />
      </FormField>
    `,
  }),
};

export const ComplexForm: Story = {
  render: () => ({
    components: { FormField, Input, Select, Checkbox },
    setup() {
      const name = ref('');
      const email = ref('');
      const country = ref(null);
      const agree = ref(false);
      const countries = [
        { label: 'United States', value: 'us' },
        { label: 'Canada', value: 'ca' },
        { label: 'United Kingdom', value: 'uk' },
      ];
      return { name, email, country, agree, countries };
    },
    template: `
      <form class="space-y-6 max-w-md">
        <FormField label="Full Name" required>
          <Input v-model="name" placeholder="John Doe" />
        </FormField>
        <FormField label="Email" required helper-text="We'll send confirmation to this email">
          <Input v-model="email" type="email" placeholder="john@example.com" />
        </FormField>
        <FormField label="Country" required>
          <Select v-model="country" :options="countries" placeholder="Select country" />
        </FormField>
        <FormField>
          <Checkbox v-model="agree" label="I agree to the terms and conditions" />
        </FormField>
      </form>
    `,
  }),
};
