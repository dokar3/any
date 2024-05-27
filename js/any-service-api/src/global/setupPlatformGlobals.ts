/// #if platform === 'android'
import { setupAndroidGlobals } from "../global/Globals.android";
/// #elif platform === 'desktop'
import { setupDesktopGlobals } from "../global/Globals.desktop";
/// #elif platform === 'browser'
import { setupBrowserGlobals } from "../global/Globals.browser";
/// #endif

/**
 * This should be called by the compiler module.
 */
export function _setupPlatformGlobals() {
  /// #if platform === 'android'
  setupAndroidGlobals();
  /// #elif platform === 'desktop'
  setupDesktopGlobals();
  /// #elif platform === 'browser'
  setupBrowserGlobals();
  /// #else
  throw new Error("Unknown platform: " + globalThis.platform);
  /// #endif
}
