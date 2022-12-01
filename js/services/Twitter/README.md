### API key required

You need an API key (`client id`) to make this service work, the following steps may help to get and set your own key:

1. Create a developer account and login: [Twitter Developer Portal](https://developer.twitter.com/en/portal/dashboard)

2. Create a project in the developer portal, then create an developer app.

3. Find your client id in the developer portal:
   'Keys and tokens' -> 'OAuth 2.0 Client ID and Client Secret'

4. Create a `.env` file in the project root directory and put your client id in:

   ```properties
   CLIENT_ID=YOUR_CLIENT_ID
   ```

5. Set the redirect URL in the developer portal:
   'User authentication settings' -> 'App info' -> 'Callback URI / Redirect URL'
   
   And set to `https://dokar3.github.io/twitter_oauth_redirect`
   
   Check the redirect page repository if you want to use your own page: [twitter_oauth_redirect](https://github.com/dokar3/twitter_oauth_redirect)

