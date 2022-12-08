# Classes and objects

Unlike the browser environment, the service runtime does not have a `window` object, the following
classes and objects are provided for creating services alternatively:

| Class              | Description                               | Details                                                                                                            |
|:-------------------|:------------------------------------------|:-------------------------------------------------------------------------------------------------------------------|
| `AnyService`       | Features, configs and manifest management | Add features in the `onCreate()` function                                                                          |
| `ServiceRegistry`  | service registration                      | Call `ServiceRegistry.register(ServiceClass)` to register the implemented service                                  |
| `Feature`          | Basic feature                             | The base feature, all features must extend this class                                                              |
| `AnyPostFeature`   | Post-related data fetching                | Extend this class and implement fetch functions, then add it in the `onCreate()` of service class                  |
| `AnyUserFeature`   | User-related data fetching                | Extend this class and implement fetch functions, then add it in the `onCreate()` of service class                  |
| `AnyConfigFeature` | Service configs validation                | Extend this class and implement the `validateConfigs()` function, then add it in the `onCreate()` of service class |
| `DomElement`       | The html/xml element                      | Use the global function `$(String)` to create a DOM element from html/xml                                          |
| `DOM`              | Simple DOM implementation                 | Use `DOM.createDocument()` to create a document from html/xml string. Exported as `globalThis.DOM`                 |
| `Console`          | Logging                                   | `log()`, `info()`, `warn()` and `error()` are supported                                                            |
| `Http`             | Network                                   | Provides `get()` and `post()` to fetch and send data. Exported as `globalThis.http`                                |
| `Env`              | Environment properties                    | Used to access the service runtime environment properties like `LANGUAGE`. Exported as `globalThis.env`            |

# Create services (TypeScript)

**It's recommended to use `any-service-cli` to create a service quickly. The following steps show how to
create
a service project from scratch.**

1. Create a npm new project:

   ```shell
    mkdir MyService
    cd MyService
    npm init
   ```

2. Update the `package.json`:

   ```json
   {
     "name": "MyService",
     "version": "1.0.0",
     "description": "Description of your service",
     "author": "Author",
     "license": "Apache-2.0",
     "type": "module",
     "scripts": {
       "build-android": "any-service-build --platform=android --output=./dist/",
       "build-desktop": "any-service-build --platform=desktop --output=./dist/",
       "build-browser": "any-service-build --platform=browser --output=./dist/",
       "any-service-runner-backend": "any-service-runner --backend",
       "any-service-runner-frontend": "any-service-runner --frontend",
       "runner": "concurrently --kill-others -n \"Backend \",\"Frontend\" \"npm run any-service-runner-backend\" \"npm run any-service-runner-frontend\""
     },
     "dependencies": {
       "any-service-api": "portal:path/to/any-service-api"
     },
     "devDependencies": {
       "any-service-compile": "portal:path/to/any-service-compile",
       "any-service-runner": "portal:path/to/any-service-runner",
       "concurrently": "^7.1.0"
     }
   }
   ```

