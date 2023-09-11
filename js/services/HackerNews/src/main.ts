import { ServiceFeatures } from "any-service-api";
import * as postFeature from "./postFeature";
import * as userFeature from "./userFeature";

export const features: ServiceFeatures = {
  post: {
    fetch: postFeature.fetch,
    fetchFreshList: postFeature.fetchFreshList,
    search: postFeature.search,
    fetchComments: postFeature.fetchComments,
  },
  user: {
    fetchById: userFeature.fetchById,
    fetchByUrl: userFeature.fetchByUrl,
    fetchPosts: userFeature.fetchPosts,
  },
};
