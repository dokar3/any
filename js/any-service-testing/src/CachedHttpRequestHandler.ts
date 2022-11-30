import * as crypto from "crypto";
import * as fs from "fs";
import * as path from "path";
import * as process from "process";
import {
  HttpInterceptor,
  HttpRequest,
  HttpRequestHandler,
  HttpResponse,
} from "any-service-api";

const DEFAULT_CACHE_DIR = "./test/.http_cache";
const DEFAULT_MAX_AGE = 1000 * 60 * 60 * 24;

class Cache {
  time: number;

  data: HttpResponse;

  constructor({ time, data }: { time: number; data: HttpResponse }) {
    this.time = time;
    this.data = data;
  }
}

/**
 * This cached interceptor treats all response bodies as text and ONLY it works in
 * the nodejs environment.
 *
 * This should only be used for testing purposes.
 */
export class CachedHttpInterceptor implements HttpInterceptor {
  private cacheDir: string;

  /**
   * Set to false to disable the caching. Defaults to true.
   */
  enabled: boolean;

  /**
   * The max-age of cache item in milliseconds, similar to the 'cache-control'
   * in the response header. Defaults to 24 hours.
   */
  maxAge: number;

  /**
   * If true, the maxAge will be ignored.
   */
  respectCacheControlHeader: boolean;

  constructor(
    {
      cacheDir,
      enabled,
      maxAge,
      respectCacheControlHeader,
    }: {
      cacheDir?: string;
      enabled?: boolean;
      maxAge?: number;
      respectCacheControlHeader?: boolean;
    } = {
      cacheDir: DEFAULT_CACHE_DIR,
      enabled: true,
      maxAge: DEFAULT_MAX_AGE,
      respectCacheControlHeader: false,
    }
  ) {
    this.enabled = enabled ?? true;
    this.cacheDir = cacheDir ?? DEFAULT_CACHE_DIR;
    this.maxAge = maxAge ?? DEFAULT_MAX_AGE;
    this.respectCacheControlHeader = respectCacheControlHeader ?? false;
  }

  intercept(request: HttpRequest, handler: HttpRequestHandler): HttpResponse {
    const cacheFile = path.join(this.cacheDir, this.requestHash(request));
    if (this.enabled && fs.existsSync(cacheFile)) {
      const text = fs.readFileSync(cacheFile).toString();
      const cache: Cache = JSON.parse(text);
      if (this.isCacheValid(cache)) {
        return cache.data;
      }
    }

    const resp = handler.handle(request);
    if (this.enabled && resp.status === 200) {
      let maxAge = this.maxAge;
      if (this.respectCacheControlHeader && resp.headers) {
        const age = this.findMaxAgeFromHeaders(resp.headers);
        maxAge = age !== null ? age : maxAge;
      }
      if (this.maxAge > 0) {
        this.checkCacheDir();
        const cache = new Cache({ time: Date.now(), data: resp });
        fs.writeFileSync(cacheFile, JSON.stringify(cache));
      }
    }

    return resp;
  }

  /**
   * Clear all cached requests.
   */
  clearCache() {
    fs.readdirSync(this.cacheDir).forEach((filename) => {
      fs.rmSync(path.join(this.cacheDir, filename));
    });
  }

  /** @internal */
  private isCacheValid(cache: Cache): boolean {
    if (cache == null || cache.data == null) {
      return false;
    }

    const time = cache.time;
    if (time == null) {
      return true;
    }

    let maxAge = this.maxAge;
    if (this.respectCacheControlHeader && cache.data.headers) {
      const age = this.findMaxAgeFromHeaders(cache.data.headers);
      maxAge = age !== null ? age : maxAge;
    }
    if (maxAge == null) {
      return true;
    }

    return Date.now() <= time + maxAge;
  }

  /** @internal */
  private findMaxAgeFromHeaders(headers: any): number | null {
    let maxAge: number | null = null;
    const cacheControl = this.findCacheControl(headers);
    if (cacheControl == "no-cache") {
      maxAge = 0;
    } else if (cacheControl != null) {
      const match = cacheControl.match(/max\-age=(\d+)/);
      if (match !== null) {
        maxAge = parseInt(match[1]) * 1000;
      }
    }
    return maxAge;
  }

  /** @internal */
  private findCacheControl(headers: any): string | null {
    const keys = Object.keys(headers);
    if (keys.length === 0) {
      return null;
    }
    for (const key of keys) {
      if (key.toLowerCase() === "cache-control") {
        return headers[key].toLowerCase();
      }
    }
    return null;
  }

  /** @internal */
  private requestHash(request: HttpRequest): string {
    const url = request.url!!;
    const method = request.method!!;
    const headers = JSON.stringify(request.headers);
    const params = JSON.stringify(request.params);
    const key = url + method + headers + params;
    return crypto.createHash("md5").update(key).digest("hex");
  }

  /** @internal */
  private checkCacheDir() {
    if (!path.isAbsolute(this.cacheDir)) {
      this.cacheDir = path.join(process.cwd(), this.cacheDir);
    }
    if (!fs.existsSync(this.cacheDir)) {
      fs.mkdirSync(this.cacheDir, { recursive: true });
    }
  }
}
