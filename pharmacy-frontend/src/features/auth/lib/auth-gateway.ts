import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

import { createDemoAuthGateway } from "./demo-auth-gateway";
import { createSessionAuthGateway } from "./session-auth-gateway";

export const authGateway = DEMO_AUTH_ENABLED
  ? createDemoAuthGateway()
  : createSessionAuthGateway();
