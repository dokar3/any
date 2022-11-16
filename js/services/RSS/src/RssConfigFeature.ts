import { ConfigFeature, ValidationError } from "any-service-api";

export class RssConfigFeature extends ConfigFeature {
  validateConfigs(): ValidationError | ValidationError[] {
    if (!this.service.configs.url) {
      return new ValidationError({
        key: "url",
        message: "Config 'url' is a invalid value: " + this.service.configs.url,
      });
    }
    const code = http.get(this.service.configs.url).status;
    if (code !== 200) {
      return new ValidationError({
        key: "url",
        message: "Unable to fetch content from this url, code: " + code,
      });
    }
    return null;
  }
}
