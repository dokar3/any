import { AnyService } from "any-service-api";
import { CommonFeature } from "./CommonFeature";
import { UnsplashPhotoFeature } from "./UnsplashPhotoFeature";
import { UnsplashUserFeature } from "./UnsplashUserFeature";

export const BASE_URL = "https://api.unsplash.com/";

class UnsplashService extends AnyService {
  onCreate(): void {
    this.addFeature(CommonFeature);
    this.addFeature(UnsplashPhotoFeature);
    this.addFeature(UnsplashUserFeature);
  }
}

export default UnsplashService;
