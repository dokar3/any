import { ManifestUpdater, ServiceManifest } from "any-service-api";

export class MemoryManifestUpdater extends ManifestUpdater {
  readonly manifest: ServiceManifest;

  constructor(manifest: ServiceManifest) {
    super();
    this.manifest = manifest;
  }

  updateName(newValue: string): void {
    this.manifest.name = newValue;
  }

  updateDeveloper(newValue: string): void {
    this.manifest.developer = newValue;
  }

  updateDeveloperUrl(newValue: string | null): void {
    this.manifest.developerUrl = newValue;
  }

  updateDeveloperAvatar(newValue: string | null): void {
    this.manifest.developerAvatar = newValue;
  }

  updateDescription(newValue: string): void {
    this.manifest.description = newValue;
  }

  updateHomepage(newValue: string | null): void {
    this.manifest.homepage = newValue;
  }

  updateChangelog(newValue: string | null): void {
    this.manifest.changelog = newValue;
  }

  updateIsPageable(newValue: boolean): void {
    this.manifest.isPageable = newValue;
  }

  updateViewType(newValue: string | null): void {
    this.manifest.viewType = newValue;
  }

  updateMediaAspectRatio(newValue: string): void {
    this.manifest.mediaAspectRatio = newValue;
  }

  updateIcon(newValue: string | null): void {
    this.manifest.icon = newValue;
  }

  updateHeaderImage(newValue: string | null): void {
    this.manifest.headerImage = newValue;
  }

  updateThemeColor(newValue: string | null): void {
    this.manifest.themeColor = newValue;
  }

  updateDarkThemeColor(newValue: string | null): void {
    this.manifest.darkThemeColor = newValue;
  }

  updateLanguages(newValue: string[] | null): void {
    this.manifest.languages = newValue;
  }

  updateSupportedPostUrls(newValue: string[] | null): void {
    this.manifest.supportedPostUrls = newValue;
  }

  updateSupportedUserUrls(newValue: string[] | null): void {
    this.manifest.supportedUserUrls = newValue;
  }
}
