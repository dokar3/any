import {
  DOM,
  DomElement,
  Env,
  Http,
  HttpRequest,
  HttpResponse,
  StringOrVoid,
} from "any-service-api";
import { IncomingHttpHeaders } from "http";
import { JSDOM } from "jsdom";
import { osLocaleSync } from "os-locale";
import syncRequest from "sync-request";

const env = new Env({
  LANGUAGE: osLocaleSync(),
});

const http = new Http({
  handle: (request: HttpRequest) => {
    const resp = syncRequest(
      request.method === "POST" ? "POST" : "GET",
      request.url!!,
      {
        timeout: request.timeout,
        headers: request.headers as IncomingHttpHeaders,
        body: request.method === "POST" ? request.params : undefined,
      }
    );
    const respHeaders = resp.headers;
    const headers = new Map<string, string>();
    for (const key of Object.keys(respHeaders)) {
      headers[key] = respHeaders[key];
    }
    return new HttpResponse(resp.body.toString(), resp.statusCode, headers);
  },
});

const dom: DOM = {
  createDocument(text, type?) {
    type = type === "xml" ? "application/xml" : "text/html";
    const doc = new JSDOM(text, { contentType: type }).window.document;
    return new JsDomElement(doc);
  },
};

export function setupTestGlobals() {
  globalThis.env = env;
  globalThis.http = http;
  globalThis.DOM = dom;
}

class JsDomElement implements DomElement {
  private element: any;

  constructor(element: any) {
    this.element = element;
  }

  select(cssQuery: string): DomElement | null {
    const ele = this.element.querySelector(cssQuery);
    return ele !== null ? new JsDomElement(ele) : null;
  }

  selectAll(cssQuery: string): DomElement[] {
    const elements = this.element.querySelectorAll(cssQuery);
    const wrappedElements = [elements.length];
    for (let i = 0; i < elements.length; i++) {
      wrappedElements[i] = new JsDomElement(elements[i]);
    }
    return wrappedElements;
  }

  attr<V>(name: string, value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.element.setAttribute(name, value);
    } else {
      return this.element.getAttribute(name);
    }
  }

  text<V>(value?: V): StringOrVoid<V> {
    if (typeof value === "string") {
      this.element.textContent = value;
    } else {
      return this.element.textContent;
    }
  }

  html<V>(html?: V): StringOrVoid<V> {
    if (typeof html === "string") {
      this.element.innerHTML = html;
    } else {
      return this.element.innerHTML;
    }
  }
}
