import {
  CalendarCheck,
  ClipboardList,
  FileBarChart,
  ChevronLeft,
  ChevronRight,
  LayoutDashboard,
  LogOut,
  LucideIcon,
  LockKeyhole,
  Pencil,
  Plus,
  Scale,
  ShieldCheck,
  UserCog,
  Users
} from "lucide-react";
import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import {
  AbsenceRequest,
  AbsenceType,
  ApiError,
  AppUser,
  AuditLog,
  AuthUser,
  ContactType,
  ContractRule,
  DddReference,
  DdiReference,
  Dashboard,
  Professional,
  UserAddress,
  UserContact,
  auditActionLabels,
  absenceTypeLabels,
  api,
  getAuthToken,
  onTokenRenewed,
  roleLabels,
  setAuthToken,
  statusLabels
} from "./lib/api";

type View = "dashboard" | "professionals" | "rules" | "requests" | "approvals" | "reports" | "userSearch" | "userCreate" | "audit";

const absenceTypes = Object.keys(absenceTypeLabels) as AbsenceType[];
const auditActions = Object.keys(auditActionLabels) as Array<AuditLog["action"]>;

const navItems: Array<{ id: View; label: string; icon: LucideIcon }> = [
  { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
  { id: "professionals", label: "Profissionais", icon: Users },
  { id: "rules", label: "Regras", icon: Scale },
  { id: "requests", label: "Solicitacoes", icon: ClipboardList },
  { id: "approvals", label: "Aprovacoes", icon: CalendarCheck },
  { id: "reports", label: "Relatorios", icon: FileBarChart },
  { id: "audit", label: "Auditoria", icon: ShieldCheck }
];

const viewTitles: Record<View, string> = {
  dashboard: "Dashboard",
  professionals: "Profissionais",
  rules: "Regras",
  requests: "Solicitacoes",
  approvals: "Aprovacoes",
  reports: "Relatorios",
  userSearch: "Pesquisar usuarios",
  userCreate: "Cadastrar usuario",
  audit: "Auditoria"
};

const fallbackDdiOptions: DdiReference[] = [
  { code: "+1", country: "Estados Unidos/Canadá" },
  { code: "+7", country: "Rússia/Cazaquistão" },
  { code: "+20", country: "Egito" },
  { code: "+27", country: "África do Sul" },
  { code: "+30", country: "Grécia" },
  { code: "+33", country: "França" },
  { code: "+34", country: "Espanha" },
  { code: "+39", country: "Itália" },
  { code: "+44", country: "Reino Unido" },
  { code: "+49", country: "Alemanha" },
  { code: "+52", country: "México" },
  { code: "+54", country: "Argentina" },
  { code: "+55", country: "Brasil" },
  { code: "+56", country: "Chile" },
  { code: "+57", country: "Colômbia" },
  { code: "+351", country: "Portugal" },
  { code: "+598", country: "Uruguai" }
];

const fallbackDddOptions: DddReference[] = [
  "11-SP", "12-SP", "13-SP", "14-SP", "15-SP", "16-SP", "17-SP", "18-SP", "19-SP",
  "21-RJ", "22-RJ", "24-RJ", "27-ES", "28-ES", "31-MG", "32-MG", "33-MG", "34-MG",
  "35-MG", "37-MG", "38-MG", "41-PR", "42-PR", "43-PR", "44-PR", "45-PR", "46-PR",
  "47-SC", "48-SC", "49-SC", "51-RS", "53-RS", "54-RS", "55-RS", "61-DF", "62-GO",
  "63-TO", "64-GO", "65-MT", "66-MT", "67-MS", "68-AC", "69-RO", "71-BA", "73-BA",
  "74-BA", "75-BA", "77-BA", "79-SE", "81-PE", "82-AL", "83-PB", "84-RN", "85-CE",
  "86-PI", "87-PE", "88-CE", "89-PI", "91-PA", "92-AM", "93-PA", "94-PA", "95-RR",
  "96-AP", "97-AM", "98-MA", "99-MA"
].map((item) => {
  const [ddd, state] = item.split("-");
  return { ddd, state };
});

type FieldErrors = Record<string, string[]>;

type UserFormState = {
  fullName: string;
  cpf: string;
  birthDate: string;
  motherName: string;
  fatherName: string;
  email: string;
  role: AppUser["role"];
  active: boolean;
  contacts: UserContact[];
  address: UserAddress;
};

const emptyAddress: UserAddress = {
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: ""
};

function emptyUserForm(role: AppUser["role"]): UserFormState {
  return {
    fullName: "",
    cpf: "",
    birthDate: "",
    motherName: "",
    fatherName: "",
    email: "",
    role,
    active: true,
    contacts: [{ type: "MOBILE", ddi: "+55", ddd: "", phone: "" }],
    address: { ...emptyAddress }
  };
}

export function App() {
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [view, setView] = useState<View>("dashboard");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [users, setUsers] = useState<AppUser[]>([]);
  const [rules, setRules] = useState<ContractRule[]>([]);
  const [requests, setRequests] = useState<AbsenceRequest[]>([]);
  const [reportRows, setReportRows] = useState<AbsenceRequest[]>([]);
  const [auditRows, setAuditRows] = useState<AuditLog[]>([]);
  const [userSearchNotice, setUserSearchNotice] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [sessionMessage, setSessionMessage] = useState("");
  const [sessionExpiresAt, setSessionExpiresAt] = useState(() => tokenExpiration(getAuthToken()));
  const [now, setNow] = useState(Date.now());

  const pendingRequests = useMemo(() => requests.filter((request) => request.status === "PENDING"), [requests]);

  async function refresh() {
    const [dashboardData, professionalsData, usersData, rulesData, requestsData] = await Promise.all([
      api.dashboard(),
      api.professionals(),
      api.users(),
      api.rules(),
      api.requests()
    ]);
    setDashboard(dashboardData);
    setProfessionals(professionalsData);
    setUsers(usersData);
    setRules(rulesData);
    setRequests(requestsData);
    const userFromToken = resolveCurrentUser(usersData);
    if (userFromToken) {
      setCurrentUser(userFromToken);
    }
  }

  async function run(action: () => Promise<unknown>, message: string) {
    setError("");
    setNotice("");
    try {
      await action();
      await refresh();
      setNotice(message);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Nao foi possivel concluir a operacao.");
    }
  }

  useEffect(() => {
    onTokenRenewed((token) => setSessionExpiresAt(tokenExpiration(token)));
    if (getAuthToken()) {
      refresh().catch((err) => {
        setAuthToken(null);
        setSessionExpiresAt(null);
        setError(err instanceof Error ? err.message : "Sessao expirada.");
      });
    }
    return () => onTokenRenewed(null);
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!sessionExpiresAt || !getAuthToken()) return;
    if (sessionExpiresAt <= now) {
      logout("Sua sessão expirou. Faça login novamente.");
    }
  }, [now, sessionExpiresAt]);

  async function login(email: string, password: string) {
    setError("");
    const response = await api.login({ email, password });
    setAuthToken(response.token);
    setSessionExpiresAt(tokenExpiration(response.token));
    setSessionMessage("");
    setCurrentUser(response.user);
    await refresh();
  }

  function logout(message = "") {
    setAuthToken(null);
    setSessionExpiresAt(null);
    setSessionMessage(message);
    setCurrentUser(null);
    setDashboard(null);
    setProfessionals([]);
    setUsers([]);
    setRules([]);
    setRequests([]);
    setReportRows([]);
    setAuditRows([]);
  }

  function navigate(nextView: View) {
    setNotice("");
    setError("");
    setUserSearchNotice("");
    setView(nextView);
  }

  async function createUserAndReturnToSearch(body: Parameters<typeof api.createUser>[0]) {
    await api.createUser(body);
    await refresh();
    setNotice("");
    setError("");
    setUserSearchNotice("Usuário cadastrado com sucesso.");
    setView("userSearch");
  }

  if (!getAuthToken()) {
    return <LoginPage error={error || sessionMessage} onLogin={(email, password) => login(email, password).catch((err) => setError(err instanceof Error ? err.message : "Login invalido."))} />;
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">BF</div>
          <div>
            <strong>BonusFlow PJ</strong>
            <span>Gestao contratual</span>
          </div>
        </div>
        <nav className="nav">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <button className={view === item.id ? "active" : ""} key={item.id} onClick={() => navigate(item.id)}>
                <Icon size={18} />
                {item.label}
              </button>
            );
          })}
          <div className="nav-group">
            <span><UserCog size={18} /> Usuarios</span>
            <button className={view === "userSearch" ? "active" : ""} onClick={() => navigate("userSearch")}>Pesquisar</button>
            <button className={view === "userCreate" ? "active" : ""} onClick={() => navigate("userCreate")}>Cadastrar</button>
          </div>
        </nav>
        <button className="logout-button" onClick={() => logout()}>
          <LogOut size={18} />
          Sair
        </button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <h1>{viewTitles[view]}</h1>
            <p>{currentUser ? `${currentUser.name} · ${roleLabels[currentUser.role]}` : "Controle de bonificacoes, afastamentos e saldo contratual para profissionais PJ."}</p>
          </div>
          <div className="topbar-actions">
            <span className="session-countdown">Sessao: {formatSessionRemaining(sessionExpiresAt, now)}</span>
            <button className="icon-button" onClick={() => refresh()} title="Atualizar dados">
              <Plus size={18} />
              Atualizar
            </button>
          </div>
        </header>

        {notice && <div className="notice success">{notice}</div>}
        {error && <div className="notice error">{error}</div>}

        {view === "dashboard" && <DashboardPage dashboard={dashboard} requests={requests} />}
        {view === "professionals" && (
          <ProfessionalsPage professionals={professionals} onCreate={(body) => run(() => api.createProfessional(body), "Profissional cadastrado.")} />
        )}
        {view === "rules" && (
          <RulesPage professionals={professionals} rules={rules} onCreate={(body) => run(() => api.createRule(body), "Regra cadastrada.")} />
        )}
        {view === "requests" && (
          <RequestsPage professionals={professionals} users={users} requests={requests} onCreate={(body) => run(() => api.createAbsence(body), "Solicitacao registrada.")} />
        )}
        {view === "approvals" && (
          <ApprovalsPage
            users={users}
            requests={pendingRequests}
            onTransition={(id, action, userId, comment) => run(() => api.transition(id, action, userId, comment), "Status atualizado.")}
          />
        )}
        {view === "reports" && (
          <ReportsPage
            professionals={professionals}
            rows={reportRows}
            onSearch={async (month, professionalId, absenceType) => {
              setError("");
              try {
                setReportRows(await api.report(month, professionalId, absenceType));
              } catch (err) {
                setError(err instanceof Error ? err.message : "Nao foi possivel gerar o relatorio.");
              }
            }}
          />
        )}
        {view === "userSearch" && (
          <UserSearchPage
            professionals={professionals}
            currentUser={currentUser}
            users={users}
            onNew={() => navigate("userCreate")}
            onUpdate={async (id, body) => { await api.updateUser(id, body); await refresh(); }}
            onActivate={async (id, justification) => { await api.activateUser(id, justification); await refresh(); }}
            onDeactivate={async (id, justification) => { await api.deactivateUser(id, justification); await refresh(); }}
            onLinkProfessional={async (id, professionalId) => { await api.linkProfessional(id, professionalId); await refresh(); }}
            onUnlinkProfessional={async (id) => { await api.unlinkProfessional(id); await refresh(); }}
            successNotice={userSearchNotice}
            onSuccessNoticeShown={() => setUserSearchNotice("")}
          />
        )}
        {view === "userCreate" && (
          <UserCreatePage
            currentUser={currentUser}
            onSearch={() => navigate("userSearch")}
            onCreate={createUserAndReturnToSearch}
          />
        )}
        {view === "audit" && (
          <AuditPage
            users={users}
            rows={auditRows}
            onSearch={async (filters) => {
              setError("");
              try {
                setAuditRows(await api.auditLogs(filters));
              } catch (err) {
                setError(err instanceof Error ? err.message : "Nao foi possivel consultar a auditoria.");
              }
            }}
          />
        )}
      </section>
    </main>
  );
}

