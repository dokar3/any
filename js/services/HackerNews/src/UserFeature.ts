import {
  FetchResult,
  FetchUserByIdParams,
  FetchUserByUrlParams,
  FetchUserPostsParams,
  PagedResult,
  Post,
  User,
} from "any-service-api";
import { parseNews } from "./postFeature";
import { BASE_URL } from "./shared";

export function fetchByUrl(params: FetchUserByUrlParams): FetchResult<User> {
  const result = /user?id=(.+)/g.exec(params.userUrl);
  if (result === null) {
    return FetchResult.err({
      error: `Unsupported user url: ${params.userUrl}`,
    });
  }
  return fetchById(new FetchUserByIdParams({ userId: result[1] }));
}

export function fetchById(params: FetchUserByIdParams): FetchResult<User> {
  const userId = params.userId;
  const url = "https://hacker-news.firebaseio.com/v0/user/" + userId + ".json";
  const json = http.get(url).text;
  if (json === null) {
    return FetchResult.err({ error: "Cannot fetch the user: " + userId });
  }

  let ret: any;
  try {
    ret = JSON.parse(json);
  } catch (e) {
    return PagedResult.err({ error: "Cannot fetch the user: " + userId });
  }
  const user = new User({
    id: ret.id,
    name: ret.id,
    description: ret.about,
  });

  return FetchResult.ok({ data: user });
}

export function fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
  let url: string;
  if (params.pageKey === null) {
    url = "https://news.ycombinator.com/submitted?id=" + params.userId;
  } else {
    url = BASE_URL + params.pageKey;
  }

  console.log("fetch user posts: " + url);

  const html = http.get(url).text;
  if (html === null) {
    return PagedResult.err({ error: "Cannot fetch the news page" });
  }
  const doc = DOM.createDocument(html);
  if (doc === null) {
    return PagedResult.err({ error: "Cannot parse the news page" });
  }

  const result = parseNews(doc, -1);
  if (result.isOk()) {
    result.nextKey = doc.select("tr td a.morelink")?.attr("href") ?? null;
  }

  return result;
}
