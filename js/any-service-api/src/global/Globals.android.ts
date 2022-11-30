import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { LoadingProgressUpdater } from "../service/LoadingProgressUpdater";
import { AndroidManifestUpdater } from "./AndroidManifestUpdater";
import { Console } from "./Console";
import { DOM } from "./Dom";
import { DomElement, StringOrVoid } from "./DomElement";
import { Env } from "./Env";
import { Http, HttpResponse } from "./Http";

declare const __ANY_ENV__: any;
declare const __ANY_LOG_PLUGIN__: any;
declare const __ANY_HTTP_PLUGIN__: any;
declare const __ANY_DOM_PLUGIN__: any;
declare const __ANY_PROGRESS_PLUGIN__: any;
declare const __ANY_CONFIGS_UPDATER__: any;

const console = new Console(
  (message) => __ANY_LOG_PLUGIN__.log(message),
  (message) => __ANY_LOG_PLUGIN__.info(message),
  (message) => __ANY_LOG_PLUGIN__.warn(message),
  (message) => __ANY_LOG_PLUGIN__.error(message)
);

const env = new Env(__ANY_ENV__);

const http = new Http({
  handle: (request) => {
    const json = JSON.stringify(request);
    const jsonRes = __ANY_HTTP_PLUGIN__.request(json);
    const res = JSON.parse(jsonRes);
    return new HttpResponse(res.text, res.status, res.headers);
  },
});

const dom: DOM = {
  createDocument(text, type?) {
    const t = type === "xml" ? type : "html";
    const id = __ANY_DOM_PLUGIN__.create(text, t);
    return new AndroidDomElement(id);
  },
};

const progressUpdater = new LoadingProgressUpdater((progress, message) => {
  __ANY_PROGRESS_PLUGIN__.update(
    progress,
    message !== undefined ? message : null
  );
});

const manifestUpdater = new AndroidManifestUpdater();

const configsUpdater = new ConfigsUpdater((configs) => {
  __ANY_CONFIGS_UPDATER__.update(JSON.stringify(configs));
});

export function setupAndroidGlobals() {
  globalThis.console = console as any;
  globalThis.env = env;
  globalThis.http = http;
  globalThis.DOM = dom;
  globalThis.progressUpdater = progressUpdater;
  globalThis.manifestUpdater = manifestUpdater;
  globalThis.configsUpdater = configsUpdater;
}

class AndroidDomElement implements DomElement {
  private elementId: number;

  constructor(elementId: number) {
    this.elementId = elementId;
  }

  selectAll(cssQuery: string): DomElement[] {
    const ids = __ANY_DOM_PLUGIN__.select(this.elementId, cssQuery);
    const len = ids.length;
    const elements = new Array(len);
    for (var i = 0; i < len; i++) {
      elements[i] = new AndroidDomElement(ids[i]);
    }
    return elements;
  }

  select(cssQuery: string): DomElement | null {
    const id = __ANY_DOM_PLUGIN__.selectFirst(this.elementId, cssQuery);
    if (id !== -1) {
      return new AndroidDomElement(id);
    } else {
      return null;
    }
  }

  attr<V>(name: string, value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      __ANY_DOM_PLUGIN__.setAttr(this.elementId, name, value);
    } else {
      return __ANY_DOM_PLUGIN__.attr(this.elementId, name);
    }
  }

  text<V>(value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      __ANY_DOM_PLUGIN__.setText(this.elementId, value);
    } else {
      return __ANY_DOM_PLUGIN__.text(this.elementId);
    }
  }

  html<V>(html?: V): StringOrVoid<V> {
    if (typeof html === "string") {
      __ANY_DOM_PLUGIN__.setHtml(this.elementId, html);
    } else {
      return __ANY_DOM_PLUGIN__.html(this.elementId);
    }
  }
}