function LoginPage({ error, onLogin }: { error: string; onLogin: (email: string, password: string) => void }) {
  const [email, setEmail] = useState("admin@bonusflow.com");
  const [password, setPassword] = useState("Admin@123");
  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="brand login-brand">
          <div className="brand-mark">BF</div>
          <div>
            <strong>BonusFlow PJ</strong>
            <span>Acesso seguro</span>
          </div>
        </div>
        <form className="form" onSubmit={(event) => submit(event, () => onLogin(email, password))}>
          <Input label="E-mail" value={email} onChange={setEmail} />
          <Input label="Senha" type="password" value={password} onChange={setPassword} />
          <button className="primary login-submit" type="submit"><LockKeyhole size={18} /> Entrar</button>
        </form>
        {error && <div className="notice error">{error}</div>}
      </section>
    </main>
  );
}

function DashboardPage({ dashboard, requests }: { dashboard: Dashboard | null; requests: AbsenceRequest[] }) {
  const approved = requests.filter((request) => request.status === "APPROVED").slice(0, 5);
  return (
    <div className="stack">
      <div className="metric-grid">
        <Metric label="Profissionais" value={dashboard?.professionals ?? 0} />
        <Metric label="Pendentes" value={dashboard?.pendingRequests ?? 0} />
        <Metric label="Dias aprovados" value={dashboard?.approvedDays ?? 0} />
      </div>
      <Panel title="Ultimos lancamentos aprovados">
        <RequestsTable rows={approved} />
      </Panel>
    </div>
  );
}

