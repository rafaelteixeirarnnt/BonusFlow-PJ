export type AbsenceType = "VACATION" | "MEDICAL_LEAVE" | "PERSONAL_LEAVE" | "BONUS_DAY" | "OTHER";
export type AbsenceStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type UserRole = "SUPER_ADMIN" | "ADMIN" | "MANAGER" | "PROFESSIONAL" | "VIEWER";
export type ContactType = "RESIDENTIAL" | "MOBILE";
export type AuditAction =
  | "CHANGE_USER_EMAIL"
  | "USER_CREATED"
  | "USER_PROFESSIONAL_LINKED"
  | "USER_PROFESSIONAL_UNLINKED"
  | "USER_LOGIN_WITHOUT_PROFESSIONAL_BLOCKED"
  | "USER_ROLE_GRANT_BLOCKED"
  | "USER_EMAIL_CHANGED"
  | "USER_ROLE_CHANGED"
  | "USER_DEACTIVATED"
  | "USER_REACTIVATED"
  | "SUPER_ADMIN_DEACTIVATION_BLOCKED"
  | "PROFESSIONAL_DOCUMENT_CHANGED"
  | "PROFESSIONAL_DEACTIVATED"
  | "PROFESSIONAL_REACTIVATED"
  | "INSTITUTION_DEACTIVATED"
  | "INSTITUTION_REACTIVATED"
  | "ORGANIZATION_UNIT_MOVED";

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
  fullName: string;
  cpf?: string | null;
  birthDate?: string | null;
  motherName?: string | null;
  fatherName?: string | null;
  email: string;
  role: UserRole;
  active: boolean;
  professionalId?: number | null;
  professionalName?: string | null;
  contacts: UserContact[];
  address?: UserAddress | null;
  systemUser: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string | null;
};

export type UserContact = {
  type: ContactType | "";
  ddi: string;
  ddd: string;
  phone: string;
};

export type UserAddress = {
  zipCode: string;
  street: string;
  number: string;
  complement?: string | null;
  neighborhood: string;
  city: string;
  state: string;
};

export type DdiReference = {
  code: string;
  country: string;
};

export type DddReference = {
  ddd: string;
  state: string;
};

