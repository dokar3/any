import { AnyService } from "any-service-api";
import { HnUserFeature } from "./HnUserFeature";
import { HnNewsFeature } from "./HnNewsFeature";

export const BASE_URL = "https://news.ycombinator.com/";

class HNService extends AnyService {
  onCreate(): void {
    this.addFeature(HnNewsFeature);
    this.addFeature(HnUserFeature);
  }
}

export default HNService;
