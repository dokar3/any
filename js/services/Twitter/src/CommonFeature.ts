import { Feature, HttpResponse } from "any-service-api";

export class CommonFeature extends Feature {
  checkLogin(): boolean {
    const cookiesAuthCode = this.getCookiesAuthCode();
    const currentAuthCode = this.getCurrentAuthCode();
    if (!cookiesAuthCode) {
      return currentAuthCode !== null;
    }
    if (currentAuthCode) {
      if (cookiesAuthCode !== currentAuthCode) {
        // Login state changed
        this.service.configs.authCode = cookiesAuthCode;
        this.service.configs.bearerToken = null;
      }
    } else {
      this.service.configs.authCode = cookiesAuthCode;
    }
    return true;
  }

  checkBearerToken(): boolean {
    const bearerToken = this.service.configs.bearerToken;
    if (bearerToken) {
      return true;
    }
    return false;
  }

  checkMyUserId(): boolean {
    return (
      this.service.configs.userId !== undefined &&
      this.service.configs.userId !== null
    );
  }

  private getCurrentAuthCode(): string {
    return this.service.configs.authCode;
  }

  private getCookiesAuthCode(): string {
    const authCookies = this.service.configs.authCookies;
    if (!authCookies) {
      return null;
    }
    const match = /twitter_auth_code=([A-Za-z0-9]+)/.exec(authCookies);
    if (!match) {
      return null;
    }
    return match[1];
  }

  requestBearerToken(): void {
    const authCode = this.getCurrentAuthCode();
    if (!authCode) {
      throw new Error("Cannot request a bearer token: Not logged in yet");
    }
    const res = http.post("https://api.twitter.com/2/oauth2/token", {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      params: {
        code: authCode,
        grant_type: "authorization_code",
        client_id: this.service.configs.clientId,
        redirect_uri: this.service.configs.redirectUri,
        code_verifier: "challenge",
      },
    });

    this.tryUpdateTokens(res);
  }

  private refreshBearerToken() {
    const authCode = this.getCurrentAuthCode();
    if (!authCode) {
      throw new Error("Not logged in yet");
    }

    const refreshToken = this.service.configs.refreshToken;
    if (!refreshToken) {
      throw new Error("No login info, please login again");
    }

    const res = http.post("https://api.twitter.com/2/oauth2/token", {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      params: {
        code: authCode,
        grant_type: "refresh_token",
        client_id: this.service.configs.clientId,
        refresh_token: refreshToken,
      },
    });

    this.tryUpdateTokens(res);
  }

  private tryUpdateTokens(res: HttpResponse) {
    const data = JSON.parse(res.text);
    const bearerToken = data.access_token;
    if (!bearerToken) {
      console.error("Cannot fetch the bearer token: " + res.text);
      throw new Error("Invalid login info, please login again");
    }
    console.log("Write bearer token: " + bearerToken);
    this.service.configs.bearerToken = bearerToken;

    const refreshToken = data.refresh_token;
    if (!refreshToken) {
      console.error("Cannot get the refresh token");
      throw new Error("Invalid login info, please login again");
    }
    console.log("Write refresh token: " + refreshToken);
    this.service.configs.refreshToken = refreshToken;
  }

  requestMyUserId(): void {
    const url = "https://api.twitter.com/2/users/me";
    const res = this.authorizedGet(url, () => this.service.configs.bearerToken);
    const e = JSON.parse(res.text);
    const userId = e.data?.id;
    if (!userId) {
      console.error("Cannot request user id, response: " + res.text);
      throw new Error("Cannot fetch user info");
    }
    console.log("Write user id: " + userId);
    // Save user id
    this.service.configs.userId = userId;
  }

  /**
   * Make an authorized http GET request.
   *
   * @param {string} url The request url.
   * @param {string} bearerToken The bearer token getter.
   * @returns {HttpResponse} The http response.
   */
  authorizedGet(url: string, bearerToken: () => string): HttpResponse {
    function get() {
      const token = bearerToken();
      if (!token) {
        console.log("Cannot make a request: Missing bearer token");
        throw new Error("No login info, please login again");
      }
      http.debug = true;
      return http.get(url, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
    }

    const res = get();

    if (this.isUnauthorizedResponse(res)) {
      // Refresh the token
      this.refreshBearerToken();
      return get();
    } else {
      return res;
    }
  }

  /**
   * Check if the http response is a unauthorized response.
   *
   * @param {HttpResponse} res Http response.
   */
  private isUnauthorizedResponse(res: HttpResponse): boolean {
    const e = JSON.parse(res.text);
    return e.status === 401 && e.title === "Unauthorized";
  }
}
