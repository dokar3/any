/**
 * Http request parameters.
 *
 * @since 0.1.0
 */
export class HttpRequest {
  /**
   * Request url.
   *
   * @since 0.1.0
   */
  url?: string;

  /**
   * Request method, 'GET' or 'POST'.
   *
   * @since 0.1.0
   */

  method?: string;
  /**
   * Request params, 'POST' only.
   *
   * @since 0.1.0
   */
  params?: any;

  /**
   * Request headers.
   *
   * @since 0.1.0
   */
  headers?: any;

  /**
   * Timeout.
   *
   * @since 0.1.0
   */
  timeout?: number;
}

/**
 * Http response.
 *
 * @since 0.1.0
 */
export class HttpResponse {
  /**
   * Response text.
   *
   * @since 0.1.0
   */
  text: string | null;

  /**
   * Response status.
   *
   * @since 0.1.0
   */
  status: number;

  /**
   * Response headers.
   *
   * @since 0.1.0
   */
  headers?: Map<string, string>;

  /**
   * @param {string|null} text Response text.
   * @param {number} status Response status.
   * @param {Map<string, string>} [headers] Response headers.
   */
  constructor(
    text: string | null,
    status: number,
    headers?: Map<string, string>
  ) {
    this.text = text;
    this.status = status;
    this.headers = headers;
  }
}

/**
 * Http request handler.
 *
 * @since 0.1.0
 */
export interface HttpRequestHandler {
  handle(request: HttpRequest): HttpResponse;
}

/**
 * Http interceptor.
 *
 * @since 0.1.0
 */
export interface HttpInterceptor {
  intercept(request: HttpRequest, handler: HttpRequestHandler): HttpResponse;
}

/**
 * Simple http client, use 'get()' or 'post' to send requests.
 *
 * Only string responses are supported now.
 *
 * @since 0.1.0
 */
export class Http {
  /** @internal */
  private requestHandler: HttpRequestHandler;

  /**
   * The http interceptor. Defaults to null.
   *
   * @since 0.1.0
   */
  interceptor?: HttpInterceptor | null = null;

  /**
   * Set true to enable request logs, defaults to false.
   *
   * @since 0.1.0
   */
  debug: boolean = false;

  /**
   * @param {HttpRequestHandler} requestHandler Http request implementation.
   */
  constructor(requestHandler: HttpRequestHandler) {
    this.requestHandler = requestHandler;
  }

  /**
   * Execute a http request.
   *
   * @param {HttpRequest} request Request body.
   * @returns {HttpResponse} Response.
   * @since 0.1.0
   */
  request(request: HttpRequest): HttpResponse {
    if (request.url == null || request.method == null) {
      throw new Error("Invalid http request: " + JSON.stringify(request));
    }
    if (this.debug) {
      console.log("New http request: \n" + JSON.stringify(request));
    }
    const interceptor = this.interceptor;
    if (interceptor != null) {
      return interceptor.intercept(request, this.requestHandler);
    } else {
      return this.requestHandler.handle(request);
    }
  }

  /**
   * Execute a http get request.
   *
   * @param {string}            url      Request url.
   * @param {HttpRequest} [request] Optional request body.
   * @returns {HttpResponse} Response.
   * @since 0.1.0
   */
  get(url: string, request?: HttpRequest): HttpResponse {
    const method = "GET";
    const r = this.createRequest(url, method, request);
    return this.request(r);
  }

  /**
   * Execute a http post request.
   *
   * @param {string}            url      Request url.
   * @param {HttpRequest} [request] Optional request body.
   * @returns {HttpResponse} Response.
   * @since 0.1.0
   */
  post(url: string, request?: HttpRequest): HttpResponse {
    const method = "POST";
    const r = this.createRequest(url, method, request);
    return this.request(r);
  }

  createRequest(url: string, method: string, request?: HttpRequest) {
    const params = request?.params ?? {};
    const headers = request?.headers ?? {};
    const timeout = request?.timeout ?? 20000;
    return {
      url: url,
      method: method,
      params: params,
      headers: headers,
      timeout: timeout,
    };
  }
}
