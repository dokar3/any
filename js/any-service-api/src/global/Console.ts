/**
 * Console callback.
 *
 * @since 0.1.0
 * @internal
 */
export type ConsoleCallback = (message: string) => any;

/**
 * @since 0.1.0
 */
class Console {
  /** @internal */
  private logImpl: ConsoleCallback;

  /** @internal */
  private infoImpl: ConsoleCallback;

  /** @internal */
  private warnImpl: ConsoleCallback;

  /** @internal */
  private errorImpl: ConsoleCallback;

  /**
   * @param {ConsoleCallback} logImpl Call on console.log().
   * @param {ConsoleCallback} infoImpl Call on console.info().
   * @param {ConsoleCallback} warnImpl Call on console.warn().
   * @param {ConsoleCallback} errorImpl Call on console.error().
   *
   * @internal
   */
  constructor(
    logImpl: ConsoleCallback,
    infoImpl: ConsoleCallback,
    warnImpl: ConsoleCallback,
    errorImpl: ConsoleCallback
  ) {
    this.logImpl = logImpl;
    this.infoImpl = infoImpl;
    this.warnImpl = warnImpl;
    this.errorImpl = errorImpl;
  }

  /**
   * Print log message.
   *
   * @param {string[]} data Messages to print, will be joined with spaces.
   * @since 0.1.0
   */
  log(...data: string[]): void {
    this.logImpl(data.join(" "));
  }

  /**
   * Print info message.
   *
   * @param {string[]} data Messages to print, will be joined with spaces.
   * @since 0.1.0
   */
  info(...data: string[]): void {
    this.infoImpl(data.join(" "));
  }

  /**
   * Print warning message.
   *
   * @param {string[]} data Messages to print, will be joined with spaces.
   * @since 0.1.0
   */
  warn(...data: string[]): void {
    this.warnImpl(data.join(" "));
  }

  /**
   * Print error message.
   *
   * @param {string[]} data Messages to print, will be joined with spaces.
   * @since 0.1.0
   */
  error(...data: string[]): void {
    this.errorImpl(data.join(" "));
  }
}

export { Console };
