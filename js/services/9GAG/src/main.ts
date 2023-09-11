import { ServiceFeatures } from "any-service-api";
import { validateConfigs } from "./configFeature";
import * as postFeature from "./postFeature";

export const features: ServiceFeatures = {
  post: {
    fetch: postFeature.fetch,
    fetchFreshList: postFeature.fetchFreshList,
    fetchComments: postFeature.fetchComments,
  },
  config: {
    validate: validateConfigs,
  },
};
