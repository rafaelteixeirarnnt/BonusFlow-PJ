export type AbsenceType = "VACATION" | "MEDICAL_LEAVE" | "PERSONAL_LEAVE" | "BONUS_DAY" | "OTHER";
export type AbsenceStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type UserRole = "ADMIN" | "MANAGER" | "VIEWER";

export type Professional = {
  id: number;
  name: string;
  email: string;
  document: string;
  team: string;
  active: boolean;
};

export type AppUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
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

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
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
  createProfessional: (body: Omit<Professional, "id">) =>
    request<Professional>("/professionals", { method: "POST", body: JSON.stringify(body) }),
  createUser: (body: Omit<AppUser, "id">) =>
    request<AppUser>("/users", { method: "POST", body: JSON.stringify(body) }),
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
