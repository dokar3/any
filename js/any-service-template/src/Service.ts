import { AnyService } from "any-service-api";
import { PostFeature } from "./PostFeature";

export default class Service extends AnyService {
  onCreate(): void {
    // There are 4 types of feature for now:
    //
    // Feature          - The base feature, all features must extend this class.
    //                    If your utility functions need to access the service instance,
    //                    you can put them in a feature and add it to the service, then
    //                    call them in other features. For example: 
    //                    this.service.getFeature(MyFeature).myFn()
    // AnyPostFeature   - Used to fetch post related data.
    // AnyUserFeature   - Used to fetch user related data.
    // AnyConfigFeature - Used to validate service configs.
    //
    // Extend and add them if needed.
    this.addFeature(PostFeature);
  }
}
