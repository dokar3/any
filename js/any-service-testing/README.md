# any-testing

Create services on the node.js environment for testing purposes.

# Usage

```typescript
import { createTestService } from "any-testing";
import { MyService } from "./MyService";
import { features } from "../src/main.ts";

const service = createTestService({
  features: features,
  manifestPath: "manifest.json",
});
// Call service or feature functions
```

