import {
  Comment,
  FetchCommentsParams,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
  SearchPostsParams,
} from "any-service-api";
import { fetchPostComments } from "./FetchComments";
import { fetchPosts } from "./FetchPost";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  throw new NotImplementedError("Implement this function");
}

export function fetchComments(
  params: FetchCommentsParams
): PagedResult<Comment[]> {
  const postId = params.loadKey;
  return fetchPostComments(postId, params.pageKey as string);
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const section = service.configs.section;
  const tag = service.configs.tag;
  if (typeof tag === "string" && tag.length > 0) {
    return fetchTagHotPosts(tag, params.pageKey as string);
  } else if (typeof section === "string" && section.length > 0) {
    return fetchSectionPosts(section, params.pageKey as string);
  } else {
    throw new Error("Both 'section' and 'tag' are missing or empty");
  }
}

export function search(params: SearchPostsParams): PagedResult<Post[]> {
  return searchPosts(params.query, params.pageKey as string);
}

export function fetchSectionPosts(
  name: string,
  pageKey: string | null
): PagedResult<Post[]> {
  const type = name.toLocaleLowerCase();
  let url: string;
  if (type === "home") {
    url = "https://9gag.com/v1/feed-posts/type/home";
  } else {
    url = "https://9gag.com/v1/group-posts/type/" + type;
  }
  return fetchPosts(pagedUrl(url, pageKey));
}

export function fetchTagHotPosts(
  name: string,
  pageKey: string | null
): PagedResult<Post[]> {
  const url = `https://9gag.com/v1/tag-posts/tag/${name.toLowerCase()}/type/hot`;
  return fetchPosts(pagedUrl(url, pageKey));
}

export function searchPosts(
  query: string,
  pageKey: string | null
): PagedResult<Post[]> {
  const url =
    "https://9gag.com/v1/search-posts?query=" + encodeURIComponent(query);
  return fetchPosts(pagedUrl(url, pageKey));
}

function pagedUrl(url: string, pageKey: string | null): string {
  if (pageKey != null) {
    return url + "?" + pageKey;
  } else {
    return url;
  }
}
