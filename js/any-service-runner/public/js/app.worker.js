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
  console.log("this:", globalThis)
  initService(manifest, serviceConfigs);
  if (type === EventType.TYPE_FETCH_LATEST_POSTS) {
    return service.features.post.fetchFreshList(params);
  } else if (type === EventType.TYPE_FETCH_POST_CONTENT) {
    return service.features.post.fetch(params);
  } else if (type == EventType.TYPE_SEARCH_POSTS) {
    return service.features.post.search(params);
  } else {
    throw new Error("Unknown call event: " + type);
  }
}

Comlink.expose(call);