function ProfessionalsPage({ professionals, onCreate }: { professionals: Professional[]; onCreate: (body: Omit<Professional, "id" | "createdAt" | "updatedAt">) => void }) {
  const [form, setForm] = useState({ name: "", email: "", document: "", team: "", active: true });
  return (
    <TwoColumn>
      <Panel title="Cadastrar profissional">
        <form className="form" onSubmit={(event) => submit(event, () => onCreate(form))}>
          <Input label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} />
          <Input label="Email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
          <Input label="Documento" value={form.document} onChange={(document) => setForm({ ...form, document })} />
          <Input label="Equipe" value={form.team} onChange={(team) => setForm({ ...form, team })} />
          <label className="check"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Ativo</label>
          <SubmitButton label="Cadastrar" />
        </form>
      </Panel>
      <Panel title="Profissionais cadastrados">
        <table>
          <thead><tr><th>Nome</th><th>Equipe</th><th>Email</th><th>Status</th></tr></thead>
          <tbody>{professionals.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.team}</td><td>{item.email}</td><td><StatusBadge label={item.active ? "Ativo" : "Inativo"} tone={item.active ? "good" : "muted"} /></td></tr>)}</tbody>
        </table>
      </Panel>
    </TwoColumn>
  );
}

function RulesPage({ professionals, rules, onCreate }: { professionals: Professional[]; rules: ContractRule[]; onCreate: (body: Parameters<typeof api.createRule>[0]) => void }) {
  const [form, setForm] = useState({ professionalId: "", absenceType: "VACATION" as AbsenceType, daysAllowed: 0, validFrom: "", validTo: "" });
  return (
    <TwoColumn>
      <Panel title="Cadastrar regra contratual">
        <form className="form" onSubmit={(event) => submit(event, () => onCreate({ ...form, professionalId: Number(form.professionalId), validTo: form.validTo || undefined }))}>
          <Select label="Profissional" value={form.professionalId} onChange={(professionalId) => setForm({ ...form, professionalId })} options={professionals.map((item) => ({ value: String(item.id), label: item.name }))} />
          <Select label="Tipo" value={form.absenceType} onChange={(absenceType) => setForm({ ...form, absenceType: absenceType as AbsenceType })} options={absenceTypes.map((type) => ({ value: type, label: absenceTypeLabels[type] }))} />
          <Input label="Dias previstos" type="number" value={String(form.daysAllowed)} onChange={(daysAllowed) => setForm({ ...form, daysAllowed: Number(daysAllowed) })} />
          <Input label="Inicio da vigencia" type="date" value={form.validFrom} onChange={(validFrom) => setForm({ ...form, validFrom })} />
          <Input label="Fim da vigencia" type="date" value={form.validTo} onChange={(validTo) => setForm({ ...form, validTo })} />
          <SubmitButton label="Salvar regra" />
        </form>
      </Panel>
      <Panel title="Regras cadastradas">
        <table>
          <thead><tr><th>Profissional</th><th>Tipo</th><th>Dias</th><th>Vigencia</th></tr></thead>
          <tbody>{rules.map((item) => <tr key={item.id}><td>{item.professionalName}</td><td>{absenceTypeLabels[item.absenceType]}</td><td>{item.daysAllowed}</td><td>{item.validFrom} ate {item.validTo ?? "aberto"}</td></tr>)}</tbody>
        </table>
      </Panel>
    </TwoColumn>
  );
}

function RequestsPage({ professionals, users, requests, onCreate }: { professionals: Professional[]; users: AppUser[]; requests: AbsenceRequest[]; onCreate: (body: Parameters<typeof api.createAbsence>[0]) => void }) {
  const [form, setForm] = useState({ professionalId: "", createdById: "", absenceType: "VACATION" as AbsenceType, startDate: "", endDate: "", reason: "" });
  return (
    <TwoColumn>
      <Panel title="Nova solicitacao">
        <form className="form" onSubmit={(event) => submit(event, () => onCreate({ ...form, professionalId: Number(form.professionalId), createdById: Number(form.createdById) }))}>
          <Select label="Profissional" value={form.professionalId} onChange={(professionalId) => setForm({ ...form, professionalId })} options={professionals.map((item) => ({ value: String(item.id), label: item.name }))} />
          <Select label="Responsavel" value={form.createdById} onChange={(createdById) => setForm({ ...form, createdById })} options={users.map((item) => ({ value: String(item.id), label: `${item.name} (${item.role})` }))} />
          <Select label="Tipo" value={form.absenceType} onChange={(absenceType) => setForm({ ...form, absenceType: absenceType as AbsenceType })} options={absenceTypes.map((type) => ({ value: type, label: absenceTypeLabels[type] }))} />
          <Input label="Inicio" type="date" value={form.startDate} onChange={(startDate) => setForm({ ...form, startDate })} />
          <Input label="Fim" type="date" value={form.endDate} onChange={(endDate) => setForm({ ...form, endDate })} />
          <Input label="Motivo" value={form.reason} onChange={(reason) => setForm({ ...form, reason })} />
          <SubmitButton label="Registrar" />
        </form>
      </Panel>
      <Panel title="Solicitacoes">
        <RequestsTable rows={requests} />
      </Panel>
    </TwoColumn>
  );
}

