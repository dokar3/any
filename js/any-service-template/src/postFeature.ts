import {
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
} from "any-service-api";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  throw new NotImplementedError("Not implemented yet.");
}

export function fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
  throw new NotImplementedError("Not implemented yet.");
}

// Implement other functions if needed
