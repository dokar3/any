/**
 * Build service.
 *
 * @param {string} outputDir Output directory.
 * @param {string} platform Build platform, 'android' and 'browser are supported.
 * @param {boolean} shouldPack Should pack build outputs to a zip archive.
 * @param {string[]} [manifestNames] Specify the service manifest and source to compile. Will compile
 * all manifests if null or undefined
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 * @returns {Promise} Build Promise object.
 */
export function buildService(outputDir: string, platform: string, shouldPack: boolean, manifestNames?: string[], minimize?: boolean): Promise<any>;
