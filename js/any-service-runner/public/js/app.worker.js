importScripts("./jsdom.browserify.js");
importScripts("./main.bundle.js");

import * as Comlink from "comlink";
import * as EventType from "./call_event.js";

/**
 * Call service functions
 *
 * @param {CallEvent} event The call event.
 */
function call(serviceConfigs, type, params) {
  const manifest = {};
  const service = createService(manifest, serviceConfigs);
  if (type === EventType.TYPE_FETCH_LATEST_POSTS) {
    return service.getFeature(AnyPostFeature).fetchFreshList(params);
  } else if (type === EventType.TYPE_FETCH_POST_CONTENT) {
    return service.getFeature(AnyPostFeature).fetch(params);
  } else if (type == EventType.TYPE_SEARCH_POSTS) {
    return service.getFeature(AnyPostFeature).search(params);
  } else {
    throw new Error("Unknown call event: " + type);
  }
}

Comlink.expose(call);
