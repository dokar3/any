/**
 * The user entity.
 *
 * @since 0.1.0
 */
export class User {
  /**
   * The user id, used to identify and fetch user's posts.
   *
   * @since 0.1.0
   */
  id: string;

  /**
   * The user name.
   *
   * @since 0.1.0
   */
  name: string;

  /**
   * The alternative username.
   *
   * @since 0.1.0
   */
  alternativeName?: string | null;

  /**
   * The user url.
   *
   * @since 0.1.0
   */
  url?: string | null;

  /**
   * the avatar url.
   *
   * @since 0.1.0
   */
  avatar?: string | null;

  /**
   * The banner/header image url.
   *
   * @since 0.1.0
   */
  banner?: string | null;

  /**
   * Description/bio text.
   *
   * @since 0.1.0
   */
  description?: string | null;

  /**
   * The follower count.
   *
   * @since 0.1.0
   */
  followerCount?: number | null;

  /**
   * The following count.
   *
   * @since 0.1.0
   */
  followingCount?: number | null;

  /**
   * All post count.
   *
   * @since 0.1.0
   */
  postCount?: number | null;

  /**
   * The copy constructor.
   *
   * @param other The source object to copy.
   * @since 0.1.0
   */
  constructor(other: User) {
    Object.assign(this, other);
  }
}
