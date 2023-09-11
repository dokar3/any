import { ConfigFeature } from "./ConfigFeature";
import { PostFeature } from "./PostFeature";
import { UserFeature } from "./UserFeature";

/**
 * The features of service.
 *
 * @since 0.2.0
 */
export type ServiceFeatures = {
  /**
   * The feature to fetch and parser post content, fresh posts and comments, etc.
   *
   * @since 0.2.0
   */
  post: PostFeature;
  /**
   * The feature to fetch and parser user, user's posts, etc.
   *
   * @since 0.2.0
   */
  user?: UserFeature;
  /**
   * The validator to validate service configs after it's changed by user.
   *
   * @since 0.2.0
   */
  config?: ConfigFeature;
};
