import { AnyService } from "any-service-api";
import { DribbbleUserFeature } from "./DribbbleUserFeature";
import { DribbbleShotFeature } from "./DribbbleShotFeature";

export const BASE_URL = "https://dribbble.com";

class DribbbleService extends AnyService {
  onCreate(): void {
    this.addFeature(DribbbleShotFeature);
    this.addFeature(DribbbleUserFeature);
  }
}

export default DribbbleService;
