import { ConfigsManager } from "../config/ConfigsManager";
import { ServiceManifest } from "../manifest/ServiceManifest";
import { ManifestManager } from "../manifest/ManifestManager";
import { ServiceParams } from "./ServiceParams";
import { LoadingProgressUpdater } from "./LoadingProgressUpdater";
import { Feature } from "./feature/Feature";
import { Features } from "./Features";

/**
 * The Any service. Requiring to extend this class and override the onCreate() function
 * to add {@link Feature}s. The extended class should not contain any logic, instead, put
 * them into {@link Feature}s.
 *
 * @since 0.1.0
 */
export class AnyService {
  /** @internal */
  private _features: Features;

  /** @internal */
  private _progressUpdater: LoadingProgressUpdater;

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
  constructor(params: ServiceParams) {
    this._features = new Features(this);

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

    this.onCreate();
  }

  /**
   * Called when the service is created.
   */
  onCreate(): void {}

  /**
   * Add a feature.
   *
   * @param ctor The feature class.
   */
  addFeature<S extends Feature>(ctor: new (service: AnyService) => S) {
    this._features.add(ctor);
  }

  /**
   * Check if target feature type is added.
   *
   * @param ctor The feature class.
   * @returns True if target feature is added, false otherwise.
   */
  hasFeature<S extends Feature>(
    ctor: new (service: AnyService) => S
  ): boolean {
    return this._features.isAdded(ctor);
  }

  /**
   * Get the specified type of feature.
   *
   * @param ctor The feature class.
   * @returns The feature instance.
   * @throws A error if target feature is not registered.
   */
  getFeature<S extends Feature>(ctor: new (service: AnyService) => S): S {
    return this._features.getOrCreate(ctor);
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
