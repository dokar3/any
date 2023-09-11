import {
  Comment,
  FetchCommentsParams,
  FetchFreshListParams,
  FetchPostParams,
  FetchResult,
  PagedResult,
  Post,
  SearchPostsParams,
} from "any-service-api";
import { fetchShots, parseShotPage } from "./ParseShot";
import { BASE_URL } from "./shared";

export function fetch(params: FetchPostParams): FetchResult<Post> {
  const html = http.get(params.url).text;
  if (html === null) {
    return FetchResult.err({ error: "Cannot load Dribbble page" });
  }

  const doc = DOM.createDocument(html);
  if (doc === null) {
    return FetchResult.err({ error: "Cannot parse Dribbble page" });
  }

  const content = doc.select("#content .shot-content-container");
  if (content === null) {
    return FetchResult.err({ error: "No content found" });
  }

  const header = content.select(".shot-header .shot-header-content");
  const title = header.select(".shot-header-title")?.text();
  const artist = header.select(".shot-user-details")?.text();
  const artistId = header
    .select(".shot-user-details .shot-user-link")
    .attr("href");
  const avatar = header.select(".shot-user-avatar a .photo")?.attr("src");

  const shotId = /\/shots\/(\d+)/.exec(params.url)[1];
  const fetchCommentsUrl =
    "https://dribbble.com/shots/" +
    shotId +
    "/comments?page=1&sort=recent&format=json";
  const commentsJson = http.get(fetchCommentsUrl).text;
  const commentCount = parseInt(JSON.parse(commentsJson).commentsCount);

  const post = new Post({
    url: params.url,
    title: title,
    author: artist,
    authorId: artistId,
    avatar: avatar,
    commentsKey: shotId,
    commentCount: commentCount,
    content: parseShotPage(content),
  });
  return FetchResult.ok({ data: post });
}

export function fetchFreshList(
  params: FetchFreshListParams
): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 1;
  const url = `${BASE_URL}/shots/popular?page=${page}&per_page=24`;
  return fetchShots(url, page);
}

export function search(params: SearchPostsParams): PagedResult<Post[]> {
  const path = params.query
    .toLowerCase()
    .replace(/ /g, "-")
    .replace(/[^a-zA-Z0-9\-]/g, "");
  const page = (params.pageKey as number) ?? 1;
  const url = `${BASE_URL}/search/${path}?page=${page}&per_page=24`;
  return fetchShots(url, page);
}

export function fetchComments(
  params: FetchCommentsParams
): PagedResult<Comment[]> {
  const page = (params.pageKey as number) ?? 1;
  const url =
    "https://dribbble.com/shots/" +
    params.loadKey +
    "/comments?page=" +
    page +
    "&sort=recent&format=json";
  const json = http.get(url).text;
  if (json === null) {
    return PagedResult.err({ error: "Cannot fetch comments" });
  }

  const result = JSON.parse(json);
  if (!Array.isArray(result.comments)) {
    return PagedResult.ok({ data: [] });
  }

  const comments = [];
  const items = result.comments;
  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    comments.push(
      new Comment({
        username: item.author.name,
        avatar: item.author.avatarUrl,
        content: item.rawComment,
        upvotes: item.likesCount,
      })
    );
  }

  return PagedResult.ok({ data: comments, nextKey: page + 1 });
}
