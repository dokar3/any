import {
  AnyService,
  ConfigsUpdater,
  LoadingProgressUpdater,
  ServiceManifest,
  ServiceRegistry
} from "any-service-api";
import * as fs from "fs";
import * as path from "path";
import * as process from "process";
import { CachedHttpInterceptor } from "./CachedHttpRequestHandler";
import { MemoryManifestUpdater } from "./MemoryManifestUpdater";
import { setupTestGlobals } from "./TestGlobals";
/**
 * Create a test service in the node environment.
 *
 * @param serviceClass The service class.
 * @param manifestPath The path of the manifest.json relative to the project root directory.
 * @param configs The optional service configs.
 */
export function createTestService({
  serviceClass,
  manifestPath,
  configs = {},
  cachedHttpInterceptor = new CachedHttpInterceptor(),
}: {
  serviceClass: typeof AnyService;
  manifestPath: string;
  configs?: any;
  cachedHttpInterceptor?: CachedHttpInterceptor;
}): AnyService {
  setupTestGlobals();
  global.http.interceptor = cachedHttpInterceptor;

  const manifest = readManifest(manifestPath);
  const manifestUpdater = new MemoryManifestUpdater(manifest);
  const configsUpdater = new ConfigsUpdater((updatedConfigs: any) => {
    Object.assign(configs ?? {}, updatedConfigs);
  });
  const progressUpdater = new LoadingProgressUpdater(
    (progress: number, message: string | undefined) => {
      console.log(`Update loading progress: ${progress} - ${message}`);
    }
  );
  ServiceRegistry.register(serviceClass);
  return globalThis.createService(
    manifest,
    configs,
    manifestUpdater,
    configsUpdater,
    progressUpdater
  );
}

function readManifest(manifestPath: string): ServiceManifest {
  const tried: string[] = [];
  if (!fs.existsSync(manifestPath)) {
    tried.push(manifestPath);
    manifestPath = path.join(process.cwd(), manifestPath);
  }
  if (!fs.existsSync(manifestPath)) {
    tried.push(manifestPath);
    throw "Manifest does not exist, tried paths:\n" + tried.join("\n");
  }
  const text = fs.readFileSync(manifestPath).toString();
  const manifest = new ServiceManifest();
  Object.assign(manifest, JSON.parse(text));
  return manifest;
}
