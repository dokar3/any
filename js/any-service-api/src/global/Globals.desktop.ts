import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { LoadingProgressUpdater } from "../service/LoadingProgressUpdater";
import { Console } from "./Console";
import { DOM } from "./Dom";
import { DomElement, StringOrVoid } from "./DomElement";
import { Env } from "./Env";
import { Http, HttpResponse } from "./Http";

declare const __ANY_ENV__: any;
declare const __ANY_LOG__: any;
declare const __ANY_HTTP__: any;
declare const __AnyDoc__: any;

export function setupDesktopGlobals() {
  const console = new Console(
    (message) => __ANY_LOG__.log(message),
    (message) => __ANY_LOG__.info(message),
    (message) => __ANY_LOG__.warn(message),
    (message) => __ANY_LOG__.error(message)
  );

  const env = new Env(__ANY_ENV__);

  const http = new Http({
    handle: (request) => {
      const json = JSON.stringify(request);
      const jsonRes = __ANY_HTTP__.request(json);
      const res = JSON.parse(jsonRes);
      return new HttpResponse(res.text, res.status, res.headers);
    },
  });

  const dom: DOM = {
    createDocument(text, type?) {
      const doc = new __AnyDoc__(text, type === "xml" ? type : "html");
      return new DesktopDomElement(doc, doc.rootElementId());
    },
  };

  const progressUpdater = new LoadingProgressUpdater((progress, message) => {
    console.log("Update loading progress: " + progress + " - msg " + message);
  });

  const manifestUpdater = new ManifestUpdater();

  const configsUpdater = new ConfigsUpdater((configs) => {
    // DO NOTHING
  });

  globalThis.console = console as any;
  globalThis.env = env;
  globalThis.http = http;
  globalThis.DOM = dom;
  globalThis.progressUpdater = progressUpdater;
  globalThis.manifestUpdater = manifestUpdater;
  globalThis.configsUpdater = configsUpdater;
}

class DesktopDomElement implements DomElement {
  private doc: any;

  private elementId: number;

  constructor(doc: any, elementId: number) {
    this.doc = doc;
    this.elementId = elementId;
  }

  select(cssQuery: string): DomElement | null {
    const id = this.doc.selectFirst(this.elementId, cssQuery);
    if (id !== -1) {
      return new DesktopDomElement(this.doc, id);
    } else {
      return null;
    }
  }

  selectAll(cssQuery: string): DomElement[] {
    const ids = this.doc.select(this.elementId, cssQuery);
    const len = ids.length;
    const elements = new Array(len);
    for (var i = 0; i < len; i++) {
      elements[i] = new DesktopDomElement(this.doc, ids[i]);
    }
    return elements;
  }

  attr<V>(name: string, value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.doc.setAttr(this.elementId, name, value);
    } else {
      return this.doc.attr(this.elementId, name);
    }
  }

  text<V>(value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.doc.setText(this.elementId, value);
    } else {
      return this.doc.text(this.elementId);
    }
  }

  html<V>(html?: V): StringOrVoid<V> {
    if (typeof html === "string") {
      this.doc.setHtml(this.elementId, html);
    } else {
      return this.doc.html(this.elementId);
    }
  }
}
