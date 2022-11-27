import { AnyConfigFeature, ValidationError } from "any-service-api";
import { PostFeature } from "./PostFeature";

export class ConfigFeature extends AnyConfigFeature {
  validateConfigs(): ValidationError | ValidationError[] {
    const postFeature = this.service.getFeature(PostFeature);
    const section = this.service.configs;
    const tag = this.service.configs.tag;
    if (typeof tag == "string" && tag.length > 0) {
      const result = postFeature.fetchTagHotPosts(tag, null);
      if (result.isErr()) {
        return new ValidationError({
          key: "tag",
          message: `Cannot fetch posts from the tag '${tag}'`,
        });
      }
    } else {
      const result = postFeature.fetchSectionPosts(section, null);
      if (result.isErr()) {
        return new ValidationError({
          key: section,
          message: `Cannot fetch posts from the section '${section}'`,
        });
      }
    }
    return null;
  }
}
