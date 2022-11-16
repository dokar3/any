/**
 * The post comment entity.
 *
 * @since 0.1.0
 */
export class Comment {
  /**
   * Username.
   *
   * @since 0.1.0
   */
  username: string;
  /**
   * Comment content.
   *
   * @since 0.1.0
   */
  content: string;

  /**
   * User's avatar url.
   *
   * @since 0.1.0
   */
  avatar?: string | null;

  /**
   * Comment images.
   *
   * @since 0.1.0
   */
  images?: string[] | null;

  /**
   * Created date in milliseconds.
   *
   * @since 0.1.0
   */
  date?: number | null;

  /**
   * Upvote count.
   *
   * @since 0.1.0
   */
  upvotes?: number | null;

  /**
   * Downvote count.
   *
   * @since 0.1.0
   */
  downvotes?: number | null;

  /**
   * Replies.
   *
   * @since 0.1.0
   */
  replies?: Comment[] | null;

  /**
   * The copy constructor.
   *
   * @param other The source comment to copy.
   * @since 0.1.0
   */
  constructor(other: Comment) {
    Object.assign(this, other);
  }
}
