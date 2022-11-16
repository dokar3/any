importScripts("./jsdom.browserify.js");
importScripts("./main.bundle.js");

import * as Comlink from "comlink";
import * as EventType from "./call_event.js";

/**
 * Call app functions
 *
 * @param {CallEvent} event The call event.
 */
function call(appConfigs, type, params) {
  const app = createApp(appConfigs);
  if (type === EventType.TYPE_FETCH_LATEST_POSTS) {
    return app.fetchLatestPosts(params);
  } else if (type === EventType.TYPE_FETCH_POST_CONTENT) {
    return app.fetchPostContent(params);
  } else if (type == EventType.TYPE_SEARCH_POSTS) {
    return app.searchPosts(params);
  } else {
    throw new Error("Unknown call event: " + type);
  }
}

Comlink.expose(call);
