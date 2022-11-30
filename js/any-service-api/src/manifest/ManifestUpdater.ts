/**
 * The service manifest updater. Need to be implemented.
 *
 * @since 0.1.0
 */
export class ManifestUpdater {
  /**
   * Update service name.
   *
   * @param {string} newValue New service name
   * @since 0.1.0
   */
  updateName(newValue: string): void {}

  /**
   * Update developer name.
   *
   * @param {string} newValue New developer name.
   * @since 0.1.0
   */
  updateDeveloper(newValue: string): void {}

  /**
   * Update developer url.
   *
   * @param {string} newValue New developer url.
   * @since 0.1.0
   */
  updateDeveloperUrl(newValue: string | null): void {}

  /**
   * Update developer avatar url.
   *
   * @param {string} newValue New developer avatar url.
   * @since 0.1.0
   */
  updateDeveloperAvatar(newValue: string | null): void {}

  /**
   * Update description.
   *
   * @param {string} newValue New description text.
   * @since 0.1.0
   */
  updateDescription(newValue: string): void {}

  /**
   * Update home page url.
   *
   * @param {string} newValue New url.
   * @since 0.1.0
   */
  updateHomepage(newValue: string | null): void {}

  /**
   * Update the changelog.
   *
   * @param {string} newValue New changelog content.
   * @since 0.1.0
   */
  updateChangelog(newValue: string | null): void {}

  /**
   * Update isPageable state.
   *
   * @param {boolean} newValue New state.
   * @since 0.1.0
   */
  updateIsPageable(newValue: boolean): void {}

  /**
   * Update service post list view type.
   *
   * @param {string|null} newValue New view type.
   * @since 0.1.0
   */
  updatePostsViewType(newValue: string | null): void {}

  /**
   * Update default media aspect ratio.
   *
   * @param {string} newValue New media aspect ratio.
   * @since 0.1.0
   */
  updateMediaAspectRatio(newValue: string): void {}

  /**
   * Update service icon.
   *
   * @param {string|null} newValue New icon url.
   * @since 0.1.0
   */
  updateIcon(newValue: string | null): void {}

  /**
   * Update service header image.
   *
   * @param {string|null} newValue New header image url.
   * @since 0.1.0
   */
  updateHeaderImage(newValue: string | null): void {}

  /**
   * Update service theme color.
   *
   * @param {string|null} newValue New header image url.
   * @since 0.1.0
   */
  updateThemeColor(newValue: string | null): void {}

  /**
   * Update the service dark theme color.
   *
   * @param {string|null} newValue New header image url.
   * @since 0.1.0
   */
  updateDarkThemeColor(newValue: string | null): void {}

  /**
   * Update the language tag list.
   *
   * @param {string[]|null} newValue New language tag list.
   * @since 0.1.0
   */
  updateLanguages(newValue: string[] | null): void {}

  /**
   * Update the supported post url list.
   *
   * @param {string[]|null} newValue New supported url list.
   * @since 0.1.0
   */
  updateSupportedPostUrls(newValue: string[] | null): void {}

  /**
   * Update the supported user url list.
   *
   * @param {string[]|null} newValue New supported url list.
   * @since 0.1.0
   */
  updateSupportedUserUrls(newValue: string[] | null): void {}
}