function ApprovalsPage({ users, requests, onTransition }: { users: AppUser[]; requests: AbsenceRequest[]; onTransition: (id: number, action: "approve" | "reject" | "cancel", userId: number, comment?: string) => void }) {
  const [userId, setUserId] = useState("");
  const [comment, setComment] = useState("");
  return (
    <Panel title="Fila de aprovacao">
      <div className="approval-toolbar">
        <Select label="Aprovador" value={userId} onChange={setUserId} options={users.map((item) => ({ value: String(item.id), label: item.name }))} />
        <Input label="Comentario" value={comment} onChange={setComment} />
      </div>
      <table>
        <thead><tr><th>Profissional</th><th>Tipo</th><th>Periodo</th><th>Dias</th><th>Acoes</th></tr></thead>
        <tbody>
          {requests.map((item) => (
            <tr key={item.id}>
              <td>{item.professionalName}</td>
              <td>{absenceTypeLabels[item.absenceType]}</td>
              <td>{item.startDate} ate {item.endDate}</td>
              <td>{item.requestedDays}</td>
              <td className="actions">
                <button onClick={() => onTransition(item.id, "approve", Number(userId), comment)}>Aprovar</button>
                <button onClick={() => onTransition(item.id, "reject", Number(userId), comment)}>Reprovar</button>
                <button onClick={() => onTransition(item.id, "cancel", Number(userId), comment)}>Cancelar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Panel>
  );
}

function ReportsPage({ professionals, rows, onSearch }: { professionals: Professional[]; rows: AbsenceRequest[]; onSearch: (month: string, professionalId?: string, absenceType?: string) => void }) {
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7));
  const [professionalId, setProfessionalId] = useState("");
  const [absenceType, setAbsenceType] = useState("");
  return (
    <div className="stack">
      <Panel title="Filtros">
        <form className="report-form" onSubmit={(event) => submit(event, () => onSearch(month, professionalId, absenceType))}>
          <Input label="Mes" type="month" value={month} onChange={setMonth} />
          <Select label="Profissional" value={professionalId} onChange={setProfessionalId} options={professionals.map((item) => ({ value: String(item.id), label: item.name }))} optionalLabel="Todos" />
          <Select label="Tipo" value={absenceType} onChange={setAbsenceType} options={absenceTypes.map((type) => ({ value: type, label: absenceTypeLabels[type] }))} optionalLabel="Todos" />
          <SubmitButton label="Gerar relatorio" />
        </form>
      </Panel>
      <Panel title="Resultado">
        <RequestsTable rows={rows} />
      </Panel>
    </div>
  );
}

function UserSearchPage({
  professionals,
  currentUser,
  users,
  onNew,
  onUpdate,
  onActivate,
  onDeactivate,
  onLinkProfessional,
  onUnlinkProfessional,
  successNotice,
  onSuccessNoticeShown
}: {
  professionals: Professional[];
  currentUser: AuthUser | null;
  users: AppUser[];
  onNew: () => void;
  onUpdate: (id: number, body: Parameters<typeof api.updateUser>[1]) => Promise<void>;
  onActivate: (id: number, justification: string) => Promise<void>;
  onDeactivate: (id: number, justification: string) => Promise<void>;
  onLinkProfessional: (id: number, professionalId: number) => Promise<void>;
  onUnlinkProfessional: (id: number) => Promise<void>;
  successNotice: string;
  onSuccessNoticeShown: () => void;
}) {
  const emptyFilters = { name: "", email: "", role: "", status: "", professionalId: "", institution: "", board: "", area: "" };
  const [filters, setFilters] = useState({ name: "", email: "", role: "", status: "", professionalId: "", institution: "", board: "", area: "" });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [editing, setEditing] = useState<AppUser | null>(null);
  const [linking, setLinking] = useState<AppUser | null>(null);
  const [editForm, setEditForm] = useState<UserFormState>(() => emptyUserForm("VIEWER"));
  const [editFieldErrors, setEditFieldErrors] = useState<FieldErrors>({});
  const [justificationAction, setJustificationAction] = useState<null | { onConfirm: (justification: string) => void }>(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const allowedRoles = allowedRoleOptions(currentUser?.role);

  useEffect(() => {
    if (!successNotice) return;
    setFilters(emptyFilters);
    setPage(1);
    setNotice(successNotice);
    setError("");
    onSuccessNoticeShown();
  }, [successNotice]);

  const professionalById = useMemo(() => new Map(professionals.map((item) => [item.id, item])), [professionals]);
  const activeLinkedProfessionalIds = useMemo(() => new Set(users.filter((item) => item.active && item.professionalId).map((item) => item.professionalId as number)), [users]);
  const linkableProfessionals = useMemo(() => professionals.filter((item) => item.active && !activeLinkedProfessionalIds.has(item.id)), [activeLinkedProfessionalIds, professionals]);
  const filteredUsers = useMemo(() => users
    .filter((item) => {
      const professional = item.professionalId ? professionalById.get(item.professionalId) : null;
      const matchesName = item.name.toLowerCase().includes(filters.name.toLowerCase());
      const matchesEmail = item.email.toLowerCase().includes(filters.email.toLowerCase());
      const matchesRole = !filters.role || item.role === filters.role;
      const matchesStatus = !filters.status || (filters.status === "active" ? item.active : !item.active);
      const matchesProfessional = !filters.professionalId || String(item.professionalId ?? "") === filters.professionalId;
      const matchesArea = !filters.area || professional?.team.toLowerCase().includes(filters.area.toLowerCase());
      return matchesName && matchesEmail && matchesRole && matchesStatus && matchesProfessional && matchesArea;
    })
    .sort((first, second) => second.id - first.id), [filters, professionalById, users]);
  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const pageRows = filteredUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  function openEdit(user: AppUser) {
    setNotice("");
    setError("");
    setEditFieldErrors({});
    setEditing(user);
    setEditForm({
      fullName: user.fullName ?? user.name,
      cpf: formatCpf(user.cpf ?? ""),
      birthDate: user.birthDate ?? "",
      motherName: user.motherName ?? "",
      fatherName: user.fatherName ?? "",
      email: user.email,
      role: user.role,
      active: user.active,
      contacts: user.contacts?.length ? user.contacts : [{ type: "MOBILE", ddi: "+55", ddd: "", phone: "" }],
      address: user.address ?? { ...emptyAddress }
    });
  }

  function closeEdit() {
    setEditing(null);
  }

  function updateFilter(nextFilter: Partial<typeof filters>) {
    setNotice("");
    setError("");
    setFilters({ ...filters, ...nextFilter });
    setPage(1);
  }

  function updatePage(nextPage: number) {
    setNotice("");
    setError("");
    setPage(nextPage);
  }

  function updatePageSize(nextPageSize: number) {
    setNotice("");
    setError("");
    setPageSize(nextPageSize);
    setPage(1);
  }

  function saveEdit() {
    if (!editing) return;
    const emailChanged = editForm.email.trim().toLowerCase() !== editing.email.toLowerCase();
    const cpfChanged = digitsOnly(editForm.cpf) !== (editing.cpf ?? "");
    const roleChanged = editForm.role !== editing.role;
    const clientErrors = validateUserForm(editForm, true);
    setEditFieldErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) return;
    const body = {
      fullName: editForm.fullName,
      cpf: digitsOnly(editForm.cpf),
      birthDate: editForm.birthDate,
      motherName: editForm.motherName,
      fatherName: editForm.fatherName,
      email: currentUser?.role === "SUPER_ADMIN" ? editForm.email : editing.email,
      role: editForm.role,
      professionalId: editing.professionalId ?? null,
      active: editForm.active,
      contacts: editForm.contacts.map(toContactPayload),
      address: { ...editForm.address, zipCode: digitsOnly(editForm.address.zipCode) }
    };
    if (emailChanged || roleChanged || cpfChanged) {
      setJustificationAction({
        onConfirm: (justification) => {
          runUserAction(() => onUpdate(editing.id, { ...body, justification }), "Usuario atualizado.");
          closeEdit();
        }
      });
      return;
    }
    runUserAction(() => onUpdate(editing.id, body), "Usuario atualizado.");
    closeEdit();
  }

  function statusChange(id: number, action: "activate" | "deactivate") {
    setJustificationAction({
      onConfirm: (justification) => {
        if (action === "activate") {
          runUserAction(() => onActivate(id, justification), "Usuario ativado.");
        } else {
          runUserAction(() => onDeactivate(id, justification), "Usuario inativado.");
        }
      }
    });
  }

  function openLink(user: AppUser) {
    setNotice("");
    setError("");
    setLinking(user);
  }

  function linkProfessional(professionalId: number) {
    if (!linking) return;
    runUserAction(() => onLinkProfessional(linking.id, professionalId), "Profissional vinculado ao usuario.");
    setLinking(null);
  }

  function unlinkProfessional(user: AppUser) {
    runUserAction(() => onUnlinkProfessional(user.id), "Vinculo removido do usuario.");
  }

  async function runUserAction(action: () => Promise<void>, successMessage: string) {
    setNotice("");
    setError("");
    try {
      await action();
      setNotice(successMessage);
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setEditFieldErrors(err.fieldErrors);
        setError("");
      } else {
        setError(err instanceof Error ? err.message : "Nao foi possivel concluir a operacao.");
      }
    }
  }

  return (
    <div className="stack">
      {notice && <div className="notice success">{notice}</div>}
      {error && <div className="notice error">{error}</div>}

      <Panel title="Pesquisar usuarios">
        <div className="panel-header-actions">
          <button className="primary" onClick={() => { setNotice(""); setError(""); onNew(); }}><Plus size={16} /> Novo usuario</button>
        </div>
        <div className="user-filter-grid">
          <Input label="Nome" value={filters.name} onChange={(name) => updateFilter({ name })} />
          <Input label="E-mail" value={filters.email} onChange={(email) => updateFilter({ email })} />
          <Select label="Perfil" value={filters.role} onChange={(role) => updateFilter({ role })} options={Object.entries(roleLabels).map(([value, label]) => ({ value, label }))} optionalLabel="Todos" />
          <Select label="Status" value={filters.status} onChange={(status) => updateFilter({ status })} options={[{ value: "active", label: "Ativo" }, { value: "inactive", label: "Inativo" }]} optionalLabel="Todos" />
          <Select label="Profissional vinculado" value={filters.professionalId} onChange={(professionalId) => updateFilter({ professionalId })} options={professionals.map((item) => ({ value: String(item.id), label: item.name }))} optionalLabel="Todos" />
          <Input label="Instituicao" value={filters.institution} onChange={(institution) => updateFilter({ institution })} />
          <Input label="Diretoria" value={filters.board} onChange={(board) => updateFilter({ board })} />
          <Input label="Area" value={filters.area} onChange={(area) => updateFilter({ area })} />
        </div>
      </Panel>

      <Panel title="Usuarios">
        <div className="table-toolbar">
          <span>Total de registros: <strong>{filteredUsers.length}</strong></span>
          <div className="pagination-controls">
            <span>Pagina {currentPage} de {totalPages}</span>
            <Select label="Por pagina" value={String(pageSize)} onChange={(value) => updatePageSize(Number(value))} options={[5, 10, 20, 50, 100].map((value) => ({ value: String(value), label: String(value) }))} />
            <button className="secondary-button" disabled={currentPage === 1} onClick={() => updatePage(Math.max(1, currentPage - 1))}><ChevronLeft size={16} /> Anterior</button>
            <button className="secondary-button" disabled={currentPage === totalPages} onClick={() => updatePage(Math.min(totalPages, currentPage + 1))}>Proxima <ChevronRight size={16} /></button>
          </div>
        </div>
        <table>
          <thead><tr><th>Nome</th><th>Email</th><th>Perfil</th><th>Profissional</th><th>Status</th><th>Acoes</th></tr></thead>
          <tbody>{pageRows.map((item) => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>{item.email}</td>
              <td>{roleLabels[item.role]}</td>
              <td>{item.professionalName ?? "-"}</td>
              <td><StatusBadge label={item.active ? "Ativo" : "Inativo"} tone={item.active ? "good" : "muted"} /></td>
              <td className="actions">
                <button disabled={!canEditUser(currentUser, item)} onClick={() => openEdit(item)}><Pencil size={15} /> Editar</button>
                {item.systemUser || item.role === "SUPER_ADMIN" ? (
                  <button disabled>Protegido</button>
                ) : item.active ? (
                  <button onClick={() => statusChange(item.id, "deactivate")}>Inativar</button>
                ) : (
                  <button onClick={() => statusChange(item.id, "activate")}>Ativar</button>
                )}
                {item.role !== "SUPER_ADMIN" && !item.professionalId && <button onClick={() => openLink(item)}>Vincular profissional</button>}
                {item.role !== "SUPER_ADMIN" && item.professionalId && <button onClick={() => unlinkProfessional(item)}>Remover vinculo</button>}
              </td>
            </tr>
          ))}</tbody>
        </table>
      </Panel>
      {editing && (
        <Modal title="Editar usuario" onClose={closeEdit}>
          <UserFormFields
            form={editForm}
            fieldErrors={editFieldErrors}
            allowedRoles={allowedRoles}
            showActive
            canEditSensitive={currentUser?.role === "SUPER_ADMIN"}
            onChange={setEditForm}
          />
          <div className="modal-actions">
            <button className="ghost-button" onClick={closeEdit}>Cancelar</button>
            <button className="primary" onClick={saveEdit}>Salvar alteracao</button>
          </div>
        </Modal>
      )}
      {justificationAction && (
        <JustificationModal
          onCancel={() => setJustificationAction(null)}
          onConfirm={(justification) => {
            justificationAction.onConfirm(justification);
            setJustificationAction(null);
          }}
        />
      )}
      {linking && (
        <LinkProfessionalModal
          professionals={linkableProfessionals}
          userName={linking.name}
          onCancel={() => setLinking(null)}
          onConfirm={linkProfessional}
        />
      )}
    </div>
  );
}

