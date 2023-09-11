/**
 * Configuration validation error.
 *
 * @property {string} key The config key.
 * @property {string} reason The error message.
 * @since 0.1.0
 */
export class ValidationError {
  /**
   * The config key.
   *
   * @since 0.1.0
   */
  key: string;

  /**
   * The error reason.
   *
   * @since 0.1.0
   */
  message: string;

  constructor({ key, message }: { key: string; message: string }) {
    this.key = key;
    this.message = message;
  }
}

/**
 * The service config feature.
 *
 * @since 0.2.0
 */
export type ConfigFeature = {
  /**
   * Validate service configs.
   *
   * @returns {ValidationError|ValidationError[]|null} Failures, null if all pass.
   * @since 0.2.0
   */
  validate(): ValidationError | ValidationError[] | null;
};
