import { ServiceRegistry } from "any-service-api";
import { find } from "./Array.find.js";
import Service from "./Service";

if (!Array.prototype.find) {
  Object.defineProperty(Array.prototype, "find", {
    value: find,
  });
}

ServiceRegistry.register(Service);