function UserCreatePage({
  currentUser,
  onCreate,
  onSearch
}: {
  currentUser: AuthUser | null;
  onCreate: (body: Parameters<typeof api.createUser>[0]) => Promise<void>;
  onSearch: () => void;
}) {
  const allowedRoles = allowedRoleOptions(currentUser?.role);
  const defaultRole = (allowedRoles[0]?.value ?? "PROFESSIONAL") as AppUser["role"];
  const [form, setForm] = useState<UserFormState>(() => emptyUserForm(defaultRole));
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  async function createUser() {
    setError("");
    setFieldErrors({});
    if (allowedRoles.length === 0) {
      setError("Você não possui permissão para conceder este perfil.");
      return;
    }
    const clientErrors = validateUserForm(form, false);
    setFieldErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      return;
    }
    try {
      await onCreate({
        fullName: form.fullName,
        cpf: digitsOnly(form.cpf),
        birthDate: form.birthDate,
        motherName: form.motherName,
        fatherName: form.fatherName,
        email: form.email,
        role: form.role,
        professionalId: null,
        contacts: form.contacts.map(toContactPayload),
        address: { ...form.address, zipCode: digitsOnly(form.address.zipCode) }
      });
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        setError(Object.keys(err.fieldErrors).length ? "" : err.message);
      } else {
        setError(err instanceof Error ? err.message : "Nao foi possivel cadastrar o usuario.");
      }
    }
  }

  return (
    <div className="stack">
      {error && <div className="notice error">{error}</div>}
      <Panel title="Cadastrar usuario">
        <div className="panel-header-actions">
          <button className="ghost-button" type="button" onClick={onSearch}>Ir para pesquisa</button>
        </div>
        <form className="form" onSubmit={(event) => submit(event, createUser)}>
          <UserFormFields
            form={form}
            fieldErrors={fieldErrors}
            allowedRoles={allowedRoles}
            showActive={false}
            canEditSensitive
            onChange={setForm}
          />
          <button className="primary" disabled={allowedRoles.length === 0} type="submit">Cadastrar</button>
        </form>
      </Panel>
    </div>
  );
}

