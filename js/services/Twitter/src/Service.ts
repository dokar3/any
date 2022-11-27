import { AnyService } from "any-service-api";
import { ConfigFeature } from "./ConfigFeature";
import { CommonFeature } from "./CommonFeature";
import { TweetFeature } from "./TweetFeature";
import { UserFeature } from "./UserFeature";

class Service extends AnyService {
  onCreate(): void {
    this.addFeature(CommonFeature);
    this.addFeature(ConfigFeature);
    this.addFeature(TweetFeature);
    this.addFeature(UserFeature);
  }
}

export default Service;
