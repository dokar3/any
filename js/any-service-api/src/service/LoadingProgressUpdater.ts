export type OnProgressUpdate = (
  progress: number,
  message: string | undefined
) => void;

/**
 * The loading progress updater, call update() to notify ui if needed.
 *
 * @since 0.1.0
 */
export class LoadingProgressUpdater {
  /** @internal */
  private updateCallback: OnProgressUpdate;

  /**
   * @param {OnProgressUpdate} updateCallback Update callback.
   */
  constructor(updateCallback: OnProgressUpdate) {
    this.updateCallback = updateCallback;
  }

  /**
   * Update the loading progress.
   *
   * @param {number} progress  Progress value in Double, from 0.0 to 1.0, inclusive.
   * @param {string} [message] Optional message to describe current loading state.
   */
  update(progress: number, message: string | undefined): void {
    this.updateCallback(progress, message);
  }
}