export type CepReference = {
  zipCode: string;
  street: string;
  neighborhood: string;
  city: string;
  state: string;
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

export type AuditLog = {
  id: number;
  entityName: string;
  entityId: number;
  action: AuditAction;
  previousValue?: string | null;
  newValue?: string | null;
  justification: string;
  performedByUserId: number;
  performedByUserName: string;
  performedAt: string;
  ipAddress?: string | null;
};

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
const TOKEN_KEY = "bonusflow.token";

let authToken = localStorage.getItem(TOKEN_KEY);
let tokenRenewalListener: ((token: string) => void) | null = null;

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

export function onTokenRenewed(listener: ((token: string) => void) | null) {
  tokenRenewalListener = listener;
}

export class ApiError extends Error {
  fieldErrors: Record<string, string[]>;

  constructor(message: string, fieldErrors: Record<string, string[]> = {}) {
    super(message);
    this.name = "ApiError";
    this.fieldErrors = fieldErrors;
  }
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
    throw new ApiError(payload?.message ?? "Nao foi possivel concluir a operacao.", payload?.fieldErrors ?? {});
  }

  const renewedToken = response.headers.get("X-Renewed-Token");
  if (renewedToken) {
    setAuthToken(renewedToken);
    tokenRenewalListener?.(renewedToken);
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
  ddis: () => request<DdiReference[]>("/reference/ddis"),
  ddds: () => request<DddReference[]>("/reference/ddds"),
  cep: (cep: string) => request<CepReference>(`/reference/cep/${cep.replace(/\D/g, "")}`),
  rules: () => request<ContractRule[]>("/contract-rules"),
  requests: () => request<AbsenceRequest[]>("/absence-requests"),
  report: (month: string, professionalId?: string, absenceType?: string) => {
    const params = new URLSearchParams({ month });
    if (professionalId) params.set("professionalId", professionalId);
    if (absenceType) params.set("absenceType", absenceType);
    return request<AbsenceRequest[]>(`/absence-requests/report?${params}`);
  },
  auditLogs: (filters: {
    entityName?: string;
    action?: string;
    performedByUserId?: string;
    startAt?: string;
    endAt?: string;
  }) => {
    const params = new URLSearchParams();
    if (filters.entityName) params.set("entityName", filters.entityName);
    if (filters.action) params.set("action", filters.action);
    if (filters.performedByUserId) params.set("performedByUserId", filters.performedByUserId);
    if (filters.startAt) params.set("startAt", `${filters.startAt}T00:00:00Z`);
    if (filters.endAt) params.set("endAt", `${filters.endAt}T23:59:59Z`);
    return request<AuditLog[]>(`/audit-logs/search?${params}`);
  },
  auditLog: (id: number) => request<AuditLog>(`/audit-logs/${id}`),
  createProfessional: (body: Omit<Professional, "id" | "createdAt" | "updatedAt">) =>
    request<Professional>("/professionals", { method: "POST", body: JSON.stringify(body) }),
  createUser: (body: {
    fullName: string;
    cpf: string;
    birthDate: string;
    motherName: string;
    fatherName?: string;
    email: string;
    role: UserRole;
    professionalId?: number | null;
    contacts: UserContact[];
    address: UserAddress;
  }) =>
    request<AppUser>("/users", { method: "POST", body: JSON.stringify(body) }),
  updateUser: (id: number, body: {
    fullName: string;
    cpf: string;
    birthDate: string;
    motherName: string;
    fatherName?: string;
    email: string;
    role: UserRole;
    professionalId?: number | null;
    active: boolean;
    contacts: UserContact[];
    address: UserAddress;
    justification?: string;
  }) => request<AppUser>(`/users/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  deactivateUser: (id: number, justification: string) =>
    request<AppUser>(`/users/${id}/deactivate`, { method: "PATCH", body: JSON.stringify({ justification }) }),
  activateUser: (id: number, justification: string) =>
    request<AppUser>(`/users/${id}/activate`, { method: "PATCH", body: JSON.stringify({ justification }) }),
  linkProfessional: (id: number, professionalId: number) =>
    request<AppUser>(`/users/${id}/link-professional`, { method: "PATCH", body: JSON.stringify({ professionalId }) }),
  unlinkProfessional: (id: number) =>
    request<AppUser>(`/users/${id}/unlink-professional`, { method: "PATCH" }),
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

export const auditActionLabels: Record<AuditAction, string> = {
  CHANGE_USER_EMAIL: "Alteracao de e-mail de usuario",
  USER_CREATED: "Criacao de usuario",
  USER_PROFESSIONAL_LINKED: "Vinculo de profissional ao usuario",
  USER_PROFESSIONAL_UNLINKED: "Remocao de vinculo de profissional",
  USER_LOGIN_WITHOUT_PROFESSIONAL_BLOCKED: "Login bloqueado sem profissional vinculado",
  USER_ROLE_GRANT_BLOCKED: "Tentativa bloqueada de conceder perfil superior",
  USER_EMAIL_CHANGED: "Alteracao de e-mail de usuario",
  USER_ROLE_CHANGED: "Alteracao de perfil de usuario",
  USER_DEACTIVATED: "Inativacao de usuario",
  USER_REACTIVATED: "Reativacao de usuario",
  SUPER_ADMIN_DEACTIVATION_BLOCKED: "Tentativa bloqueada de inativar SUPER_ADMIN",
  PROFESSIONAL_DOCUMENT_CHANGED: "Alteracao de documento de profissional",
  PROFESSIONAL_DEACTIVATED: "Inativacao de profissional",
  PROFESSIONAL_REACTIVATED: "Reativacao de profissional",
  INSTITUTION_DEACTIVATED: "Inativacao de instituicao",
  INSTITUTION_REACTIVATED: "Reativacao de instituicao",
  ORGANIZATION_UNIT_MOVED: "Movimentacao de unidade organizacional"
};
