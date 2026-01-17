import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Switch } from './index';

const meta: Meta<typeof Switch> = {
  title: 'Components/Form/Switch',
  component: Switch,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    activeColor: {
      control: 'select',
      options: ['primary', 'success', 'warning', 'error'],
    },
    labelPosition: {
      control: 'select',
      options: ['left', 'right'],
    },
    disabled: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Switch>;

export const Default: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const enabled = ref(false);
      return { enabled };
    },
    template: '<Switch v-model="enabled" label="Enable notifications" />',
  }),
};

export const Checked: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const enabled = ref(true);
      return { enabled };
    },
    template: '<Switch v-model="enabled" label="Dark mode" />',
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const sm = ref(true);
      const md = ref(true);
      const lg = ref(true);
      return { sm, md, lg };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Switch v-model="sm" size="sm" label="Small switch" />
        <Switch v-model="md" size="md" label="Medium switch" />
        <Switch v-model="lg" size="lg" label="Large switch" />
      </div>
    `,
  }),
};

export const Colors: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const primary = ref(true);
      const success = ref(true);
      const warning = ref(true);
      const error = ref(true);
      return { primary, success, warning, error };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Switch v-model="primary" active-color="primary" label="Primary" />
        <Switch v-model="success" active-color="success" label="Success" />
        <Switch v-model="warning" active-color="warning" label="Warning" />
        <Switch v-model="error" active-color="error" label="Error" />
      </div>
    `,
  }),
};

export const LabelPosition: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const left = ref(true);
      const right = ref(true);
      return { left, right };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Switch v-model="left" label-position="left" label="Label on left" />
        <Switch v-model="right" label-position="right" label="Label on right" />
      </div>
    `,
  }),
};

export const Disabled: Story = {
  render: () => ({
    components: { Switch },
    setup() {
      const on = ref(true);
      const off = ref(false);
      return { on, off };
    },
    template: `
      <div class="flex flex-col gap-4">
        <Switch v-model="on" disabled label="Disabled (on)" />
        <Switch v-model="off" disabled label="Disabled (off)" />
      </div>
    `,
  }),
};
