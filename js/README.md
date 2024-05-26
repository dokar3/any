# The JavaScript sources

### Summary

Modules in this folder are used to create, build and run services.

`/any-service-cli`

Command line tool to create a service from template.

`/any-service-api`

The service API module, written in TypeScript.

`/any-service-compile`

Using `webpack`, `babel` and other tools to compile and bundle services.

`/any-service-runner`

(WIP) Service runner for browsers. Providing web ui to run services.

`/any-service-template`

The service template module.

`/any-service-testing`

Provides test utility functions like `createTestService()` to test services on the node environment. 

`/services`

Built-in services.

### Start a new service project

*The following commands are only tested on Windows 10*

**Requirements:**

- [Bun](https://bun.sh/) The JavaScript runtime and package manager.

**Setup:**

```shell
cd project/root/dir

node ./scripts/service/setup.js
```

The `setup.js` script will do:

1. Install dependencies for projects in the `js/any-***` and `js/services/` folder
2. Build local dependencies, e.g. `js/any-service-api`, `js/any-service-test`
3. Link `js/any-service-cli` so you can use the `any-service-cli` command to create a new project

**Create and run a new service:**


1. Run `any-service-cli` command to create new project:

    ```shell
    cd js/services/
    
    any-service-cli
    ```

2. Install dependencies:

    ```shell
    cd NewProject

    bun install
    ```

3. Test, run and build:

    ```shell
    # Test with jest framework
    bun test
    
    # (WIP) Start runner servers to debug service in a browser
    bun runner
    
    # Build
    bun build-android
    ```

### Other commands and scripts

**Run commands for all built-in projects**

```shell
# Install dependencies for all projects
bun ./scripts/service/runInEachProject.js bun install

# Upgrade 'typescript' for all projects
bun ./scripts/service/runInEachProject.js bun update typescript
```

**Build all built-in services:**

```shell
bun ./scripts/service/buildAll.js --platform=android --output=/path/to/output_dir
```
