/**
 * The fetch result.
 *
 * @template T The data type.
 *
 * @since 0.1.0
 */
export class FetchResult<T> {
  /**
   * The data, null if it's an error result.
   *
   * @since 0.1.0
   */
  data?: T;

  /**
   * The error message.
   *
   * @since 0.1.0
   */
  error?: string;

  /**
   * @param ret The result properties.
   */
  private constructor(ret: FetchResult.OkProps<T> | FetchResult.ErrProps) {
    Object.assign(this, ret);
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
   * Create an ok result.
   *
   * @template T The data type.
   *
   * @returns {FetchResult<T>} The result.
   * @since 0.1.0
   */
  static ok<T>({ data }: FetchResult.OkProps<T>): FetchResult<T> {
    return new FetchResult({ data: data });
  }

  /**
   * Create an error result.
   *
   * @returns {FetchResult} The result.
   * @since 0.1.0
   */
  static err({ error }: FetchResult.ErrProps): FetchResult<any> {
    return new FetchResult({ error: error });
  }
}

export namespace FetchResult {
  export type OkProps<T> = {
    /**
     * The result data.
     */
    data: T;
  };

  export type ErrProps = {
    /**
     * The error message.
     */
    error: string;
  };
}