function UserFormFields({
  form,
  fieldErrors,
  allowedRoles,
  showActive,
  canEditSensitive,
  onChange
}: {
  form: UserFormState;
  fieldErrors: FieldErrors;
  allowedRoles: Array<{ value: string; label: string }>;
  showActive: boolean;
  canEditSensitive: boolean;
  onChange: (form: UserFormState) => void;
}) {
  const [cepError, setCepError] = useState("");
  const [ddiReferences, setDdiReferences] = useState<DdiReference[]>(fallbackDdiOptions);
  const [dddReferences, setDddReferences] = useState<DddReference[]>(fallbackDddOptions);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api.ddis().catch(() => fallbackDdiOptions),
      api.ddds().catch(() => fallbackDddOptions)
    ]).then(([ddis, ddds]) => {
      if (cancelled) return;
      setDdiReferences(ddis.length ? ddis : fallbackDdiOptions);
      setDddReferences(ddds.length ? ddds : fallbackDddOptions);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  function updateContact(index: number, next: Partial<UserContact>) {
    onChange({
      ...form,
      contacts: form.contacts.map((contact, currentIndex) => currentIndex === index ? { ...contact, ...next } : contact)
    });
  }

  function addContact() {
    onChange({
      ...form,
      contacts: [...form.contacts, { type: "MOBILE", ddi: "+55", ddd: "", phone: "" }]
    });
  }

  function removeContact(index: number) {
    onChange({
      ...form,
      contacts: form.contacts.filter((_, currentIndex) => currentIndex !== index)
    });
  }

  async function fetchZipCode(zipCode: string) {
    const cleanZipCode = digitsOnly(zipCode);
    onChange({ ...form, address: { ...form.address, zipCode: formatCep(cleanZipCode) } });
    setCepError("");
    if (cleanZipCode.length !== 8) return;
    try {
      const payload = await api.cep(cleanZipCode);
      onChange({
        ...form,
        address: {
          ...form.address,
          zipCode: formatCep(cleanZipCode),
          street: payload.street ?? "",
          neighborhood: payload.neighborhood ?? "",
          city: payload.city ?? "",
          state: payload.state ?? ""
        }
      });
    } catch (err) {
      setCepError(err instanceof Error ? err.message : "CEP: Não foi possível consultar o CEP.");
    }
  }

  return (
    <div className="user-form-sections">
      <section className="form-section">
        <h3>Dados pessoais</h3>
        <div className="form-grid">
          <Input label="Nome completo" value={form.fullName} onChange={(fullName) => onChange({ ...form, fullName })} error={firstError(fieldErrors, "fullName")} />
          <Input label="CPF" value={form.cpf} disabled={!canEditSensitive && Boolean(form.cpf)} onChange={(cpf) => onChange({ ...form, cpf: formatCpf(cpf) })} error={firstError(fieldErrors, "cpf")} />
          <Input label="Data de nascimento" type="date" value={form.birthDate} onChange={(birthDate) => onChange({ ...form, birthDate })} error={firstError(fieldErrors, "birthDate")} />
          <Input label="Nome da mãe" value={form.motherName} onChange={(motherName) => onChange({ ...form, motherName })} error={firstError(fieldErrors, "motherName")} />
          <Input label="Nome do pai" value={form.fatherName} onChange={(fatherName) => onChange({ ...form, fatherName })} />
        </div>
      </section>

      <section className="form-section">
        <div className="section-title-row">
          <h3>Dados de contato</h3>
          <button className="secondary-button" type="button" onClick={addContact}>Adicionar contato</button>
        </div>
        <div className="contacts-list">
          {form.contacts.map((contact, index) => (
            <div className="contact-row" key={index}>
              <Select label="Tipo de contato" value={contact.type} onChange={(type) => updateContact(index, { type: type as ContactType })} options={[
                { value: "RESIDENTIAL", label: "Residencial" },
                { value: "MOBILE", label: "Celular" }
              ]} error={firstError(fieldErrors, `contacts[${index}].type`)} />
              <Select
                label="DDI"
                value={contact.ddi}
                onChange={(ddi) => updateContact(index, { ddi, ddd: ddi === "+55" ? contact.ddd : "" })}
                options={ddiReferences.map((ddi) => ({ value: ddi.code, label: `${ddi.code} - ${ddi.country}` }))}
                error={firstError(fieldErrors, `contacts[${index}].ddi`)}
              />
              <Select
                label="DDD"
                value={contact.ddd}
                onChange={(ddd) => updateContact(index, { ddd })}
                options={dddReferences.map((ddd) => ({ value: ddd.ddd, label: `${ddd.ddd} - ${ddd.state}` }))}
                optionalLabel={contact.ddi === "+55" ? "Selecione" : "Não obrigatório"}
                disabled={contact.ddi !== "+55"}
                error={firstError(fieldErrors, `contacts[${index}].ddd`)}
              />
              <Input
                label="Telefone"
                value={formatPhone(contact.phone, contact.type, contact.ddi)}
                onChange={(phone) => updateContact(index, { phone: phoneDigitsForContact(phone, contact.type, contact.ddi) })}
                error={firstError(fieldErrors, `contacts[${index}].phone`)}
              />
              <button className="ghost-button" type="button" disabled={form.contacts.length === 1} onClick={() => removeContact(index)}>Remover</button>
            </div>
          ))}
        </div>
        {firstError(fieldErrors, "contacts") && <div className="field-error">{firstError(fieldErrors, "contacts")}</div>}
      </section>

      <section className="form-section">
        <h3>Endereço</h3>
        <div className="form-grid">
          <Input label="CEP" value={form.address.zipCode} onChange={fetchZipCode} error={cepError || firstError(fieldErrors, "address.zipCode")} />
          <Input label="Logradouro" value={form.address.street} onChange={(street) => onChange({ ...form, address: { ...form.address, street } })} />
          <Input label="Número" value={form.address.number} onChange={(number) => onChange({ ...form, address: { ...form.address, number } })} error={firstError(fieldErrors, "address.number")} />
          <Input label="Complemento" value={form.address.complement ?? ""} onChange={(complement) => onChange({ ...form, address: { ...form.address, complement } })} />
          <Input label="Bairro" value={form.address.neighborhood} disabled onChange={(neighborhood) => onChange({ ...form, address: { ...form.address, neighborhood } })} />
          <Input label="Cidade" value={form.address.city} disabled onChange={(city) => onChange({ ...form, address: { ...form.address, city } })} />
          <Input label="UF" value={form.address.state} disabled onChange={(state) => onChange({ ...form, address: { ...form.address, state: state.toUpperCase().slice(0, 2) } })} />
        </div>
      </section>

      <section className="form-section">
        <h3>Dados de acesso</h3>
        <div className="form-grid">
          <Input label="E-mail" value={form.email} disabled={!canEditSensitive && Boolean(form.email)} onChange={(email) => onChange({ ...form, email })} error={firstError(fieldErrors, "email")} />
          <Select label="Perfil" value={form.role} onChange={(role) => onChange({ ...form, role: role as AppUser["role"] })} options={allowedRoles} error={firstError(fieldErrors, "role")} />
          {showActive && <label className="check"><input type="checkbox" checked={form.active} onChange={(event) => onChange({ ...form, active: event.target.checked })} /> Ativo</label>}
        </div>
      </section>
    </div>
  );
}

function AuditPage({
  users,
  rows,
  onSearch
}: {
  users: AppUser[];
  rows: AuditLog[];
  onSearch: (filters: Parameters<typeof api.auditLogs>[0]) => void;
}) {
  const [filters, setFilters] = useState({ entityName: "", action: "", performedByUserId: "", startAt: "", endAt: "" });
  return (
    <div className="stack">
      <Panel title="Filtros de auditoria">
        <form className="audit-form" onSubmit={(event) => submit(event, () => onSearch(filters))}>
          <Select label="Entidade" value={filters.entityName} onChange={(entityName) => setFilters({ ...filters, entityName })} options={[
            { value: "User", label: "Usuario" },
            { value: "Professional", label: "Profissional" },
            { value: "Institution", label: "Instituicao" },
            { value: "OrganizationUnit", label: "Unidade organizacional" }
          ]} optionalLabel="Todas" />
          <Select label="Acao" value={filters.action} onChange={(action) => setFilters({ ...filters, action })} options={auditActions.map((action) => ({ value: action, label: auditActionLabels[action] }))} optionalLabel="Todas" />
          <Select label="Responsavel" value={filters.performedByUserId} onChange={(performedByUserId) => setFilters({ ...filters, performedByUserId })} options={users.map((item) => ({ value: String(item.id), label: item.name }))} optionalLabel="Todos" />
          <Input label="Data inicial" type="date" value={filters.startAt} onChange={(startAt) => setFilters({ ...filters, startAt })} />
          <Input label="Data final" type="date" value={filters.endAt} onChange={(endAt) => setFilters({ ...filters, endAt })} />
          <SubmitButton label="Consultar" />
        </form>
      </Panel>
      <Panel title="Registros de auditoria">
        <table>
          <thead><tr><th>Data</th><th>Entidade</th><th>Acao</th><th>Responsavel</th><th>Justificativa</th><th>IP</th></tr></thead>
          <tbody>{rows.map((item) => (
            <tr key={item.id}>
              <td>{new Date(item.performedAt).toLocaleString("pt-BR")}</td>
              <td>{item.entityName} #{item.entityId}</td>
              <td>{auditActionLabels[item.action] ?? item.action}</td>
              <td>{item.performedByUserName}</td>
              <td>{item.justification}</td>
              <td>{item.ipAddress ?? "-"}</td>
            </tr>
          ))}</tbody>
        </table>
      </Panel>
    </div>
  );
}

function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <header className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Fechar">x</button>
        </header>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  );
}

