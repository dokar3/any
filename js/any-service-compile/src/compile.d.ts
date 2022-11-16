/**
 * Compile js source files.
 *
 * @param {string[]} file paths Source file paths.
 * @param {string} outputDir Output directory.
 * @param {string} outputFilename Output filename.
 * @param {string} platform Target platform.
 * @param {object[]} [plugins] Optional webpack plugins.
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 */
export function compileJsSources(filePaths: any, outputDir: string, outputFilename: string, platform: string, plugins?: object[], minimize?: boolean): Promise<any>;
