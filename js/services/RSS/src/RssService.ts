import { AnyService } from "any-service-api";
import { RssConfigFeature } from "./RssConfigFeature";
import { FeedFeature } from "./FeedFeature";

class RssService extends AnyService {
  onCreate(): void {
    this.addFeature(RssConfigFeature);
    this.addFeature(FeedFeature);
  }
}

export default RssService;
