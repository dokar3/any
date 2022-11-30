import { AnyService } from "any-service-api";
import { ConfigFeature } from "./ConfigFeature";
import { PostFeature } from "./PostFeature";

export default class Service extends AnyService {
  onCreate(): void {
    this.addFeature(PostFeature);
    this.addFeature(ConfigFeature);
  }
}
