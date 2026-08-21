import type {
  Permission,
  TenantRole,
} from "@/features/auth/access-control";

export interface ActiveBranch {
  id: string;
  code: string;
  name: string;
}

export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  pharmacyId: string;
  pharmacyName: string;
  activeBranch: ActiveBranch;
  roles: TenantRole[];
  permissions: Permission[];
  featureFlags: Record<string, boolean>;
}

export interface AuthSession {
  expiresAt: string;
  user: AuthUser;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AuthGateway {
  login(credentials: LoginCredentials): Promise<AuthSession>;
  logout(): Promise<void>;
  restore(): Promise<AuthSession | null>;
  switchBranch(branchId: string): Promise<AuthSession>;
}
