import process from "process";
import fs from "fs";
import * as path from "path";
import Webpack from "webpack";
import HtmlWebpackPlugin from "html-webpack-plugin";
import WebpackDevServer from "webpack-dev-server";
import WebpackCopyPlugin from "copy-webpack-plugin";
import SpeedMeasurePlugin from "speed-measure-webpack-plugin";
import { fileURLToPath } from "url";
import { dirname } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Start the frontend server.
 *
 * @param {number} port The server port, defaults to 10102
 * @param {number} backendServerPort The backend server port, defaults to 10101.
 */
function startFrontendRunnerServer(port, backendServerPort) {
  const currentDir = process.cwd();
  const outputDir = path.join(currentDir, "dist/runner");

  const ifdefOpts = {
    platform: "browser",
    "ifdef-verbose": true, // add this for verbose output
    "ifdef-uncomment-prefix": "// #code ", // add this to uncomment code starting with "// #code "
  };

  const babelOpts = {
    presets: [
      [
        "@babel/preset-env",
        {
          loose: true,
          modules: false,
        },
      ],
      "@babel/preset-typescript",
    ],
  };

  const manifestPath = path.join(currentDir, "manifest.json");
  if (!fs.existsSync(manifestPath)) {
    throw new Error("manifest.json not found");
  }

  const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf-8"));
  if (!manifest.main) {
    throw new Error("Field 'main' not found in manifest.json");
  }

  const compiler = Webpack({
    entry: {
      main: path.join(currentDir, manifest.main),
      index: path.join(__dirname, "../public/js/index.js"),
      app_worker: path.join(__dirname, "../public/js/app.worker.js"),
    },
    output: {
      path: outputDir,
      filename: "[name].bundle.js",
      clean: true,
    },
    mode: "development",
    module: {
      rules: [
        {
          test: /\.(js|ts|tsx)$/,
          include: [
            path.join(currentDir, "node_modules/any-api"),
            path.join(currentDir, "src"),
          ],
          use: [
            { loader: "thread-loader" },
            { loader: "babel-loader", options: babelOpts },
            { loader: "ifdef-loader", options: ifdefOpts },
          ],
        },
      ],
    },
    optimization: {
      minimize: false,
    },
    plugins: [
      new SpeedMeasurePlugin(),
      new HtmlWebpackPlugin({
        filename: "index.html",
        template: path.join(__dirname, "../public/index.html"),
        chunks: ["main"],
      }),
      new WebpackCopyPlugin({
        patterns: [
          {
            from: path.join(__dirname, "../public/js//jsdom.browserify.js"),
            to: "",
          },
          {
            from: "*.json",
            to: "",
            filter: async (resourcePath) => {
              return path.basename(resourcePath).startsWith("manifest.");
            },
          },
          {
            from: "src/**/*",
            to: "[path][name][ext]",
            toType: "template",
            filter: async (resourcePath) => {
              return (
                resourcePath.endsWith(".png") || resourcePath.endsWith(".jpg")
              );
            },
          },
        ],
      }),
    ],
    resolveLoader: {
      modules: [path.join(process.cwd(), "node_modules")],
    },
    resolve: {
      extensions: [".ts", ".tsx", "..."],
    },
  });

  if (!backendServerPort || backendServerPort <= 0) {
    backendServerPort = 10101;
  }
  const backendUrl = `http://localhost:${backendServerPort}`;

  const devServerOptions = {
    static: [
      { directory: outputDir },
      {
        directory: path.join(__dirname, "../public/images/"),
        publicPath: "/images",
      },
      { directory: path.join(__dirname, "../public/css/"), publicPath: "/css" },
    ],
    port: !port || port <= 0 ? 10102 : port,
    open: true,
    proxy: {
      "/manifests": backendUrl,
      "/request": backendUrl,
      "/compile_service": backendUrl,
      "/run_command": backendUrl,
    },
  };
  const server = new WebpackDevServer(devServerOptions, compiler);

  const start = async () => {
    console.log("Starting frontend server...");
    await server.start();
  };

  start();
}

export { startFrontendRunnerServer };