function JustificationModal({ onCancel, onConfirm }: { onCancel: () => void; onConfirm: (justification: string) => void }) {
  const [justification, setJustification] = useState("");
  const [error, setError] = useState("");
  function confirm() {
    if (!justification.trim()) {
      setError("Justificativa: Não deve estar em branco.");
      return;
    }
    onConfirm(justification.trim());
  }
  return (
    <Modal title="Justificativa da alteração" onClose={onCancel}>
      <p className="modal-copy">Esta alteração será registrada na auditoria com usuário responsável, data, hora e valores alterados.</p>
      <label className="field">
        <span>Justificativa</span>
        <textarea value={justification} onChange={(event) => { setJustification(event.target.value); setError(""); }} rows={5} autoFocus />
      </label>
      {error && <div className="inline-error">{error}</div>}
      <div className="modal-actions">
        <button className="ghost-button" onClick={onCancel}>Cancelar</button>
        <button className="primary" onClick={confirm}>Confirmar alteração</button>
      </div>
    </Modal>
  );
}

function LinkProfessionalModal({
  professionals,
  userName,
  onCancel,
  onConfirm
}: {
  professionals: Professional[];
  userName: string;
  onCancel: () => void;
  onConfirm: (professionalId: number) => void;
}) {
  const [professionalId, setProfessionalId] = useState("");
  const [error, setError] = useState("");
  function confirm() {
    if (!professionalId) {
      setError("Profissional: Deve ser informado.");
      return;
    }
    onConfirm(Number(professionalId));
  }
  return (
    <Modal title="Vincular profissional" onClose={onCancel}>
      <p className="modal-copy">Selecione o profissional que recebera acesso pelo usuario {userName}.</p>
      <Select
        label="Profissional"
        value={professionalId}
        onChange={(value) => { setProfessionalId(value); setError(""); }}
        options={professionals.map((item) => ({ value: String(item.id), label: `${item.name} (${item.email})` }))}
      />
      {professionals.length === 0 && <div className="inline-error">Nao ha profissionais ativos disponiveis para vinculo.</div>}
      {error && <div className="inline-error">{error}</div>}
      <div className="modal-actions">
        <button className="ghost-button" onClick={onCancel}>Cancelar</button>
        <button className="primary" disabled={professionals.length === 0} onClick={confirm}>Confirmar vinculo</button>
      </div>
    </Modal>
  );
}

function RequestsTable({ rows }: { rows: AbsenceRequest[] }) {
  return (
    <table>
      <thead><tr><th>Profissional</th><th>Tipo</th><th>Periodo</th><th>Dias</th><th>Status</th></tr></thead>
      <tbody>
        {rows.map((item) => (
          <tr key={item.id}>
            <td>{item.professionalName}</td>
            <td>{absenceTypeLabels[item.absenceType]}</td>
            <td>{item.startDate} ate {item.endDate}</td>
            <td>{item.requestedDays}</td>
            <td><StatusBadge label={statusLabels[item.status]} tone={item.status === "APPROVED" ? "good" : item.status === "PENDING" ? "warn" : "muted"} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div className="metric"><span>{label}</span><strong>{value}</strong></div>;
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return <section className="panel"><h2>{title}</h2>{children}</section>;
}

function TwoColumn({ children }: { children: ReactNode }) {
  return <div className="two-column">{children}</div>;
}

function Input({
  label,
  value,
  onChange,
  type = "text",
  error,
  disabled = false
}: {
  label: string;
  value: string;
  onChange: (value: string) => void | Promise<void>;
  type?: string;
  error?: string;
  disabled?: boolean;
}) {
  return (
    <label className={`field ${error ? "invalid" : ""}`}>
      <span>{label}</span>
      <input type={type} value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)} />
      <small className="field-error">{error || "\u00a0"}</small>
    </label>
  );
}

function Select({
  label,
  value,
  onChange,
  options,
  optionalLabel,
  error,
  disabled = false
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string }>;
  optionalLabel?: string;
  error?: string;
  disabled?: boolean;
}) {
  return (
    <label className={`field ${error ? "invalid" : ""}`}>
      <span>{label}</span>
      <select value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)}>
        <option value="">{optionalLabel ?? "Selecione"}</option>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
      <small className="field-error">{error || "\u00a0"}</small>
    </label>
  );
}

