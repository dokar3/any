import { ServiceManifest } from "./ServiceManifest";
import { ManifestUpdater } from "./ManifestUpdater";

class ManifestManager {
  private manifest: ServiceManifest;
  /**
   * @param {ServiceManifest} manifest Service manifest.
   * @param {ManifestUpdater} manifestUpdater Service manifest updater.
   */
  constructor(manifest: ServiceManifest, manifestUpdater: ManifestUpdater) {
    /** @private */
    this.manifest = this.createObservableManifest(manifest, manifestUpdater);
  }

  /**
   * @param {ServiceManifest} manifest The manifest.
   * @param {ManifestUpdater} manifestUpdater The service manifest updater.
   * @returns {ServiceManifest} Observable service manifest object.
   */
  private createObservableManifest(
    manifest: ServiceManifest,
    manifestUpdater: ManifestUpdater
  ): ServiceManifest {
    const observable = {};

    const keys = Object.keys(manifest);
    for (const key of keys) {
      observable[`__${key}`] = manifest[key];
    }

    function defineObservableProp(
      propName: string,
      onUpdate: (newValue: any) => void
    ) {
      Object.defineProperty(observable, propName, {
        get() {
          return observable[`__${propName}`];
        },
        set(value) {
          observable[`__${propName}`] = value;
          onUpdate(value);
        },
      });
    }

    defineObservableProp("id", (_) => {
      throw new Error("Read only field: id");
    });

    defineObservableProp("name", (newValue) => {
      manifestUpdater.updateName(newValue);
    });

    defineObservableProp("developer", (newValue) => {
      manifestUpdater.updateDeveloper(newValue);
    });

    defineObservableProp("developerUrl", (newValue) => {
      manifestUpdater.updateDeveloperUrl(newValue);
    });

    defineObservableProp("developerAvatar", (newValue) => {
      manifestUpdater.updateDeveloperAvatar(newValue);
    });

    defineObservableProp("description", (newValue) => {
      manifestUpdater.updateDescription(newValue);
    });

    defineObservableProp("homepage", (newValue) => {
      manifestUpdater.updateHomepage(newValue);
    });

    defineObservableProp("changelog", (newValue) => {
      manifestUpdater.updateChangelog(newValue);
    });

    defineObservableProp("mainChecksums", (_) => {
      throw new Error("Read only field: mainChecksums");
    });

    defineObservableProp("version", (_) => {
      throw new Error("Read only field: version");
    });

    defineObservableProp("minApiVersion", (_) => {
      throw new Error("Read only field: minApiVersion");
    });

    defineObservableProp("maxApiVersion", (_) => {
      throw new Error("Read only field: maxApiVersion");
    });

    defineObservableProp("isPageable", (newValue) => {
      manifestUpdater.updateIsPageable(newValue);
    });

    defineObservableProp("postsViewType", (newValue) => {
      manifestUpdater.updatePostsViewType(newValue);
    });

    defineObservableProp("mediaAspectRatio", (newValue) => {
      manifestUpdater.updateMediaAspectRatio(newValue);
    });

    defineObservableProp("icon", (newValue) => {
      manifestUpdater.updateIcon(newValue);
    });

    defineObservableProp("headerImage", (newValue) => {
      manifestUpdater.updateHeaderImage(newValue);
    });

    defineObservableProp("themeColor", (newValue) => {
      manifestUpdater.updateThemeColor(newValue);
    });

    defineObservableProp("darkThemeColor", (newValue) => {
      manifestUpdater.updateDarkThemeColor(newValue);
    });

    defineObservableProp("languages", (newValue) => {
      manifestUpdater.updateLanguages(newValue);
    });

    defineObservableProp("supportedPostUrls", (newValue) => {
      manifestUpdater.updateSupportedPostUrls(newValue);
    });

    defineObservableProp("supportedUserUrls", (newValue) => {
      manifestUpdater.updateSupportedUserUrls(newValue);
    });

    Object.seal(observable);

    return observable as ServiceManifest;
  }

  /**
   * Get the observable service manifest which can be used to update manifest fields.
   *
   * @returns {ServiceManifest} Observable service manifest.
   */
  observableManifest(): ServiceManifest {
    return this.manifest;
  }
}

/** @internal */
export { ManifestManager };
