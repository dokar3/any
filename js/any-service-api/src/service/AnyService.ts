import { ConfigsManager } from "../config/ConfigsManager";
import { ManifestManager } from "../manifest/ManifestManager";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";
import { ServiceParams } from "./ServiceParams";
import { ServiceFeatures } from "./feature/Features";

/**
 * The Any service.
 *
 * @since 0.1.0
 */
export class AnyService {
  /**
   * The features of this service.
   */
  readonly features: ServiceFeatures;

  /** @internal */
  private readonly _progressUpdater: LoadingProgressUpdater;

  /**
   * The service manifest.
   *
   * @example
   * // Read:
   * const serviceName = manifest.name;
   * // Update a mutable field
   * manifest.description = "New description here";
   * // ❌ Update an immutable field is not allowed
   * manifest.id = "Not allowed";
   *
   * @since 0.1.0
   */
  readonly manifest: ServiceManifest;

  /**
   * The configurations object. Only fields defined in the manifest can be read or updated.
   *
   * @example
   * // Read:
   * const value = this.configs.myKey1;
   * // Update:
   * this.configs.myKey1 = 'My value';
   * this.configs.myKey2 = 12;
   *
   * @since 0.1.0
   */
  readonly configs: any;

  /**
   * @param {ServiceParams} params Constructor parameters.
   * @internal
   */
  constructor({
    features,
    params,
  }: {
    features: ServiceFeatures;
    params: ServiceParams;
  }) {
    this.features = features;
    this._progressUpdater = params.progressUpdater;

    const manifestManager = new ManifestManager(
      params.manifest,
      params.manifestUpdater
    );
    Object.defineProperty(this, "manifest", {
      get() {
        return manifestManager.observableManifest();
      },
      set(_) {
        throw new Error(
          "Cannot override whole service manifest, " +
            "update single field instead, e.g. 'this.manifest.name = 'New name';'"
        );
      },
    });

    const configsManager = new ConfigsManager(
      params.configs,
      params.configsUpdater
    );
    Object.defineProperty(this, "configs", {
      get() {
        return configsManager.observableConfigs();
      },
      set(_) {
        throw new Error(
          "Cannot override whole configurations, " +
            "update single field instead, e.g. 'configs.key = newValue;'"
        );
      },
    });
  }

  /**
   * Update loading progress.
   *
   * @param {number} progress  Progress value in Double, from 0.0 to 1.0, inclusive.
   * @param {string} [message] Optional message to describe current loading state.
   * @since 0.1.0
   */
  updateLoadingProgress(progress: number, message?: string): void {
    this._progressUpdater.update(progress, message);
  }
}
