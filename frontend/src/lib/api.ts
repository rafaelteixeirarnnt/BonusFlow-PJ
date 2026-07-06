export type AbsenceType = "VACATION" | "MEDICAL_LEAVE" | "PERSONAL_LEAVE" | "BONUS_DAY" | "OTHER";
export type AbsenceStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type UserRole = "SUPER_ADMIN" | "ADMIN" | "MANAGER" | "PROFESSIONAL" | "VIEWER";

export type Professional = {
  id: number;
  name: string;
  email: string;
  document: string;
  team: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AppUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  professionalId?: number | null;
  professionalName?: string | null;
  systemUser: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string | null;
};

export type ContractRule = {
  id: number;
  professionalId: number;
  professionalName: string;
  absenceType: AbsenceType;
  daysAllowed: number;
  validFrom: string;
  validTo?: string | null;
};

export type AbsenceRequest = {
  id: number;
  professionalId: number;
  professionalName: string;
  createdById: number;
  createdByName: string;
  absenceType: AbsenceType;
  startDate: string;
  endDate: string;
  requestedDays: number;
  status: AbsenceStatus;
  reason?: string | null;
  createdAt: string;
};

export type Dashboard = {
  professionals: number;
  pendingRequests: number;
  approvedDays: number;
};

export type AuthUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  professionalId?: number | null;
  systemUser: boolean;
  lastLoginAt?: string | null;
};

export type LoginResponse = {
  token: string;
  user: AuthUser;
};

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "bonusflow.token";

let authToken = localStorage.getItem(TOKEN_KEY);

export function setAuthToken(token: string | null) {
  authToken = token;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getAuthToken() {
  return authToken;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message ?? "Nao foi possivel concluir a operacao.");
  }

  return response.json();
}

export const api = {
  login: (body: { email: string; password: string }) =>
    request<LoginResponse>("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  health: () => request<{ status: string }>("/health"),
  dashboard: () => request<Dashboard>("/dashboard"),
  professionals: () => request<Professional[]>("/professionals"),
  users: () => request<AppUser[]>("/users"),
  rules: () => request<ContractRule[]>("/contract-rules"),
  requests: () => request<AbsenceRequest[]>("/absence-requests"),
  report: (month: string, professionalId?: string, absenceType?: string) => {
    const params = new URLSearchParams({ month });
    if (professionalId) params.set("professionalId", professionalId);
    if (absenceType) params.set("absenceType", absenceType);
    return request<AbsenceRequest[]>(`/absence-requests/report?${params}`);
  },
  createProfessional: (body: Omit<Professional, "id" | "createdAt" | "updatedAt">) =>
    request<Professional>("/professionals", { method: "POST", body: JSON.stringify(body) }),
  createUser: (body: {
    name: string;
    email: string;
    password?: string;
    role: UserRole;
    professionalId?: number | null;
    active: boolean;
  }) =>
    request<AppUser>("/users", { method: "POST", body: JSON.stringify(body) }),
  updateUser: (id: number, body: {
    name: string;
    email: string;
    role: UserRole;
    professionalId?: number | null;
    active: boolean;
  }) => request<AppUser>(`/users/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  deactivateUser: (id: number) => request<AppUser>(`/users/${id}/deactivate`, { method: "PATCH" }),
  activateUser: (id: number) => request<AppUser>(`/users/${id}/activate`, { method: "PATCH" }),
  createRule: (body: {
    professionalId: number;
    absenceType: AbsenceType;
    daysAllowed: number;
    validFrom: string;
    validTo?: string;
  }) => request<ContractRule>("/contract-rules", { method: "POST", body: JSON.stringify(body) }),
  createAbsence: (body: {
    professionalId: number;
    createdById: number;
    absenceType: AbsenceType;
    startDate: string;
    endDate: string;
    reason?: string;
  }) => request<AbsenceRequest>("/absence-requests", { method: "POST", body: JSON.stringify(body) }),
  transition: (id: number, action: "approve" | "reject" | "cancel", userId: number, comment?: string) =>
    request<AbsenceRequest>(`/absence-requests/${id}/${action}`, {
      method: "PATCH",
      body: JSON.stringify({ userId, comment })
    })
};

export const absenceTypeLabels: Record<AbsenceType, string> = {
  VACATION: "Ferias",
  MEDICAL_LEAVE: "Afastamento medico",
  PERSONAL_LEAVE: "Afastamento pessoal",
  BONUS_DAY: "Dia bonus",
  OTHER: "Outro"
};

export const statusLabels: Record<AbsenceStatus, string> = {
  PENDING: "Pendente",
  APPROVED: "Aprovado",
  REJECTED: "Reprovado",
  CANCELLED: "Cancelado"
};

export const roleLabels: Record<UserRole, string> = {
  SUPER_ADMIN: "Super Admin",
  ADMIN: "Admin",
  MANAGER: "Gestor",
  PROFESSIONAL: "Profissional",
  VIEWER: "Consulta"
};
