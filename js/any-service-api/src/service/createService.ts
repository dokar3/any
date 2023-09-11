import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { AnyService } from "./AnyService";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";
import { ServiceFeatures } from "./feature/Features";

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
 * This should be called by the compiler module.
 */
export function _createService(
  features: ServiceFeatures,
  manifest: ServiceManifest,
  configs: any,
  manifestUpdater: ManifestUpdater,
  configsUpdater: ConfigsUpdater,
  progressUpdater: LoadingProgressUpdater
): AnyService {
  return new AnyService({
    features: features,
    params: {
      manifest: manifest,
      configs: configs,
      manifestUpdater: manifestUpdater ?? globalThis.manifestUpdater,
      configsUpdater: configsUpdater ?? globalThis.configsUpdater,
      progressUpdater: progressUpdater ?? globalThis.progressUpdater,
    },
  });
}
