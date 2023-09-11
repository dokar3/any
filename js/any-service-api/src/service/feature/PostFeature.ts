import { Comment } from "../../post/Comment";
import { Post } from "../../post/Post";
import { FetchResult } from "../../result/FetchResult";
import { PagedResult } from "../../result/PagedResult";

/**
 * Parameters for {@link PostFeature.fetch}
 *
 * @since 0.1.0
 */
export class FetchPostParams {
  /**
   * The ost url.
   *
   * @since 0.1.0
   */
  url: string;

  /**
   * The copy constructor.
   *
   * @param other The source object to copy.
   */
  constructor(other: FetchPostParams) {
    Object.assign(this, other);
  }
}

/**
 * Parameters for {@link PostFeature.fetchFreshList}
 *
 * @since 0.1.0
 */
export class FetchFreshListParams {
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
  constructor(other: FetchFreshListParams) {
    Object.assign(this, other);
  }
}

/**
 * Parameters for {@link PostFeature.search}
 *
 * @since 0.1.0
 */
export class SearchPostsParams {
  /**
   * The query text.
   *
   * @since 0.1.0
   */
  query: string;

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
  constructor(other: SearchPostsParams) {
    Object.assign(this, other);
  }
}

/**
 * Parameters for {@link PostFeature.fetchComments}
 *
 * @since 0.1.0
 */
export class FetchCommentsParams {
  /**
   * The post url.
   *
   * @since 0.1.0
   */
  postUrl: string;

  /**
   * A load key in the @type {Post} entity.
   *
   * @since 0.1.0
   */
  loadKey: string;

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
  constructor(other: FetchCommentsParams) {
    Object.assign(this, other);
  }
}

/**
 * Fetch post related data.
 * 
 * @since 0.2.0
 */
export type PostFeature = {
  /**
   * Fetch the detailed post.
   *
   * @param {FetchPostParams} params Fetch parameters.
   * @returns {FetchResult<Post>} The post result.
   * @since 0.2.0
   */
  fetch(params: FetchPostParams): FetchResult<Post>;

  /**
   * Search posts.
   *
   * @param {SearchPostsParams} params Fetch parameters.
   * @returns {PagedResult<Post[]>} Search result.
   * @since 0.2.0
   */
  search?(params: SearchPostsParams): PagedResult<Post[]>;

  /**
   * Fetch fresh post list.
   *
   * @param {FetchFreshListParams} params Fetch parameters.
   * @returns {PagedResult<Post[]>} The posts result.
   * @since 0.2.0
   */
  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]>;

  /**
   * Fetch comments.
   *
   * @param {FetchCommentsParams} params Fetch parameters.
   * @returns {PagedResult<Comment[]>} Comments.
   * @since 0.2.0
   */
  fetchComments?(params: FetchCommentsParams): PagedResult<Comment[]>;
};
