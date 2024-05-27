import { ConfigsUpdater } from "../config/ConfigsUpdater";
import { ManifestUpdater } from "../manifest/ManifestUpdater";
import { LoadingProgressUpdater } from "../service/LoadingProgressUpdater";
import { DOM } from "./Dom";
import { DomElement, StringOrVoid } from "./DomElement";
import { Env } from "./Env";
import { Http, HttpResponse } from "./Http";

export function setupBrowserGlobals() {
  const env = new Env({
    LANGUAGE: typeof window !== "undefined" ? window.navigator.language : "en",
  });

  const http = new Http({
    handle: (request) => {
      const json = JSON.stringify(request);
      // Send request to mediator server
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/request", false); // synchronous request
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.send(json);

      try {
        const res = JSON.parse(xhr.responseText);
        return new HttpResponse(res.text, res.status, res.headers);
      } catch (e) {
        const headers = new Map();
        const headerText = xhr.getAllResponseHeaders();
        headerText.split("/n").forEach((line) => {
          const key = line.split(": ")[0];
          headers.set(key, line.substring(key.length));
        });
        return new HttpResponse(null, xhr.status, headers);
      }
    },
  });

  const dom: DOM = {
    createDocument(text, type?) {
      type = type === "xml" ? "application/xml" : "text/html";
      const doc = new DOMParser().parseFromString(text, type as any);
      return new BrowserDomElement(doc);
    },
  };

  const progressUpdater = new LoadingProgressUpdater((progress, message) => {
    console.log("Update progress: " + progress + ", msg: " + message);
  });

  const manifestUpdater = new ManifestUpdater();

  const configsUpdater = new ConfigsUpdater((configs) => {
    // DO NOTHING
  });

  globalThis.env = env;
  globalThis.http = http;
  globalThis.DOM = dom;
  globalThis.progressUpdater = progressUpdater;
  globalThis.manifestUpdater = manifestUpdater;
  globalThis.configsUpdater = configsUpdater;
}

class BrowserDomElement implements DomElement {
  private element: Element;

  constructor(element: any) {
    this.element = element;
  }

  select(cssQuery: string): DomElement | null {
    const ele = this.element.querySelector(cssQuery);
    return ele !== null ? new BrowserDomElement(ele) : null;
  }

  selectAll(cssQuery: string): DomElement[] {
    const elements = this.element.querySelectorAll(cssQuery);
    const wrappedElements = new Array(elements.length);
    for (let i = 0; i < elements.length; i++) {
      wrappedElements[i] = new BrowserDomElement(elements[i]);
    }
    return wrappedElements;
  }

  attr<V>(name: string, value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.element.setAttribute(name, value);
    } else {
      return this.element.getAttribute(name) as StringOrVoid<V>;
    }
  }

  text<V>(value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.element.textContent = value;
    } else {
      return this.element.textContent as StringOrVoid<V>;
    }
  }

  html<V>(html?: V): StringOrVoid<V> {
    if (typeof html === "string") {
      this.element.innerHTML = html;
    } else {
      return this.element.innerHTML as StringOrVoid<V>;
    }
  }
}
