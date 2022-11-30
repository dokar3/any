import { DomElement } from "./DomElement";

/**
 * Simple DOM interface.
 *
 * @since 0.1.0
 */
export interface DOM {
  /**
   * Create a document from html/xml.
   *
   * @param {string} text The html or xml string.
   * @param {string} [type] Parser type, 'html' or 'xml'. Defaults to 'html'.
   * @returns {DomElement} The root document.
   *
   * @since 0.1.0
   */
  createDocument(text: string, type?: string): DomElement;
}
