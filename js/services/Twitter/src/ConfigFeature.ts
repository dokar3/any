import { AnyConfigFeature, ValidationError } from "any-service-api";
import { CommonFeature } from "./CommonFeature";

export class ConfigFeature extends AnyConfigFeature {
  validateConfigs(): ValidationError | ValidationError[] {
    const commonFeature = this.service.getFeature(CommonFeature);
    if (!commonFeature.checkLogin()) {
      return new ValidationError({
        key: "bearerToken",
        message: "Not logged in, please try again",
      });
    }

    if (!commonFeature.checkBearerToken()) {
      commonFeature.requestBearerToken();
    }
    if (!commonFeature.checkBearerToken()) {
      return new ValidationError({
        key: "bearerToken",
        message: "Not logged in, please try again",
      });
    }

    if (!commonFeature.checkMyUserId()) {
      commonFeature.requestMyUserId();
    }
    if (!commonFeature.checkMyUserId()) {
      return new ValidationError({
        key: "bearerToken",
        message: "Cannot fetch the user info",
      });
    }

    return null;
  }
}
