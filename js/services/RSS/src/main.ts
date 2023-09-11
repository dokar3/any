import { ServiceFeatures } from "any-service-api";
import * as postFeature from "./postFeature";
import { validateConfigs } from "./configFeature";

export const features: ServiceFeatures = {
  post: {
    fetch: postFeature.fetch,
    fetchFreshList: postFeature.fetchFreshList,
  },
  config: {
    validate: validateConfigs,
  },
};
