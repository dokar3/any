import { HttpResponse } from "any-service-api";

/**
 * Http GET with authorization header.
 *
 * @param {string} url The request url.
 * @returns {HttpResponse} The response.
 */
export function authorizedGet(url: string): HttpResponse {
  const configs = service.configs;
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
