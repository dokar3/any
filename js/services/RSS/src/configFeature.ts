import { ValidationError } from "any-service-api";

export function validateConfigs(): ValidationError | ValidationError[] {
  if (!service.configs.url) {
    return new ValidationError({
      key: "url",
      message: "Config 'url' is a invalid value: " + service.configs.url,
    });
  }
  const code = http.get(service.configs.url).status;
  if (code !== 200) {
    return new ValidationError({
      key: "url",
      message: "Unable to fetch content from this url, code: " + code,
    });
  }
  return null;
}
