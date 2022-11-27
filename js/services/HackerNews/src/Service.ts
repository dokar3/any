import { AnyService } from "any-service-api";
import { UserFeature } from "./UserFeature";
import { NewsFeature } from "./NewsFeature";

export const BASE_URL = "https://news.ycombinator.com/";

class HNService extends AnyService {
  onCreate(): void {
    this.addFeature(NewsFeature);
    this.addFeature(UserFeature);
  }
}

export default HNService;
