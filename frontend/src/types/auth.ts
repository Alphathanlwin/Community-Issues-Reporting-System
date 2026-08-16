export type Role = "ADMIN" | "STAFF" | "CITIZEN";
export type AccountStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";

export interface AuthUser {
  userId: number;
  fullName: string;
  email: string;
  role: Role;
  departmentId: number | null;
  accountStatus: AccountStatus;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  fullName: string;
  email: string;
  role: Role;
  departmentId: number | null;
  accountStatus: AccountStatus;
}
