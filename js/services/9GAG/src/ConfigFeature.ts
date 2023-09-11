import { ValidationError } from "any-service-api";
import { fetchSectionPosts, fetchTagHotPosts } from "./postFeature";

export function validateConfigs(): ValidationError | ValidationError[] {
  const section = service.configs;
  const tag = service.configs.tag;
  if (typeof tag == "string" && tag.length > 0) {
    const result = fetchTagHotPosts(tag, null);
    if (result.isErr()) {
      return new ValidationError({
        key: "tag",
        message: `Cannot fetch posts from the tag '${tag}'`,
      });
    }
  } else {
    const result = fetchSectionPosts(section, null);
    if (result.isErr()) {
      return new ValidationError({
        key: section,
        message: `Cannot fetch posts from the section '${section}'`,
      });
    }
  }
  return null;
}
