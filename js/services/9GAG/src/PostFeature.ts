import {
  Comment,
  FetchCommentsParams,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
  AnyPostFeature,
  SearchPostsParams,
} from "any-service-api";
import { fetchPostComments } from "./FetchComments";
import { fetchPosts } from "./FetchPost";

export class PostFeature extends AnyPostFeature {
  fetch(params: FetchPostParams): FetchResult<Post> {
    throw new NotImplementedError("Implement this function");
  }

  fetchComments(params: FetchCommentsParams): PagedResult<Comment[]> {
    const postId = params.loadKey;
    return fetchPostComments(postId, params.pageKey as string);
  }

  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
    const section = this.service.configs.section;
    const tag = this.service.configs.tag;
    if (typeof tag === "string" && tag.length > 0) {
      return this.fetchTagHotPosts(tag, params.pageKey as string);
    } else if (typeof section === "string" && section.length > 0) {
      return this.fetchSectionPosts(section, params.pageKey as string);
    } else {
      throw new Error("Both 'section' and 'tag' are missing or empty");
    }
  }

  search(params: SearchPostsParams): PagedResult<Post[]> {
    return this.searchPosts(params.query, params.pageKey as string);
  }

  fetchSectionPosts(name: string, pageKey: string | null): PagedResult<Post[]> {
    const type = name.toLocaleLowerCase();
    let url: string;
    if (type === "home") {
      url = "https://9gag.com/v1/feed-posts/type/home";
    } else {
      url = "https://9gag.com/v1/group-posts/type/" + type;
    }
    return fetchPosts(this.pagedUrl(url, pageKey));
  }

  fetchTagHotPosts(name: string, pageKey: string | null): PagedResult<Post[]> {
    const url = `https://9gag.com/v1/tag-posts/tag/${name.toLowerCase()}/type/hot`;
    return fetchPosts(this.pagedUrl(url, pageKey));
  }

  searchPosts(query: string, pageKey: string | null): PagedResult<Post[]> {
    const url =
      "https://9gag.com/v1/search-posts?query=" + encodeURIComponent(query);
    return fetchPosts(this.pagedUrl(url, pageKey));
  }

  private pagedUrl(url: string, pageKey: string | null): string {
    if (pageKey != null) {
      return url + "?" + pageKey;
    } else {
      return url;
    }
  }
}
