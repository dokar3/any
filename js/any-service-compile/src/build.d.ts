/**
 * The build outputs.
 */
export type BuildOutputs = {
    /**
     * Path of the output manifest.
     */
    manifest: string;
    /**
     * Path of the output directory.
     */
    dir: string;
    /**
     * Path of the output zip.
     */
    zip?: string;
};
/**
 * @typedef BuildOutputs The build outputs.
 *
 * @property {string} manifest Path of the output manifest.
 * @property {string} dir Path of the output directory.
 * @property {string} [zip] Path of the output zip.
 */
/**
 * Build service.
 *
 * @param {string} outputDir Output directory.
 * @param {string} platform Build platform, 'android' and 'browser are supported.
 * @param {boolean} shouldPack Should pack build outputs to a zip archive.
 * @param {string[]} [manifestNames] Specify the service manifest and source to compile. Will compile
 * all manifests if null or undefined
 * @param {boolean} [minimize] Enable the minimize optimization. Defaults to true.
 * @returns {Promise<BuildOutputs>} Build Promise object.
 */
export function buildService(outputDir: string, platform: string, shouldPack: boolean, manifestNames?: string[], minimize?: boolean): Promise<BuildOutputs>;
