import { Feature, HttpResponse } from "any-service-api";

export class CommonFeature extends Feature {
  /**
   * Http GET with authorization header.
   *
   * @param {string} url The request url.
   * @returns {HttpResponse} The response.
   */
  authorizedGet(url: string): HttpResponse {
    const configs = this.service.configs;
    if (!configs) {
      throw new Error("Invalid configs");
    }
    const accessKey = configs.accessKey;
    if (!accessKey) {
      throw new Error("'accessKey' is not configured");
    }
    const options = {
      headers: {
        Authorization: `Client-ID ${accessKey}`,
      },
    };
    return http.get(url, options);
  }
}
