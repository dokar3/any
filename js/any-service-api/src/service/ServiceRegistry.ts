import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { AnyService } from "./AnyService";
import { AnyConfigFeature } from "./feature/ConfigFeature";
import { Feature } from "./feature/Feature";
import { AnyPostFeature } from "./feature/PostFeature";
import { AnyUserFeature } from "./feature/UserFeature";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";

/// #if platform == 'android'
// #code import { setupAndroidGlobals } from "../global/Globals.android";
// #code setupAndroidGlobals();
/// #elif platform == 'desktop'
// #code import { setupDesktopGlobals } from "../global/Globals.desktop";
// #code setupDesktopGlobals();
/// #elif platform == 'browser'
// #code import { setupBrowserGlobals } from "../global/Globals.browser";
// #code setupBrowserGlobals();
/// #else
// #code throw new Error("Unknown platform: " + platform);
/// #endif

/**
 * The service registry used to register and create service.
 *
 * @since 0.1.0
 */
export class ServiceRegistry {
  /**
   * Register the implemented service.
   *
   * @param {AnyService} serviceClass The service class to register.
   * @since 0.1.0
   */
  static register(serviceClass: typeof AnyService) {
    if (serviceClass == null) {
      throw new Error(`Cannot register the '${serviceClass}'`);
    }

    // Export createService() to the global scope
    this._exportCreateService(serviceClass);

    // Export builtin features to the global scope
    this._exportFeature("AnyConfigFeature", AnyConfigFeature);
    this._exportFeature("AnyPostFeature", AnyPostFeature);
    this._exportFeature("AnyUserFeature", AnyUserFeature);
  }

  /** @internal */
  private static _exportCreateService(Service: typeof AnyService) {
    if (globalThis.hasOwnProperty("createService")) {
      return;
    }
    Object.defineProperty(globalThis, "createService", {
      enumerable: false,
      configurable: false,
      writable: false,
      value: function (
        manifest: ServiceManifest,
        configs: any,
        manifestUpdater: ManifestUpdater,
        configsUpdater: ConfigsUpdater,
        progressUpdater: LoadingProgressUpdater
      ) {
        return new Service({
          manifest: manifest,
          configs: configs,
          manifestUpdater: manifestUpdater ?? globalThis.manifestUpdater,
          configsUpdater: configsUpdater ?? globalThis.configsUpdater,
          progressUpdater: progressUpdater ?? globalThis.progressUpdater,
        });
      },
    });
  }

  /** @internal */
  private static _exportFeature(name: string, clz: typeof Feature) {
    if (globalThis.hasOwnProperty(name)) {
      return;
    }
    Object.defineProperty(globalThis, name, {
      enumerable: false,
      configurable: false,
      writable: false,
      value: clz,
    });
  }
}
