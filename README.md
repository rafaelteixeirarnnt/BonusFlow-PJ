# BonusFlow PJ

Aplicacao web para gerenciar bonificacoes e afastamentos previstos em contrato para profissionais PJ.

## Stack

- Backend: Spring Boot, Java 21, JPA, Bean Validation, Flyway
- Frontend: React, TypeScript, Vite
- Banco: PostgreSQL
- Orquestracao local: Docker Compose

## Estrutura

```text
backend/    API REST, entidades, repositories, services, controllers e migrations
frontend/   Aplicacao React com dashboard, cadastros, solicitacoes, aprovacao e relatorios
```

## Rodando com Docker Compose

```bash
docker compose up --build
```

Acesse:

- Frontend: http://localhost:5173
- API: http://localhost:8080/api
- PostgreSQL: localhost:5432, database `bonusflow`, usuario `bonusflow`, senha `bonusflow`

## Rodando localmente sem Docker

Suba um PostgreSQL local e exporte as variaveis, se quiser sobrescrever os padroes:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bonusflow
export SPRING_DATASOURCE_USERNAME=bonusflow
export SPRING_DATASOURCE_PASSWORD=bonusflow
```

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Fluxo inicial de uso

1. Cadastre um usuario Admin ou Gestor.
2. Cadastre um profissional PJ ativo.
3. Cadastre uma regra contratual para o profissional e tipo de afastamento.
4. Registre uma solicitacao de afastamento.
5. Aprove, reprove ou cancele a solicitacao pela tela de aprovacoes.
6. Consulte dashboard e relatorios por mes, profissional e tipo.

## Regras implementadas

- Saldo disponivel = dias previstos na regra contratual menos dias aprovados.
- Apenas solicitacoes aprovadas consomem saldo.
- Profissional inativo nao pode receber lancamento.
- Data final menor que data inicial e bloqueada.
- Periodos pendentes ou aprovados sobrepostos para o mesmo profissional sao bloqueados.
- Todo lancamento registra criacao, usuario responsavel e status.
- Toda aprovacao, reprovacao ou cancelamento gera historico.

## Perfis

Os perfis `ADMIN`, `MANAGER` e `VIEWER` ja estao modelados na entidade `User` e disponiveis nos cadastros. Esta estrutura inicial ainda nao inclui login nem bloqueio de endpoints por perfil; esse e o proximo passo natural com Spring Security/JWT ou integracao SSO.

## Endpoints principais

- `GET/POST /api/professionals`
- `GET/POST /api/users`
- `GET/POST /api/contract-rules`
- `GET/POST /api/absence-requests`
- `PATCH /api/absence-requests/{id}/approve`
- `PATCH /api/absence-requests/{id}/reject`
- `PATCH /api/absence-requests/{id}/cancel`
- `GET /api/absence-requests/balance`
- `GET /api/absence-requests/report?month=2026-07`
- `GET /api/absence-requests/{id}/history`
- `GET /api/dashboard`

## Testes

```bash
cd backend
mvn test
```
