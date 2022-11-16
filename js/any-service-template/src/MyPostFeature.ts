import {
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
  PostFeature,
} from "any-api";

export class MyPostFeature extends PostFeature {
  fetch(params: FetchPostParams): FetchResult<Post> {
    throw new NotImplementedError("Implmenet this function");
  }

  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
    throw new NotImplementedError("Implmenet this function");
  }

  // Implement other functions if needed
}
