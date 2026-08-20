import type {
  AuthGateway,
  LoginCredentials,
} from "@/features/auth/types";

import { createDemoSession, findDemoAccount } from "./demo-accounts";

const SESSION_MARKER = "pharmacy-pos:demo-session";

function wait(delayMs: number) {
  return new Promise((resolve) => window.setTimeout(resolve, delayMs));
}

function accountForCredentials(credentials: LoginCredentials) {
  const account = findDemoAccount(credentials.username);
  if (!account || account.password !== credentials.password) {
    throw new Error("Invalid username or password.");
  }
  return account;
}

export function createDemoAuthGateway(delayMs = 350): AuthGateway {
  return {
    async login(credentials: LoginCredentials) {
      await wait(delayMs);
      const account = accountForCredentials(credentials);
      window.sessionStorage.setItem(SESSION_MARKER, account.username);
      return createDemoSession(account);
    },
    async logout() {
      window.sessionStorage.removeItem(SESSION_MARKER);
    },
    async restore() {
      const username = window.sessionStorage.getItem(SESSION_MARKER);
      const account = username ? findDemoAccount(username) : undefined;
      return account ? createDemoSession(account) : null;
    },
    async switchBranch(branchId: string) {
      const username = window.sessionStorage.getItem(SESSION_MARKER);
      const account = username ? findDemoAccount(username) : undefined;
      if (!account) throw new Error("The preview session has expired.");
      const session = createDemoSession(account);
      if (session.user.activeBranch.id !== branchId) {
        throw new Error("The preview workspace has only one branch.");
      }
      return session;
    },
  };
}
