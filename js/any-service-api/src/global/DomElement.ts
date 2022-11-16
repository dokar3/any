/**
 * Returns a string if [V] is a string otherwise void.
 */
export type StringOrVoid<V> = V extends string ? void : string;

/**
 * The DOM element.
 *
 * @since 0.1.0
 */
export interface DomElement {
  /**
   * Find first matching child.
   *
   * @param cssQuery Css selector.
   * @return {DomElement} First child, null if not found.
   * @since 0.1.0
   */
  select(cssQuery: string): DomElement | null;

  /**
   * Find all matching children.
   *
   * @param {string} cssQuery Css selector.
   * @return {DomElement[]} Children that match the query.
   * @since 0.1.0
   */
  selectAll(cssQuery: string): DomElement[];

  /**
   * Get/set the element attribute value.
   *
   * @param {string} name Attribute name.
   * @param {string} [value] The attribute value for the setter.
   * @return {string} Attribute text, empty if absent. Returns void if the value is absent.
   * @since 0.1.0
   */
  attr<V>(name: string, value?: V): StringOrVoid<V>;

  /**
   * Get/set the element text.
   *
   * @param {string} [value] The element text for the setter.
   * @return {string} The element text. Returns void if the value is absent.
   * @since 0.1.0
   */
  text<V>(value?: V): StringOrVoid<V>;

  /**
   * Get/set the element inner html.
   *
   * @param {string} [html] The element html for the setter.
   * @return {string} The html string. Returns void if the html is absent.
   * @since 0.1.0
   */
  html<V>(html?: V): StringOrVoid<V>;
}
