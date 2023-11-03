import { _createService, ServiceManifest } from "any-service-api";
import { features } from "{{MAIN_PATH}}";

globalThis.initService = function(manifest, configs) {
  globalThis.service = _createService(
    features,
    manifest,
    configs,
    manifestUpdater ?? globalThis.manifestUpdater,
    configsUpdater ?? globalThis.configsUpdater,
    progressUpdater ?? globalThis.progressUpdater,
  );
}