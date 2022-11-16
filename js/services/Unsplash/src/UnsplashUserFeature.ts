import {
  FetchResult,
  FetchUserByIdParams,
  FetchUserByUrlParams,
  FetchUserPostsParams,
  PagedResult,
  Post,
  User,
  UserFeature,
} from "any-service-api";
import { BASE_URL } from "./UnsplashService";
import { CommonFeature } from "./CommonFeature";
import { UnsplashPhotoFeature } from "./UnsplashPhotoFeature";

export class UnsplashUserFeature extends UserFeature {
  fetchByUrl(params: FetchUserByUrlParams): FetchResult<User> {
    const userUrl = params.userUrl;
    const results = /@(.+)/g.exec(userUrl);
    if (results === null) {
      return FetchResult.err({ error: `Unsupported user url: ${userUrl}` });
    }
    const username = results[1];
    const byIdParams = new FetchUserByIdParams({ userId: username });
    return this.fetchById(byIdParams);
  }

  fetchById(params: FetchUserByIdParams): FetchResult<User> {
    const userId = params.userId;
    const url = `${BASE_URL}users/${userId}`;
    const commonFeature = this.service.getFeature(CommonFeature);
    const json = commonFeature.authorizedGet(url).text;
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

  fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
    const page = (params.pageKey as number) ?? 1;
    const countPerPage = this.service.configs.countPerPage;
    const url =
      `${BASE_URL}users/${params.userId}/photos` +
      `?page=${page}&per_page=${countPerPage}`;
    const postFeature = this.service.getFeature(UnsplashPhotoFeature);
    return postFeature.fetchPhotos(url, page);
  }
}
