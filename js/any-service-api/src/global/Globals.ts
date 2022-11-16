import { DOM } from "./Dom";
import { Env } from "./Env";
import { Http } from "./Http";

declare global {
  /**
   * The environment object.
   *
   * @since 0.1.0
   */
  var env: Env;

  /**
   * Http instance which provides GET and POST methods.
   *
   * @since 0.1.0
   */
  var http: Http;

  /**
   * The DOM implementation used to create documents from html/xml.
   *
   * @since 0.1.0
   */
  var DOM: DOM;
}

export {};
