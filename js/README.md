# The JavaScript sources

### Summary

Modules in this folder are used to create, build and run services.

`/any-service-cli`

Command line tool to create a service from template.

`/any-service-api`

The APIs module. Providing both Android and browser APIs to run service. Write in TypeScript.

`/any-service-compile`

Compile and bundle service. Using `webpack`, `babel` and other tools to build compatible JS bundle
for `duktape` JS engine.

`/any-service-runner`

(WIP) Service runner for browsers. Providing web GUI to run services.

`/any-service-template`

The service template module.

`/any-service-testing`

Provides test utilities like `createTestService()` to test services on the node environment. 

`/services`

Built-in services.

### Start a new service project

*The following commands are only tested on Windows 10*

**Requirements:**

- [Node.js](https://nodejs.org/) The JavaScript runtime for compiling and packing.
- [yarn](https://yarnpkg.com/) The dependency management.

**Create and run a new service:**


1. Build local dependencies (Required on the first run, rebuild if dependencies have been updated):

    ```shell
    cd /path/to/any-service-api/
    # Install dependencies
    yarn
    # Build
    yarn tsc
    
    # Same for the 'any-service-testing'
    cd /path/to/any-service-testing/
    yarn
    yarn tsc
    ```

2. Link the `any-service-cli` module (Required on the first run):

    ```shell
    cd /path/to/any-service-cli/
    npm link --bin-links
    ```

3. Run `any-service-cli` command to create new project:

    ```shell
    cd your/projects/dir
    
    any-service-cli
    ```

4. Install dependencies:

    ```shell
    cd NewProject

    yarn
    ```

5. Test, run and build:

    ```shell
    # Test with jest framework
    yarn test
    
    # (WIP) Start runner servers to debug service in a browser
    yarn runner
    
    # Build
    yarn build-android
    ```

### Other commands and scripts

**Run commands for all built-in projects**

```shell
cd /path/to/services/dir/

# Install dependencies for all projects
node runInEachProject.js yarn

# Upgrade 'typescript' for all projects
node runInEachProject.js yarn up typescript
```

**Build all built-in services:**

```shell
cd /path/to/services/dir/

node buildAll.js --platform=android --output=/path/to/output_dir
```
