import {
  AspectRatio,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  NotImplementedError,
  PagedResult,
  Post,
  SearchPostsParams,
} from "any-service-api";
import { authorizedGet } from "./commonFeature";
import { BASE_URL } from "./shared";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  throw new NotImplementedError("open in browser instead");
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 1;
  const countPerPage = service.configs.countPerPage;
  const url = `${BASE_URL}photos?page=${page}&per_page=${countPerPage}`;
  return fetchPhotos(url, page);
}

export function search(params: SearchPostsParams): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 1;
  const countPerPage = service.configs.countPerPage;
  const url =
    `${BASE_URL}search/photos?query=${params.query}` +
    `&page=${page}&per_page=${countPerPage}`;
  const res = authorizedGet(url);
  if (res.status != 200) {
    throw new Error(`Http error, code: ${res.status}, msg: ${res.text}`);
  }

  const result = JSON.parse(res.text);
  if (!result) {
    throw new Error(`Cannot fetch photos, result: ${res.text}`);
  }

  const items = result.results;
  if (!items || !Array.isArray(items)) {
    throw new Error(`Cannot fetch photos from unsplash, result: ${res.text}`);
  }

  return parseResponse(items, page);
}

export function fetchPhotos(url: string, page: number): PagedResult<Post[]> {
  const res = authorizedGet(url);
  if (res.status != 200) {
    return PagedResult.err({
      error: `Http error, code: ${res.status}, msg: ${res.text}`,
    });
  }

  const result = JSON.parse(res.text);
  if (!Array.isArray(result)) {
    return PagedResult.err({
      error: `Cannot fetch photos from unsplash, result: ${res.text}`,
    });
  }
  return parseResponse(result, page);
}

/**
 * Parse UnSplash response items.
 *
 * @param {object[]} items UnSplash items.
 * @param {number} page The current page.
 * @returns {PagedResult<Post[]>} The paged result.
 */
function parseResponse(items: any[], page: number): PagedResult<Post[]> {
  const posts = new Array<Post>();
  items.forEach((item) => {
    const description = item.description ?? item.alt_description ?? null;
    const width: number = item.width;
    const height: number = item.height;
    const aspectRatio: AspectRatio = `${width}:${height}`;

    const content: Post.ContentElement[] = [
      Post.ContentElement.image({
        url: item.urls.full,
        aspectRatio: aspectRatio,
      }),
    ];
    if (description != null) {
      content.push(Post.ContentElement.text({ text: description }));
    }
    content.push(Post.ContentElement.text({ text: `${item.likes} likes` }));

    const post = new Post({
      title: "",
      url: item.links.html,
      author: item.user.name,
      authorId: item.user.username,
      avatar: item.user.profile_image.large,
      date: item.updated_at,
      summary: description,
      media: [
        Post.Media.photo({
          url: item.urls.regular,
          aspectRatio: aspectRatio,
        }),
      ],
      content: content,
    });
    posts.push(post);
  });
  return PagedResult.ok({ data: posts, nextKey: page + 1 });
}
