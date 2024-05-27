import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { AnyService } from "./AnyService";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";
import { ServiceFeatures } from "./feature/Features";

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
