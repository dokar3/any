import { AnyService } from "any-service-api";
import { ConfigFeature } from "./ConfigFeature";
import { FeedFeature } from "./FeedFeature";

class Service extends AnyService {
  onCreate(): void {
    this.addFeature(ConfigFeature);
    this.addFeature(FeedFeature);
  }
}

export default Service;
