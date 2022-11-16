type OnConfigsUpdate = (configs: any) => void;

/**
 * The configurations updater, call update() to save the updated configurations.
 *
 * @since 0.1.0
 */
export class ConfigsUpdater {
  /** @internal */
  private updateCallback: OnConfigsUpdate;
  /**
   * @param {OnConfigsUpdate} updateCallback Update callback.
   */
  constructor(updateCallback: OnConfigsUpdate) {
    this.updateCallback = updateCallback;
  }

  /**
   * Update configurations.
   *
   * @param {object} configs The configurations object to update.
   */
  update(configs: any): void {
    this.updateCallback(configs);
  }
}
