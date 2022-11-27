import {
  AnyPostFeature,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
} from "any-service-api";

export class PostFeature extends AnyPostFeature {
  fetch(params: FetchPostParams): FetchResult<Post> {
    throw new NotImplementedError("Implement this function");
  }

  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
    throw new NotImplementedError("Implement this function");
  }

  // Implement other functions if needed
}
