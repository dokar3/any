import { AnyService } from "any-service-api";
import { UserFeature } from "./UserFeature";
import { ShotFeature } from "./ShotFeature";

export const BASE_URL = "https://dribbble.com";

class Service extends AnyService {
  onCreate(): void {
    this.addFeature(ShotFeature);
    this.addFeature(UserFeature);
  }
}

export default Service;
