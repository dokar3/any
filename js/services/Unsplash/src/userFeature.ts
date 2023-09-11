import {
  FetchResult,
  FetchUserByIdParams,
  FetchUserByUrlParams,
  FetchUserPostsParams,
  PagedResult,
  Post,
  User,
} from "any-service-api";
import { authorizedGet } from "./commonFeature";
import * as postFeature from "./postFeature";
import { BASE_URL } from "./shared";

export function fetchByUrl(params: FetchUserByUrlParams): FetchResult<User> {
  const userUrl = params.userUrl;
  const results = /@(.+)/g.exec(userUrl);
  if (results === null) {
    return FetchResult.err({ error: `Unsupported user url: ${userUrl}` });
  }
  const username = results[1];
  const byIdParams = new FetchUserByIdParams({ userId: username });
  return fetchById(byIdParams);
}

export function fetchById(params: FetchUserByIdParams): FetchResult<User> {
  const userId = params.userId;
  const url = `${BASE_URL}users/${userId}`;
  const json = authorizedGet(url).text;
  if (json === null) {
    return FetchResult.err({ error: `Cannot fetch the user: ${userId}` });
  }

  const obj = JSON.parse(json);
  const user = new User({
    id: userId,
    name: obj.name,
    alternativeName: obj.username,
    avatar: obj.profile_image.large,
    description: obj.bio,
    url: obj.links.html,
    postCount: obj.total_photos,
    followerCount: obj.followers_count,
    followingCount: obj.following_count,
  });

  return FetchResult.ok({ data: user });
}

export function fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
  const page = (params.pageKey as number) ?? 1;
  const countPerPage = service.configs.countPerPage;
  const url =
    `${BASE_URL}users/${params.userId}/photos` +
    `?page=${page}&per_page=${countPerPage}`;
  return postFeature.fetchPhotos(url, page);
}
