import { AnyService } from "any-service-api";
import { NineGAGConfigFeature } from "./9GAGConfigsFeature";
import { NineGAGPostFeature } from "./9GAGPostFeature";

export default class NineGAGService extends AnyService {
  onCreate(): void {
    this.addFeature(NineGAGPostFeature);
    this.addFeature(NineGAGConfigFeature);
  }
}
