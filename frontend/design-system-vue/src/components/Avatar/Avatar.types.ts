/**
 * Avatar Component Types
 * Used for user profile pictures, author avatars, etc.
 */

export interface AvatarProps {
  /**
   * Image URL
   */
  src?: string;

  /**
   * Alt text for image
   */
  alt?: string;

  /**
   * User name (used for initials fallback)
   */
  name?: string;

  /**
   * Avatar size
   * @default 'md'
   */
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

  /**
   * Online status indicator
   */
  status?: 'online' | 'offline' | 'busy' | 'away';

  /**
   * Avatar shape
   * @default 'circle'
   */
  shape?: 'circle' | 'square';
}