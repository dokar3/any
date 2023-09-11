import { NotImplementedError, ServiceFeatures } from "any-service-api";
import * as postFeature from "./postFeature";

export const features: ServiceFeatures = {
  post: {
    fetch: () => {
      throw new NotImplementedError("Fetch is not implemented");
    },
    fetchFreshList: postFeature.fetchFreshList,
    search: postFeature.search,
  },
};
