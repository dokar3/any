import { Post } from "./Post";

/**
 * Properties of {@link Comment}
 *
 * @since 0.1.0
 */
export type CommentProps = {
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
   * Comment media list.
   *
   * @since 0.1.0
   */
  media?: Post.Media[] | null;

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
};

/**
 * The post comment entity.
 *
 * @since 0.1.0
 */
export class Comment {
  username: string;
  content: string;
  avatar?: string | null;
  media?: Post.Media[] | null;
  date?: number | null;
  upvotes?: number | null;
  downvotes?: number | null;
  replies?: Comment[] | null;

  constructor({
    username,
    content,
    avatar,
    media,
    date,
    upvotes,
    downvotes,
    replies,
  }: CommentProps) {
    this.username = username;
    this.content = content;
    this.avatar = avatar;
    this.media = media;
    this.date = date;
    this.upvotes = upvotes;
    this.downvotes = downvotes;
    this.replies = replies;
  }
}
