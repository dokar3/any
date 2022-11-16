import { AnyService } from "any-service-api";
import { FilmGrabPostFeature } from "./FilmGrabPostFeature";

class FilmGrabService extends AnyService {
  onCreate(): void {
    this.addFeature(FilmGrabPostFeature);
  }
}

export default FilmGrabService;