function StatusBadge({ label, tone }: { label: string; tone: "good" | "warn" | "muted" }) {
  return <span className={`status ${tone}`}>{label}</span>;
}

function SubmitButton({ label }: { label: string }) {
  return <button className="primary" type="submit">{label}</button>;
}

function submit(event: FormEvent, action: () => void) {
  event.preventDefault();
  action();
}

function resolveCurrentUser(users: AppUser[]): AuthUser | null {
  const token = getAuthToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1])) as { uid?: number };
    const user = users.find((item) => item.id === payload.uid);
    if (!user) return null;
    return {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      professionalId: user.professionalId,
      systemUser: user.systemUser,
      lastLoginAt: user.lastLoginAt
    };
  } catch {
    return null;
  }
}

function allowedRoleOptions(currentRole?: AppUser["role"] | null) {
  const allowed = (() => {
    switch (currentRole) {
      case "SUPER_ADMIN":
        return ["SUPER_ADMIN", "ADMIN", "MANAGER", "PROFESSIONAL", "VIEWER"];
      case "ADMIN":
        return ["ADMIN", "MANAGER", "PROFESSIONAL", "VIEWER"];
      case "MANAGER":
        return ["MANAGER", "PROFESSIONAL", "VIEWER"];
      default:
        return [];
    }
  })() as AppUser["role"][];
  return allowed.map((role) => ({ value: role, label: roleLabels[role] }));
}

function canEditUser(currentUser: AuthUser | null, target: AppUser) {
  if (!currentUser) return false;
  if (target.role === "SUPER_ADMIN" && currentUser.role !== "SUPER_ADMIN") return false;
  return roleRank(target.role) <= roleRank(currentUser.role);
}

function roleRank(role: AppUser["role"]) {
  switch (role) {
    case "SUPER_ADMIN":
      return 4;
    case "ADMIN":
      return 3;
    case "MANAGER":
      return 2;
    case "PROFESSIONAL":
      return 1;
    case "VIEWER":
      return 0;
  }
}

function tokenExpiration(token: string | null) {
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1])) as { exp?: number };
    return payload.exp ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function formatSessionRemaining(expiresAt: number | null, currentTime: number) {
  if (!expiresAt) return "--:--";
  const remainingSeconds = Math.max(0, Math.floor((expiresAt - currentTime) / 1000));
  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function firstError(errors: FieldErrors, field: string) {
  return errors[field]?.[0] ?? "";
}

function digitsOnly(value: string) {
  return value.replace(/\D/g, "");
}

function formatCpf(value: string) {
  const digits = digitsOnly(value).slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

function formatCep(value: string) {
  const digits = digitsOnly(value).slice(0, 8);
  return digits.replace(/(\d{5})(\d{1,3})$/, "$1-$2");
}

function isValidCpf(value: string) {
  const cpf = digitsOnly(value);
  if (cpf.length !== 11 || new Set(cpf).size === 1) return false;
  const digit = (length: number) => {
    let sum = 0;
    for (let index = 0; index < length; index += 1) {
      sum += Number(cpf[index]) * (length + 1 - index);
    }
    const result = 11 - (sum % 11);
    return result >= 10 ? 0 : result;
  };
  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10]);
}

function validateUserForm(form: UserFormState, editing: boolean) {
  const errors: FieldErrors = {};
  const add = (field: string, message: string) => {
    errors[field] = [...(errors[field] ?? []), message];
  };
  if (!form.fullName.trim()) add("fullName", "Não deve estar em branco.");
  if (!form.cpf.trim()) add("cpf", "Não deve estar em branco.");
  if (form.cpf.trim() && !isValidCpf(form.cpf)) add("cpf", "CPF inválido.");
  if (!form.birthDate) add("birthDate", "Deve ser informada.");
  if (!form.motherName.trim()) add("motherName", "Não deve estar em branco.");
  if (!form.email.trim()) add("email", "Não deve estar em branco.");
  if (!form.role) add("role", "Deve ser informado.");
  if (!form.contacts.length) add("contacts", "Informe ao menos um contato.");
  form.contacts.forEach((contact, index) => {
    if (!contact.type) add(`contacts[${index}].type`, "Deve ser informado.");
    if (!contact.ddi) add(`contacts[${index}].ddi`, "Não deve estar em branco.");
    if (contact.ddi === "+55" && !contact.ddd.trim()) add(`contacts[${index}].ddd`, "Obrigatório para contatos do Brasil.");
    const phoneDigits = digitsOnly(contact.phone);
    if (!phoneDigits.trim()) add(`contacts[${index}].phone`, "Não deve estar em branco.");
    if (phoneDigits.length > 9) add(`contacts[${index}].phone`, "Deve ter no máximo 9 caracteres.");
    if (contact.ddi === "+55" && contact.type === "MOBILE" && phoneDigits.length !== 9) add(`contacts[${index}].phone`, "Celular deve ter 9 dígitos.");
    if (contact.ddi === "+55" && contact.type === "RESIDENTIAL" && phoneDigits.length !== 8) add(`contacts[${index}].phone`, "Residencial deve ter 8 dígitos.");
  });
  if (!form.address.zipCode.trim()) add("address.zipCode", "Não deve estar em branco.");
  if (!form.address.number.trim()) add("address.number", "Não deve estar em branco.");
  if (editing && !form.active && form.role === "SUPER_ADMIN") add("active", "SUPER_ADMIN não pode ser inativado.");
  return errors;
}

function phoneDigitsForContact(value: string, type: ContactType | "", ddi: string) {
  const limit = ddi === "+55" && type === "RESIDENTIAL" ? 8 : 9;
  return digitsOnly(value).slice(0, limit);
}

function formatPhone(value: string, type: ContactType | "", ddi: string) {
  const digits = phoneDigitsForContact(value, type, ddi);
  if (ddi !== "+55") return digits;
  if (type === "RESIDENTIAL") {
    return digits.replace(/(\d{4})(\d{1,4})$/, "$1-$2");
  }
  return digits.replace(/(\d{5})(\d{1,4})$/, "$1-$2");
}

function toContactPayload(contact: UserContact) {
  return {
    ...contact,
    ddd: contact.ddi === "+55" ? digitsOnly(contact.ddd) : "",
    phone: digitsOnly(contact.phone)
  };
}
