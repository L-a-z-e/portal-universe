import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Modal } from './Modal';
import { Button } from '../Button';

const meta: Meta<typeof Modal> = {
  title: 'Components/Feedback/Modal',
  component: Modal,
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg', 'xl'],
    },
    showClose: {
      control: 'boolean',
    },
    closeOnBackdrop: {
      control: 'boolean',
    },
    closeOnEscape: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Modal>;

const ModalWithTrigger = ({
  size,
  title,
  children,
}: {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  title?: string;
  children: React.ReactNode;
}) => {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Open Modal</Button>
      <Modal open={open} onClose={() => setOpen(false)} size={size} title={title}>
        {children}
      </Modal>
    </>
  );
};

export const Default: Story = {
  render: () => (
    <ModalWithTrigger title="Modal Title">
      <p>This is the modal content. You can put any content here.</p>
    </ModalWithTrigger>
  ),
};

export const Small: Story = {
  render: () => (
    <ModalWithTrigger size="sm" title="Small Modal">
      <p>This is a small modal.</p>
    </ModalWithTrigger>
  ),
};

export const Large: Story = {
  render: () => (
    <ModalWithTrigger size="lg" title="Large Modal">
      <p>This is a large modal with more content space.</p>
      <p className="mt-2">You can fit more content here.</p>
    </ModalWithTrigger>
  ),
};

export const ExtraLarge: Story = {
  render: () => (
    <ModalWithTrigger size="xl" title="Extra Large Modal">
      <p>This is an extra large modal.</p>
      <p className="mt-2">It provides even more space for complex content.</p>
    </ModalWithTrigger>
  ),
};

export const WithForm: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    return (
      <>
        <Button onClick={() => setOpen(true)}>Open Form Modal</Button>
        <Modal open={open} onClose={() => setOpen(false)} title="Create Account">
          <form className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-body mb-1">
                Name
              </label>
              <input
                type="text"
                className="w-full px-3 py-2 border border-border-default rounded-md bg-bg-card text-text-body"
                placeholder="Enter your name"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-body mb-1">
                Email
              </label>
              <input
                type="email"
                className="w-full px-3 py-2 border border-border-default rounded-md bg-bg-card text-text-body"
                placeholder="Enter your email"
              />
            </div>
            <div className="flex justify-end gap-2 mt-6">
              <Button variant="secondary" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button type="submit">Submit</Button>
            </div>
          </form>
        </Modal>
      </>
    );
  },
};

export const ConfirmDialog: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    return (
      <>
        <Button variant="danger" onClick={() => setOpen(true)}>
          Delete Item
        </Button>
        <Modal open={open} onClose={() => setOpen(false)} title="Confirm Delete" size="sm">
          <p className="text-text-body">
            Are you sure you want to delete this item? This action cannot be undone.
          </p>
          <div className="flex justify-end gap-2 mt-6">
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={() => setOpen(false)}>
              Delete
            </Button>
          </div>
        </Modal>
      </>
    );
  },
};

export const NoCloseButton: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    return (
      <>
        <Button onClick={() => setOpen(true)}>Open Modal</Button>
        <Modal
          open={open}
          onClose={() => setOpen(false)}
          title="No Close Button"
          showClose={false}
        >
          <p>This modal has no close button. Use the button below to close.</p>
          <div className="flex justify-end mt-6">
            <Button onClick={() => setOpen(false)}>Close</Button>
          </div>
        </Modal>
      </>
    );
  },
};
