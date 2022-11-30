import { AnyService } from "./AnyService";
import { Feature } from "./feature/Feature";
import { AnyConfigFeature } from "./feature/ConfigFeature";
import { AnyPostFeature } from "./feature/PostFeature";
import { AnyUserFeature } from "./feature/UserFeature";

/**
 * The service feature manager.
 *
 * @since 0.1.0
 */
export class Features {
  private _service: AnyService;

  private _constructorId = 0;

  private _featureConstructors: any = {};

  private _features: any = {};

  constructor(service: AnyService | null) {
    this._service = service;
  }

  isAdded<S extends Feature>(
    ctor: new (service: AnyService) => S
  ): boolean {
    const keys = Object.keys(this._featureConstructors);
    for (const k of keys) {
      const c = this._featureConstructors[k];
      if (c === ctor || c.prototype instanceof ctor) {
        return true;
      }
    }
    return false;
  }

  add<S extends Feature>(ctor: new (service: AnyService) => S) {
    const proto = ctor.prototype;
    if (
      proto === Feature.prototype ||
      proto === AnyConfigFeature.prototype ||
      proto === AnyPostFeature.prototype ||
      proto === AnyUserFeature.prototype
    ) {
      throw `Cannot register a builtin feature: ${ctor.name}`;
    }
    const id = this._getOrDefConstructorId(ctor);
    this._featureConstructors[id] = ctor;
  }

  getOrCreate<S extends Feature>(ctor: new (service: AnyService) => S): S {
    let id = ctor["id"];
    if (id === undefined) {
      id = this._getOrDefConstructorId(ctor);
    }

    if (this._features[id] !== undefined) {
      return this._features[id];
    }

    const targetConstructor = this._getTargetConstructor(ctor);
    if (targetConstructor === null) {
      throw `Feature: ${ctor.name} is not registered`;
    }
    id = this._getOrDefConstructorId(targetConstructor);

    const feature = new targetConstructor(this._service);
    this._features[id] = feature;
    return feature;
  }

  private _getOrDefConstructorId(ctor: any): number {
    if (ctor.hasOwnProperty("id")) {
      return ctor["id"] as number;
    }
    const id = this._constructorId++;
    Object.defineProperty(ctor, "id", {
      enumerable: false,
      configurable: false,
      writable: false,
      value: id,
    });
    return id;
  }

  private _getTargetConstructor<S extends Feature>(
    ctor: new (service: AnyService) => S
  ): new (service: AnyService) => S {
    let targetConstructor: any = null;
    const keys = Object.keys(this._featureConstructors);
    for (const k of keys) {
      const c = this._featureConstructors[k];
      if (c === ctor || c.prototype instanceof ctor) {
        targetConstructor = c;
        break;
      }
    }
    return targetConstructor;
  }
}
