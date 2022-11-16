import { Post } from "../../post/Post";
import { FetchResult } from "../../result/FetchResult";
import { PagedResult } from "../../result/PagedResult";
import { User } from "../../user/User";
import { NotImplementedError } from "../../util";
import { Feature } from "./Feature";

/**
 * Parameters for {@link UserFeature.fetchById}
 *
 * @since 0.1.0
 */
export class FetchUserByIdParams {
  /**
   * The the userId.
   *
   * @since 0.1.0
   */
  userId: string;

  /**
   * The copy constructor.
   *
   * @param other The source object to copy.
   */
  constructor(other: FetchUserByIdParams) {
    Object.assign(this, other);
  }
}

/**
 * Parameters for {@link UserFeature.fetchByUrl}
 *
 * @since 0.1.0
 */
export class FetchUserByUrlParams {
  /**
   * The user url.
   *
   * @since 0.1.0
   */
  userUrl: string;

  /**
   * The copy constructor.
   *
   * @param other The source object to copy.
   */
  constructor(other: FetchUserByUrlParams) {
    Object.assign(this, other);
  }
}

/**
 * Parameters for {@link UserFeature.fetchPosts}
 *
 * @since 0.1.0
 */
export class FetchUserPostsParams {
  /**
   * The user id.
   *
   * @since 0.1.0
   */
  userId: string;

  /**
   * The key of the current page, null if it's the initial page.
   *
   * @since 0.1.0
   */
  pageKey: string | number | null;

  /**
   * The copy constructor.
   *
   * @param other The source object to copy.
   */
  constructor(other: FetchUserPostsParams) {
    Object.assign(this, other);
  }
}

/**
 * The feature used to fetch user related data.
 *
 * @since 0.1.0
 */
export class UserFeature extends Feature {
  /**
   * Fetch user by the user id.
   *
   * @param {FetchUserByIdParams} params Fetch parameters.
   * @returns {FetchResult<User>} The user entity.
   * @since 0.1.0
   */
  fetchById(params: FetchUserByIdParams): FetchResult<User> {
    throw new NotImplementedError("UserFeature.fetchById() is not implemented");
  }

  /**
   * Fetch user by the user url.
   *
   * @param {FetchUserByUrlParams} params Fetch parameters.
   * @returns {FetchResult<User>} The user entity.
   * @since 0.1.0
   */
  fetchByUrl(params: FetchUserByUrlParams): FetchResult<User> {
    throw new NotImplementedError(
      "UserFeature.fetchByUrl() is not implemented"
    );
  }

  /**
   * Fetch user posts.
   *
   * @param {FetchUserPostsParams} params Fetch parameters.
   * @returns {PagedResult<Post[]>} The posts result.
   * @since 0.1.0
   */
  fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
    throw new NotImplementedError(
      "UserService.fetchPosts() is not implemented"
    );
  }
}
