import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";

/**
 * Service constructor parameters.
 *
 * @internal
 * @since 0.1.0
 */
export type ServiceParams = {
  /**
   * The manifest object.
   */
  manifest: ServiceManifest;
  /**
   * The service configurations object.
   */
  configs: any;
  /**
   * The service manifest updater.
   */
  manifestUpdater: ManifestUpdater;
  /**
   * The service configurations updater.
   */
  configsUpdater: ConfigsUpdater;
  /**
   * The loading progress updater.
   */
  progressUpdater: LoadingProgressUpdater;
};
