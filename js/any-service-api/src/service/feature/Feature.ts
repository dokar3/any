import { AnyService } from "../AnyService";

/**
 * The base service. It provides access to the {@link AnyService} instance.
 *
 * @since 0.1.0
 */
export class Feature {
  /**
   * The service instance.
   *
   * @since 0.1.0
   */
  service: AnyService;

  constructor(service: AnyService) {
    this.service = service;
  }
}
