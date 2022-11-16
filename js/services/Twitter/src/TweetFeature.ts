import {
  FetchFreshListParams,
  PagedResult,
  Post,
  PostFeature,
  SearchPostsParams,
} from "any-service-api";
import { CommonFeature } from "./CommonFeature";
import { parseTweet } from "./ParseTweet";

const TWEETS_PER_PAGE = 20;

export class TweetFeature extends PostFeature {
  fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
    return this.fetchTweets(() => {
      return (
        "https://api.twitter.com/2/users/" +
        this.service.configs.userId +
        "/timelines/reverse_chronological"
      );
    }, params.pageKey);
  }

  search(params: SearchPostsParams): PagedResult<Post[]> {
    return this.fetchTweets(() => {
      const baseUrl = "https://api.twitter.com/2/tweets/search/recent";
      return (
        baseUrl +
        "?query=" +
        encodeURIComponent(params.query) +
        "&sort_order=relevancy"
      );
    }, params.pageKey);
  }

  fetchTweets(
    url: () => string,
    pageKey: string | number | null
  ): PagedResult<Post[]> {
    const commonFeature = this.service.getFeature(CommonFeature);
    if (!commonFeature.checkLogin()) {
      return PagedResult.err({ error: "Not logged in" });
    }

    if (!commonFeature.checkBearerToken()) {
      commonFeature.requestBearerToken();
    }

    if (!commonFeature.checkMyUserId()) {
      commonFeature.requestMyUserId();
    }

    let requestUrl = this.addCommonTweetQueryParams(url());
    if (pageKey) {
      requestUrl += "&pagination_token=" + pageKey;
    }
    const res = commonFeature.authorizedGet(
      requestUrl,
      () => this.service.configs.bearerToken
    );

    return this.parseTweets(res.text);
  }

  private addCommonTweetQueryParams(baseUrl: string): string {
    const hasParams = baseUrl.indexOf("?") !== -1;
    return (
      baseUrl +
      (hasParams ? "&" : "?") +
      "tweet.fields=attachments,entities" +
      "&media.fields=url,width,height,preview_image_url,type" +
      "&expansions=referenced_tweets.id,referenced_tweets.id.author_id," +
      "author_id,attachments.media_keys" +
      "&user.fields=name,profile_image_url" +
      "&max_results=" +
      TWEETS_PER_PAGE
    );
  }

  /**
   * Parse tweets.
   *
   * @param {string} jsonText The response json text.
   * @returns {PagedResult<Post[]>} Posts result.
   */
  private parseTweets(jsonText: string): PagedResult<Post[]> {
    const obj = JSON.parse(jsonText);
    if (obj.meta !== undefined && obj.meta.result_count === 0) {
      return PagedResult.err({ error: "No results" });
    }

    if (obj.errors !== undefined && !Array.isArray(obj.data)) {
      return PagedResult.err({
        error: obj.errors.title + ": " + obj.errors.detail,
      });
    }

    const data = obj.data;
    if (data === undefined) {
      return PagedResult.err({ error: "Unsupported response: " + jsonText });
    }

    const posts = new Array<Post>();

    for (const item of data) {
      const tweet = parseTweet({
        serviceId: this.service.manifest.id,
        id: item.id,
        text: item.text,
        authorId: item.author_id,
        referencedTweets: item.referenced_tweets,
        mediaKeys: item.attachments?.media_keys ?? [],
        entities: item.entities,
        includes: obj.includes,
      });
      posts.push(
        new Post({
          title: "",
          url: tweet.url,
          summary: tweet.text,
          media: tweet.media,
          author: tweet.author,
          authorId: tweet.authorId,
          avatar: tweet.avatar,
          openInBrowser: true,
          reference: tweet.reference,
        })
      );
    }

    return PagedResult.ok({ data: posts, nextKey: obj.meta.next_token });
  }
}
