import { ManifestUpdater } from "../manifest/ManifestUpdater";

declare const __ANY_MANIFEST_UPDATER__: any;

class AndroidManifestUpdater extends ManifestUpdater {
  updateName(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateName(newValue);
  }

  updateDeveloper(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateDeveloper(newValue);
  }

  updateDeveloperUrl(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateDeveloperUrl(newValue);
  }

  updateDeveloperAvatar(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateDeveloperAvatar(newValue);
  }

  updateDescription(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateDescription(newValue);
  }

  updateHomepage(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateHomepage(newValue);
  }

  updateChangelog(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateChangelog(newValue);
  }

  updateIsPageable(newValue: boolean) {
    __ANY_MANIFEST_UPDATER__.updateIsPageable(newValue);
  }

  updatePostsViewType(newValue: string): void {
    __ANY_MANIFEST_UPDATER__.updatePostsViewType(newValue);
  }

  updateMediaAspectRatio(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateMediaAspectRatio(newValue);
  }

  updateIcon(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateIcon(newValue);
  }

  updateHeaderImage(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateHeaderImage(newValue);
  }

  updateThemeColor(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateThemeColor(newValue);
  }

  updateDarkThemeColor(newValue: string) {
    __ANY_MANIFEST_UPDATER__.updateDarkThemeColor(newValue);
  }

  updateLanguages(newValue: string[]) {
    __ANY_MANIFEST_UPDATER__.updateLanguages(newValue);
  }

  updateSupportedPostUrls(newValue: string[]) {
    __ANY_MANIFEST_UPDATER__.updateSupportedPostUrls(newValue);
  }

  updateSupportedUserUrls(newValue: string[]) {
    __ANY_MANIFEST_UPDATER__.updateSupportedUserUrls(newValue);
  }
}

export { AndroidManifestUpdater };
