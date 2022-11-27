import { AnyService } from "any-service-api";
import { CommonFeature } from "./CommonFeature";
import { PhotoFeature } from "./PhotoFeature";
import { UserFeature } from "./UserFeature";

export const BASE_URL = "https://api.unsplash.com/";

class Service extends AnyService {
  onCreate(): void {
    this.addFeature(CommonFeature);
    this.addFeature(PhotoFeature);
    this.addFeature(UserFeature);
  }
}

export default Service;
