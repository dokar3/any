/**
 * Service runtime environment.
 *
 * @since 0.1.0
 */
export class Env {
  /**
   * The IETF BCP 47 language tag of the user's Locale. e.g. 'en', 'en-US', 'zh-CN'.
   * Visit {@link https://en.wikipedia.org/wiki/IETF_language_tag} for more details about
   * the language tag.
   *
   * @since 0.1.0
   */
  readonly LANGUAGE: string;

  constructor(other: Env) {
    Object.assign(this, other);
  }
}
