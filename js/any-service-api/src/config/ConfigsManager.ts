import { ConfigsUpdater } from "./ConfigsUpdater";

/**
 * Service configurations manager.
 *
 * @since 0.1.0
 */
class ConfigsManager {
  private configs: any;

  /**
   * @param {object} configs Configurations object.
   * @param {ConfigsUpdater} updater The configurations updater.
   */
  constructor(configs: any, updater: ConfigsUpdater) {
    this.configs = this.createObservableConfigs(configs ?? {}, updater);
  }

  /**
   * Create observable configurations object. When config value has changed, the latest configs
   * will be saved through the updater.
   *
   * @param {object} configs Configurations object.
   * @param {ConfigsUpdater} updater The configurations updater.
   * @returns {object} The observable configurations object.
   *
   * @private
   */
  createObservableConfigs(configs: any, updater: ConfigsUpdater): any {
    const observable = {};
    const keys = Object.keys(configs);
    for (const key of keys) {
      observable[`__${key}`] = configs[key];
      Object.defineProperty(observable, key, {
        get() {
          return this[`__${key}`];
        },
        set(value) {
          this[`__${key}`] = value;
          const updated = {};
          for (const k of keys) {
            updated[k] = observable[k];
          }
          updater.update(updated);
        },
      });
    }

    Object.seal(observable);

    return observable;
  }

  /**
   * Get the observable configurations object. Every configuration change will trigger
   * the updater.update() function.
   *
   * @returns {object} The observable configurations object.
   * @since 0.1.0
   */
  observableConfigs(): any {
    return this.configs;
  }
}

/** @internal */
export { ConfigsManager };
