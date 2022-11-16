import {
  FetchResult,
  FetchUserByIdParams,
  FetchUserPostsParams,
  PagedResult,
  Post,
  User,
  UserFeature,
} from "any-service-api";
import { CommonFeature } from "./CommonFeature";
import { TweetFeature } from "./TweetFeature";

export class TwitterUserFeature extends UserFeature {
  fetchById(params: FetchUserByIdParams): FetchResult<User> {
    const commonFeature = this.service.getFeature(CommonFeature);
    if (!commonFeature.checkLogin()) {
      return FetchResult.err({ error: "Not logged in" });
    }

    if (!commonFeature.checkBearerToken()) {
      commonFeature.requestBearerToken();
    }

    if (!commonFeature.checkMyUserId()) {
      commonFeature.requestMyUserId();
    }

    const url =
      "https://api.twitter.com/2/users/" +
      params.userId +
      "?user.fields=url,description,public_metrics,profile_image_url";
    const res = commonFeature.authorizedGet(
      url,
      () => this.service.configs.bearerToken
    );
    if (res.text == null) {
      return FetchResult.err({ error: "Cannot fetch user: no http response" });
    }

    const data = JSON.parse(res.text).data;
    if (data === undefined) {
      return FetchResult.err({
        error: "Cannot fetch user: Unsupported response: " + res.text,
      });
    }

    const user = new User({
      id: params.userId,
      name: data.name,
      url: data.url,
      alternativeName: data.username,
      // Use the 'original' image url
      avatar: data.profile_image_url?.replace("_normal.", "."),
      description: data.description,
      followerCount: data.public_metrics.followers_count,
      followingCount: data.public_metrics.following_count,
      postCount: data.public_metrics.tweet_count,
    });

    return FetchResult.ok({ data: user });
  }

  fetchPosts(params: FetchUserPostsParams): PagedResult<Post[]> {
    const baseUrl =
      "https://api.twitter.com/2/users/" + params.userId + "/tweets";
    const tweetFeature = this.service.getFeature(TweetFeature);
    return tweetFeature.fetchTweets(() => baseUrl, params.pageKey);
  }
}
