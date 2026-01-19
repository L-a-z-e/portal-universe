import type { Meta, StoryObj } from '@storybook/vue3';
import { Container } from './index';

const meta: Meta<typeof Container> = {
  title: 'Components/Layout/Container',
  component: Container,
  tags: ['autodocs'],
  argTypes: {
    maxWidth: {
      control: 'select',
      options: ['sm', 'md', 'lg', 'xl', '2xl', 'full'],
    },
    padding: {
      control: 'select',
      options: ['none', 'sm', 'md', 'lg'],
    },
    centered: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Container>;

export const Default: Story = {
  render: () => ({
    components: { Container },
    template: `
      <Container class="bg-brand-100 dark:bg-brand-900/30 py-8">
        <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow">
          <h2 class="text-lg font-semibold text-text-heading">Default Container</h2>
          <p class="text-text-body">This is the default container with max-width: lg and padding: md.</p>
        </div>
      </Container>
    `,
  }),
};

export const MaxWidths: Story = {
  render: () => ({
    components: { Container },
    template: `
      <div class="space-y-4">
        <Container max-width="sm" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">max-width: sm</div>
        </Container>
        <Container max-width="md" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">max-width: md</div>
        </Container>
        <Container max-width="lg" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">max-width: lg</div>
        </Container>
        <Container max-width="xl" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">max-width: xl</div>
        </Container>
        <Container max-width="2xl" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">max-width: 2xl</div>
        </Container>
      </div>
    `,
  }),
};

export const Padding: Story = {
  render: () => ({
    components: { Container },
    template: `
      <div class="space-y-4">
        <Container padding="none" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">padding: none</div>
        </Container>
        <Container padding="sm" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">padding: sm</div>
        </Container>
        <Container padding="md" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">padding: md</div>
        </Container>
        <Container padding="lg" class="bg-brand-100 dark:bg-brand-900/30 py-4">
          <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">padding: lg</div>
        </Container>
      </div>
    `,
  }),
};

export const NotCentered: Story = {
  render: () => ({
    components: { Container },
    template: `
      <Container max-width="md" :centered="false" class="bg-brand-100 dark:bg-brand-900/30 py-4">
        <div class="bg-white dark:bg-gray-800 p-4 rounded text-center">Not centered (aligned left)</div>
      </Container>
    `,
  }),
};

export const SemanticElements: Story = {
  render: () => ({
    components: { Container },
    template: `
      <Container as="main" class="bg-brand-100 dark:bg-brand-900/30 py-8">
        <Container as="article" max-width="md" padding="none">
          <h1 class="text-2xl font-bold text-text-heading mb-4">Article Title</h1>
          <p class="text-text-body">This container uses semantic HTML elements (main > article) for better accessibility and SEO.</p>
        </Container>
      </Container>
    `,
  }),
};
