import { AnyService } from "any-service-api";
import { PostFeature } from "./PostFeature";

class Service extends AnyService {
  onCreate(): void {
    this.addFeature(PostFeature);
  }
}

export default Service;
