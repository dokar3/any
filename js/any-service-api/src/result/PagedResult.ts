/**
 * The paged fetch result with paging support.
 *
 * @template T The data type.
 *
 * @since 0.1.0
 */
export class PagedResult<T> {
  /**
   * The result data.
   *
   * @since 0.1.0
   */
  data?: T;

  /**
   * Pagination key for the previous page.
   *
   * @since 0.1.0
   */
  prevKey?: string | number;

  /**
   * Pagination key for the next page.
   *
   * @since 0.1.0
   */
  nextKey?: string | number;

  /**
   * The error message.
   *
   * @since 0.1.0
   */
  error?: string;

  /**
   * @param ret The result properties.
   */
  private constructor(ret: PagedResult.OkProps<T> | PagedResult.ErrProps) {
    for (const key of Object.keys(ret)) {
      this[key] = ret[key];
    }
  }

  /**
   * Returns true if it's a successful result.
   *
   * @since 0.1.0
   */
  isOk() {
    return this.data !== undefined;
  }

  /**
   * Returns true if it's a failed result.
   *
   * @since 0.1.0
   */
  isErr() {
    return !this.isOk();
  }

  /**
   * Create a successful PagedResult.
   *
   * @template T The data type.
   *
   * @returns {PagedResult<T>} The result.
   * @since 0.1.0
   */
  static ok<T>({
    data,
    prevKey = null,
    nextKey = null,
  }: PagedResult.OkProps<T>): PagedResult<T> {
    return new PagedResult({
      data: data,
      prevKey: prevKey,
      nextKey: nextKey,
    });
  }

  /**
   * Create a successful PagedResult.
   *
   * @returns {PagedResult} The result.
   * @since 0.1.0
   */
  static err({ error }: PagedResult.ErrProps): PagedResult<any> {
    return new PagedResult({ error: error });
  }
}

export namespace PagedResult {
  export type OkProps<T> = {
    /**
     * The result data.
     */
    data: T;
    /**
     * The fetch key of previous page.
     */
    prevKey?: string | number | null;
    /**
     * The fetch key of previous page, null if there is no more data.
     */
    nextKey?: string | number | null;
  };

  export type ErrProps = {
    /**
     * The error message.
     */
    error: string;
  };
}
