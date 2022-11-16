import { AnyService } from "any-service-api";
import { TwitterConfigFeature } from "./TwitterConfigFeature";
import { CommonFeature } from "./CommonFeature";
import { TweetFeature } from "./TweetFeature";
import { TwitterUserFeature } from "./TwitterUserFeature";

class TwitterService extends AnyService {
  onCreate(): void {
    this.addFeature(CommonFeature);
    this.addFeature(TwitterConfigFeature);
    this.addFeature(TweetFeature);
    this.addFeature(TwitterUserFeature);
  }
}

export default TwitterService;
