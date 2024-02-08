import Webpack from "webpack";
import path from "path";
import process from "process";
import { fileURLToPath } from "url";
import { dirname } from "path";
import TerserPlugin from "terser-webpack-plugin";
import * as esbuild from "esbuild";
import ifdefPlugin from "esbuild-ifdef";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Compile js source files using Esbuild.
 *
 * @param {string[]} filePaths paths Source file paths.
 * @param {string} outputDir Output directory.
 * @param {string} outputFilename Output filename, only works if the size of filePaths is 1.
 * @param {string} platform Target platform.
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 */
async function esbuildCompileJsSources(
  filePaths,
  outputDir,
  outputFilename,
  platform,
  minimize
) {
  if (filePaths.length === 0) {
    throw new Error("No files to compile.");
  }
  await esbuild.build({
    entryPoints: filePaths,
    bundle: true,
    target: "esnext",
    outdir: filePaths.length > 1 ? outputDir : undefined,
    outfile:
      filePaths.length == 1 ? path.join(outputDir, outputFilename) : undefined,
    plugins: [
      ifdefPlugin({
        variables: {
          platform: platform,
        },
      }),
    ],
    minify: minimize ?? true,
  });
}

/**
 * Compile js source files using Webpack.
 *
 * @param {string[]} filePaths paths Source file paths.
 * @param {string} outputDir Output directory.
 * @param {string} outputFilename Output filename.
 * @param {string} platform Target platform.
 * @param {object[]} [plugins] Optional webpack plugins.
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 */
function compileJsSources(
  filePaths,
  outputDir,
  outputFilename,
  platform,
  plugins,
  minimize
) {
  console.log(
    "Compiling js sources, platform: " + platform + ", minimize: " + minimize
  );

  const ifdefOpts = {
    platform: platform,
    "ifdef-verbose": false, // add this for verbose output
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
    plugins: [
      [
        "@babel/plugin-proposal-class-static-block",
        {
          loose: true,
        },
      ],
      [
        "@babel/plugin-proposal-class-properties",
        {
          loose: true,
        },
      ],
      ["babel-plugin-inline-import"],
    ],
  };

  const compiler = Webpack([
    {
      entry: filePaths,
      output: {
        path: outputDir,
        filename: outputFilename,
        environment: {
          // Prevent top level IIFE arrow function
          arrowFunction: false,
        },
      },
      mode: "production",
      optimization: {
        minimize: minimize === true,
        minimizer: [
          new TerserPlugin({
            terserOptions: {
              ecma: 2015,
              mangle: {
                reserved: [
                  /* "createApp" */
                ],
              },
              module: false,
            },
          }),
        ],
        usedExports: false,
      },
      module: {
        rules: [
          {
            test: /\.(js|ts|tsx)$/,
            use: [
              { loader: "babel-loader", options: babelOpts },
              { loader: "ifdef-loader", options: ifdefOpts },
            ],
          },
        ],
      },
      plugins: plugins ?? undefined,
      resolveLoader: {
        modules: [
          path.join(process.cwd(), "node_modules"),
          path.join(__dirname, "../node_modules"),
        ],
      },
      resolve: {
        extensions: [".ts", ".tsx", "..."],
      },
    },
  ]);
  return new Promise((resolve, reject) => {
    compiler.run((err, stats) => {
      if (err) {
        console.error(err.stack || err);
        if (err.details) {
          console.error(err.details);
        }
        reject(err.message);
      } else if (stats.hasErrors()) {
        const errors = stats.toJson().errors;
        console.error(errors);
        reject(errors);
      } else if (stats.hasWarnings()) {
        console.warn(stats.toJson().warnings);
        resolve();
      } else {
        resolve();
      }
    });
  });
}

export { esbuildCompileJsSources, compileJsSources };