3. Add the `manifest.json` file to the project's root directory.
   See [Service manifest properties](#service-manifest-properties) for details:

   ```json
   {
     "id": "me.myservice",
     "minApiVersion": "1.0.0",
     "name": "MyService",
     "description": "Description of your service",
     "developer": "Author",
     "version": "1.0.0",
     "isPageable": true,
     "postsViewType": "card",
     "mediaAspectRatio": "12:7",
     "main": "src/main.ts"
   }
   ```

4. Create an `AnyPostFeature` and implement the fetch functions:

   ```typescript
   // src/PostService.ts
   import {
     AnyPostFeature,
     FetchFreshListParams,
     PagedResult,
     Post,
   } from "any-service-api";

   export class PostFeature extends AnyPostFeature {
     fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
       // Implement this function
     }

     // Implement more functions if needed
   }
   ```

5. Create an `AnyService` and add the post feature we created before:

   ```typescript
   // src/Service.ts
   import { AnyService } from "any-service-api";
   import { PostFeature } from "./PostFeature";

   export class Service extends AnyService {
     onCreate(): void {
       this.addFeature(PostFeature);
     }
   }
   ```

6. Register the service in the `main.ts`:

   ```javascript
   // src/main.ts
   import { ServiceRegistry } from "any-service-api";
   import { Service } from "Service";

   ServiceRegistry.register(Service);
   ```

7. Run service in a browser:

   ```shell
   yarn runner
   ```

8. Build service
   ```shell
   yarn build-android
   ```

# Service manifest properties

_the `*` mark means it is required_

| Name                     | Type     | Description                                                                                                                                                                          |
|--------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id` \*                  | string   | The unique service id. e.g. `com.name.myservice`, `io.name.myservice`. To use a dynamic id based on user configurations, see the [Dynamic service id](#dynamic-service-id) section   |
| `minApiVersion` \*       | string   | Minimum version of `any-service-api`. e.g. `1.0.0`                                                                                                                                   |
| `maxApiVersion`          | string   | Maximum version of `any-service-api`. e.g. `1.0.0`                                                                                                                                   |
| `version` \*             | string   | Version of the service, e.g. `1.0.0`, `0.0.1`                                                                                                                                        |
| `name` \*                | string   | Service name                                                                                                                                                                         |
| `description` \*         | string   | A description text, markdown is supported.                                                                                                                                           |
| `developer` \*           | string   | The name of developer/author                                                                                                                                                         |
| `developerUrl`           | string   | The url of developer/author                                                                                                                                                          |
| `developerAvatar`        | string   | The avatar url of developer/author                                                                                                                                                   |
| `homepage`               | string   | The home page or repository url of the service                                                                                                                                       |
| `changelog`              | string   | The changelog, could be raw text, markdown text, local text file path, local markdown file path or http url (starts with 'http://' or 'https://')                                    |
| `isPageable` \*          | boolean  | `true` if the service can load multiple pages of posts. Page number starts with 1                                                                                                    |
| `postsViewType` \*       | string   | View type of the post list. `list`, `grid`, `card` and`full_width` are supported                                                                                                     |
| `mediaAspectRatio` \*    | string   | Default aspect ratio of post media objects (thumbnails), e.g. `5:3`, `1024:720`                                                                                                      |
| `main` \*                | string   | The program entry point, service should be registered in the main source file. Both local file path and http url are supported. e.g. `src/main.js`, `https://my.main.js`             |
| `icon`                   | string   | Service's icon, both local file path and http url are supported. e.g. `/src/assets/icon.png`, `https://my.icon.png`                                                                  |
| `headerImage`            | string   | Service's header image, both local file path and http url are supported. e.g. `src/assets/header.jpg`, `https://my.header.jpg`                                                       |
| `themeColor`             | string   | A hex color value string used to stylize service-specific UI                                                                                                                         |
| `darkThemeColor`         | string   | A hex color value string used to stylize service-specific UI under the dark theme                                                                                                    |
| `languages`              | string[] | [IETF BCP 47 language tags](https://en.wikipedia.org/wiki/IETF_language_tag) of this service or its content                                                                          |
| `supportedPostUrls`      | string[] | A string array of post urls supported by this service, wildcards are supported. e.g. `["https://my.website.com/post/*.html"]`                                                        |
| `supportedUserUrls`      | string[] | A string array of user urls supported by this service, wildcards are supported. e.g. `["https://my.website.com/user/*"]`                                                             |
| `configs`                | object[] | Configurable fields for this service. See [Service configurations](#service-configurations) for details                                                                              |
| `forceConfigsValidation` | boolean  | If true, `ConfigFeature.validateConfigs()` will always be called before adding the service, defaults to false. If `validateConfigs()` is not implemented, validation will be skipped |

# Service configurations

### Configuration fields

_the `*` mark means it is required_

| Name            | Type    | Description                                                                                                     |
|-----------------|---------|-----------------------------------------------------------------------------------------------------------------|
| `name` \*       | string  | Name of this configuration                                                                                      |
| `key` \*        | string  | Key for reading configuration value in code                                                                     |
| `type` \*       | string  | The configuration type. `text`, `url`, `number`, `boolean`, `option`, `cookies` and `cookies_ua` are supported  |
| `description`   | string  | Short description text to describe this configuration                                                           |
| `required`      | boolean | `true` if this configuration is required (cannot be null or empty). Defaults to `false`                         |
| `visibleToUser` | boolean | `true` if user can view and edit this configuration. Defaults to `true`                                         |
| `value`         | string\ | boolean\                                                                                                        |number | The default/preset value                                     |
| `options`       | object  | A list of option. For `option` configs only, `name: string` and `value: string` are required in the object      |
| `requestUrl`    | string  | The url used to launch a WebView for requesting cookies.  For `cookies` and `cookies_ua` configs only, required |
| `targetUrl`     | string  | The url of the cookies to request. For `cookies` and `cookies_ua` configs only, required                        |
| `userAgent`     | string  | The user agent for WebView when requesting cookies. For `cookies` and `cookies_ua` configs only, optional       |

### Usages

- Text, url, boolean and number:

  ```json
  {
    "configs": [
      {
        "name": "Api key",
        "description": "Get the key: https://some.website.com",
        "key": "apiKey",
        "type": "text",
        "required": true,
        "value": "XYZ"
      },
      {
        "name": "RSS url",
        "description": "https://my.website.com/feed.rss",
        "key": "rssUrl",
        "type": "url",
        "required": true
      },
      {
        "name": "Always open in browser",
        "description": "Do not use the built-in reader",
        "key": "alwaysOpenInBrowser",
        "type": "boolean",
        "required": true,
        "value": true
      },
      {
        "name": "Post count per page",
        "description": "Post count for single page",
        "key": "postCountPerPage",
        "type": "number",
        "required": true,
        "value": 20
      }
    ]
  }
  ```

  Read:

  ```javascript
  class MyPostService extends PostFeature {
    fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
      const apiKey = this.service.configs.apiKey;
      const rssUrl = this.service.configs.rssUrl;
      const alwaysOpenInBrowser = this.service.configs.alwaysOpenInBrowser;
      const postCountPerPage = this.service.configs.postCountPerPage;
      // ...
    }
  }
  ```

- Option:

  ```json
  {
    "configs": [
      {
        "name": "News type",
        "description": "What type of news you want to read?",
        "key": "newsType",
        "type": "option",
        "required": true,
        "value": "tech",
        "options": [
          {
            "name": "Technology",
            "value": "tech"
          },
          {
            "name": "Sports",
            "value": "sports"
          },
          {
            "name": "Games",
            "value": "games"
          },
          {
            "name": "Politics",
            "value": "politics"
          }
        ]
      }
    ]
  }
  ```

  Read:

  ```javascript
  class MyPostService extends PostFeature {
    fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
      const newType = this.service.configs.newsType;
      // ...
    }
  }
  ```

- Cookie:

  ```json
  {
    "configs": [
      {
        "name": "Login",
        "description": "Connect to https://some.website.com",
        "key": "myCookie",
        "type": "cookie",
        "required": true,
        "requestUrl": "https://some.website.com/login",
        "targetUrl": "https://some.website.com",
        "userAgent": "Specify a user agent if needed"
      }
    ]
  }
  ```
  
  `targetUrl` field can be an http url, or just a host name, e.g. `some.website.com`.

  `userAgent` field is optional, use it when a custom user agent is needed.

  Read:

  ```javascript
class MyPostService extends PostFeature {
    fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
      const cookies = this.service.configs.myCookies;
      // ...
    }
  }
  ```
  
- Cookie and user agent:

  ```json
  {
    "configs": [
      {
        "name": "Login",
        "description": "Connect to https://some.website.com",
        "key": "myCookieAndUserAgent",
        "type": "cookie_ua",
        "required": true,
        "requestUrl": "https://some.website.com/login",
        "targetUrl": "https://some.website.com",
        "userAgent": "Specify a user agent if needed, optional field"
      }
    ]
  }
  ```
  
  `targetUrl` field can be an http url, or just a host name, e.g. `some.website.com`.

  `userAgent` field is optional, use it when a custom user agent is needed.

  Read:

  ```javascript
class MyPostService extends PostFeature {
    fetchFreshList(params: FetchFreshListParams): PagedResult<Post[]> {
      const userAgent = this.service.configs.myUaAndCookies.userAgent;
      const cookies = this.service.configs.myUaAndCookies.cookies;
      // ...
    }
  }
  ```

# Dynamic service id

If there are some services with the same id, users can only add one of them, but sometime we may
need to
allow users to add multiple services from the same service source code, e.g. the RSS service. Using
the dynamic id
definition allows generating a dynamic id based on user configurations.

_Dynamic ids are only working when adding a service, once the service is added, config changes will
not
affect the generated service id._

### Pattern

```c
// {configKey} will be replaced with the value user has been set
{CONFIG_KEY}

// Or use hash() to shortify the config value to a crc32c hashed hex string
{hash(CONFIG_KEY)}
```

### Usages

- Basic:

  ```json
  {
    "id": "myservice.{rssUrl}"
  }
  ```

  The generated service id will be something like `myservice.https://some.url`

- Shortify by using `hash()`:

  ```
  {
    "id": "myservice.{hash(rssUrl)}"
  }
  ```

  The generated service id will be something like `myservice.a8cb7544`

- Based on service name:

  ```json
  {
    "id": "myservice.{serviceName}"
  }
  ```

  The generated service id will be something like `myservice.user_specified_service_name`
