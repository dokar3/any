import { ServiceFeatures } from "any-service-api";
import * as postFeature from "./postFeature";

// Export all features as a 'features' field.
export const features: ServiceFeatures = {
  post: {
    fetch: postFeature.fetch,
    fetchFreshList: postFeature.fetchFreshList,
  },
  // user: {},
  // config: {},
};