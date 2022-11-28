import { Checksums } from "./Checksums";

/**
 * Fields in the service manifest.
 *
 * @since 0.1.0
 */
export class ServiceManifest {
  /**
   * [readonly] The service id.
   *
   * @since 0.1.0
   */
  readonly id: string;

  /**
   * The service name.
   *
   * @since 0.1.0
   */
  name: string;

  /**
   * The developer name.
   *
   * @since 0.1.0
   */
  developer: string;

  /**
   * The developer url.
   *
   * @since 0.1.0
   */
  developerUrl: string | null;

  /**
   * The avatar url of developer
   *
   * @since 0.1.0
   */
  developerAvatar: string | null;

  /**
   * The description text. Markdown is supported.
   *
   * @since 0.1.0
   */
  description: string;

  /**
   * The home page url.
   *
   * @since 0.1.0
   */
  homepage: string | null;

  /**
   * The changelog. Supported contents:
   * 1. Raw text or markdown text
   * 2. Local text file path or markdown file path
   * 3. Http url, starts with 'http://' or 'https://'
   *
   * @since 0.1.0
   */
  changelog: string | null;

  /**
   * [readonly] The service version.
   *
   * @since 0.1.0
   */
  readonly version: string;

  /**
   * [readonly] The minimum version requirement of any-api.
   *
   * @since 0.1.0
   */
  readonly minApiVersion: string;

  /**
   * [readonly] The maximum version requirement of any-api.
   *
   * @since 0.1.0
   */
  readonly maxApiVersion: string;

  /**
   * [readonly] The checksums of the main.js file.
   *
   * @since 0.1.0
   */
  readonly mainChecksums: Checksums;

  /**
   * true if this service supports loading multiple pages of posts.
   *
   * @since 0.1.0
   */
  isPageable: boolean;

  /**
   * Post list view type. Possible values: 'list', 'grid', 'card', 'full-width'.
   *
   * @since 0.1.0
   */
  postsViewType: string | null;

  /**
   * Default aspect ratio for the post media objects (thumbnails).
   * Supported formats: '400:300', '400x300', '1.333333'.
   *
   * @since 0.1.0
   */
  mediaAspectRatio: string;

  /**
   * The service icon url.
   *
   * @since 0.1.0
   */
  icon: string | null;

  /**
   * The header image url.
   *
   * @since 0.1.0
   */
  headerImage: string | null;

  /**
   * The service theme color hex string, e.g. '#FF6067E6'.
   *
   * @since 0.1.0
   */
  themeColor: string | null;

  /**
   * The service dark theme color hex string, e.g. '#FFA689F8'.
   *
   * @since 0.1.0
   */
  darkThemeColor: string | null;

  /**
   * IETF BCP 47 language tags of the service or its content. e.g. ['en', 'en-US', 'zh'].
   * Visit {@link https://en.wikipedia.org/wiki/IETF_language_tag} for more details about
   * the language tag.
   *
   * @since 0.1.0
   */
  languages: string[] | null;

  /**
   * An array of supported post urls.
   *
   * @since 0.1.0
   */
  supportedPostUrls: string[] | null;

  /**
   * An array of supported user urls.
   *
   * @since 0.1.0
   */
  supportedUserUrls: string[] | null;

  /**
   * If true, 'ConfigFeature.validateConfigs()' will always be called before adding the service,
   * defaults to false. If 'validateConfigs()' is not implemented, validation will be skipped.
   *
   * @since 0.1.0
   */
  readonly forceConfigsValidation: boolean | null;
}
