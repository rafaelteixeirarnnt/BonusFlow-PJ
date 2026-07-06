import {
  CalendarCheck,
  ClipboardList,
  FileBarChart,
  LayoutDashboard,
  LucideIcon,
  Plus,
  Scale,
  UserCog,
  Users
} from "lucide-react";
import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import {
  AbsenceRequest,
  AbsenceType,
  AppUser,
  ContractRule,
  Dashboard,
  Professional,
  absenceTypeLabels,
  api,
  statusLabels
} from "./lib/api";

type View = "dashboard" | "professionals" | "rules" | "requests" | "approvals" | "reports" | "users";

const absenceTypes = Object.keys(absenceTypeLabels) as AbsenceType[];

const navItems: Array<{ id: View; label: string; icon: LucideIcon }> = [
  { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
  { id: "professionals", label: "Profissionais", icon: Users },
  { id: "rules", label: "Regras", icon: Scale },
  { id: "requests", label: "Solicitacoes", icon: ClipboardList },
  { id: "approvals", label: "Aprovacoes", icon: CalendarCheck },
  { id: "reports", label: "Relatorios", icon: FileBarChart },
  { id: "users", label: "Usuarios", icon: UserCog }
];

export function App() {
  const [view, setView] = useState<View>("dashboard");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [users, setUsers] = useState<AppUser[]>([]);
  const [rules, setRules] = useState<ContractRule[]>([]);
  const [requests, setRequests] = useState<AbsenceRequest[]>([]);
  const [reportRows, setReportRows] = useState<AbsenceRequest[]>([]);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

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
    refresh().catch((err) => setError(err instanceof Error ? err.message : "API indisponivel."));
  }, []);

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
              <button className={view === item.id ? "active" : ""} key={item.id} onClick={() => setView(item.id)}>
                <Icon size={18} />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <h1>{navItems.find((item) => item.id === view)?.label}</h1>
            <p>Controle de bonificacoes, afastamentos e saldo contratual para profissionais PJ.</p>
          </div>
          <button className="icon-button" onClick={() => refresh()} title="Atualizar dados">
            <Plus size={18} />
            Atualizar
          </button>
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
        {view === "users" && <UsersPage users={users} onCreate={(body) => run(() => api.createUser(body), "Usuario cadastrado.")} />}
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

function ProfessionalsPage({ professionals, onCreate }: { professionals: Professional[]; onCreate: (body: Omit<Professional, "id">) => void }) {
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

function UsersPage({ users, onCreate }: { users: AppUser[]; onCreate: (body: Omit<AppUser, "id">) => void }) {
  const [form, setForm] = useState({ name: "", email: "", role: "ADMIN" as AppUser["role"], active: true });
  return (
    <TwoColumn>
      <Panel title="Cadastrar usuario">
        <form className="form" onSubmit={(event) => submit(event, () => onCreate(form))}>
          <Input label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} />
          <Input label="Email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
          <Select label="Perfil" value={form.role} onChange={(role) => setForm({ ...form, role: role as AppUser["role"] })} options={[{ value: "ADMIN", label: "Admin" }, { value: "MANAGER", label: "Gestor" }, { value: "VIEWER", label: "Consulta" }]} />
          <label className="check"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Ativo</label>
          <SubmitButton label="Cadastrar" />
        </form>
      </Panel>
      <Panel title="Usuarios">
        <table>
          <thead><tr><th>Nome</th><th>Email</th><th>Perfil</th><th>Status</th></tr></thead>
          <tbody>{users.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.email}</td><td>{item.role}</td><td><StatusBadge label={item.active ? "Ativo" : "Inativo"} tone={item.active ? "good" : "muted"} /></td></tr>)}</tbody>
        </table>
      </Panel>
    </TwoColumn>
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

function Input({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return <label className="field"><span>{label}</span><input type={type} value={value} onChange={(event) => onChange(event.target.value)} /></label>;
}

function Select({ label, value, onChange, options, optionalLabel }: { label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }>; optionalLabel?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{optionalLabel ?? "Selecione"}</option>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
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
