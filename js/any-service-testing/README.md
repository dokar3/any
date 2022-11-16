# any-testing

Create services on the node.js environment for testing purposes.

# Usage

```typescript
import { createTestService } from "any-testing";
import { MyService } from "./MyService";

const service = createTestService({
  serviceClass: MyService,
  manifestPath: "manifest.json",
});
// Call service functions
```

