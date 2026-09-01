import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ContractorVacancyLayout from '../../components/vagas/ContractorVacancyLayout';
import VacancyStatusBadge from '../../components/vagas/VacancyStatusBadge';
import {
  listMyVacancies,
  listAllReceivedApplications,
  MY_VACANCIES_PAGE_SIZE,
} from '../../services/vagas/vacancyManagementService';
import { vacancyManagementError } from '../../services/vagas/vacancyManagementMessages';

const FILTERS = [
  { id: 'all', label: 'Todas', matches: () => true },
  { id: 'without', label: 'Sem candidatura', matches: (vacancy, counts) => !counts[vacancy.id] },
  { id: 'with', label: 'Com candidatura', matches: (vacancy, counts) => Boolean(counts[vacancy.id]) },
  { id: 'selection', label: 'Em seleção', matches: (vacancy) => vacancy.status === 'ABERTA' },
  { id: 'closed', label: 'Concluída', matches: (vacancy) => vacancy.status === 'ENCERRADA' },
  { id: 'expired', label: 'Expirada', matches: (vacancy) => Boolean(vacancy.dataLimiteCandidatura) && vacancy.dataLimiteCandidatura < new Date().toISOString().slice(0, 10) },
];

function mergeById(current, incoming) {
  const byId = new Map(current.map((item) => [String(item.id), item]));
  incoming.forEach((item) => byId.set(String(item.id), item));
  return [...byId.values()];
}

function formatDate(value) {
  if (!value) return 'Não informado';
  const [year, month, day] = value.slice(0, 10).split('-');
  return `${day}/${month}/${year}`;
}

function VacancyOwnerCard({ vacancy, applications }) {
  const mutable = vacancy.status !== 'CANCELADA';
  const cancelable = vacancy.status === 'ABERTA' || vacancy.status === 'PAUSADA';
  return (
    <article className="management-vacancy-card">
      <div className="management-vacancy-card__body">
        <div className="management-vacancy-card__heading"><h2>{vacancy.titulo}</h2><VacancyStatusBadge status={vacancy.status} /></div>
        <p>{vacancy.categoria || vacancy.tipoContrato} · {vacancy.modeloTrabalho}</p>
        <p>{vacancy.cidade}/{vacancy.estado} · Prazo: {formatDate(vacancy.dataLimiteCandidatura)}</p>
        <p className="management-vacancy-card__applications">{applications} {applications === 1 ? 'candidatura recebida' : 'candidaturas recebidas'}</p>
      </div>
      <div className="management-vacancy-card__actions">
        <a className="btn btn--primario" href={`/vagas/${vacancy.id}/gerenciar`}>Gerenciar</a>
        {mutable ? <a className="btn management-button-secondary" href={`/vagas/${vacancy.id}/editar`}>Editar</a> : null}
        {cancelable ? <a className="management-danger-link" href={`/vagas/${vacancy.id}/gerenciar#cancelar`}>Cancelar</a> : null}
      </div>
    </article>
  );
}

export default function MyVacanciesPage() {
  const [vacancies, setVacancies] = useState([]);
  const [applicationCounts, setApplicationCounts] = useState({});
  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const loadingRef = useRef(false);
  const initialGenerationRef = useRef(0);

  const loadInitial = useCallback(async () => {
    const generation = initialGenerationRef.current + 1;
    initialGenerationRef.current = generation;
    setLoading(true);
    setError('');
    try {
      const [page, applications] = await Promise.all([
        listMyVacancies({ size: MY_VACANCIES_PAGE_SIZE }),
        listAllReceivedApplications(),
      ]);
      if (generation !== initialGenerationRef.current) return;
      setVacancies(Array.isArray(page.content) ? page.content : []);
      setCursor(page.nextCursor ?? null);
      setHasMore(page.hasMore === true && page.nextCursor !== null && page.nextCursor !== undefined);
      const counts = applications.reduce((result, application) => {
        result[application.vagaId] = (result[application.vagaId] || 0) + 1;
        return result;
      }, {});
      setApplicationCounts(counts);
    } catch (requestError) {
      if (generation === initialGenerationRef.current) {
        setError(vacancyManagementError(requestError, 'Não foi possível carregar suas vagas.'));
      }
    } finally {
      if (generation === initialGenerationRef.current) setLoading(false);
    }
  }, []);

  const loadMore = useCallback(async () => {
    if (loadingRef.current || !hasMore) return;
    loadingRef.current = true;
    setLoading(true);
    setError('');
    try {
      const page = await listMyVacancies({ cursor, size: MY_VACANCIES_PAGE_SIZE });
      setVacancies((current) => mergeById(current, Array.isArray(page.content) ? page.content : []));
      setCursor(page.nextCursor ?? null);
      setHasMore(page.hasMore === true && page.nextCursor !== null && page.nextCursor !== undefined);
    } catch (requestError) {
      setError(vacancyManagementError(requestError, 'Não foi possível carregar suas vagas.'));
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [cursor, hasMore]);

  useEffect(() => {
    document.title = 'Minhas vagas — Palco';
    loadInitial();
  }, [loadInitial]);

  const activeFilter = FILTERS.find((item) => item.id === filter) || FILTERS[0];
  const filtered = useMemo(
    () => vacancies.filter((vacancy) => activeFilter.matches(vacancy, applicationCounts)),
    [activeFilter, applicationCounts, vacancies]
  );

  return (
    <ContractorVacancyLayout>
      <main className="vacancy-management">
        <header className="management-page-header management-page-header--actions"><div><p className="management-eyebrow">Gestão do contratante</p><h1>Minhas vagas</h1><p>Acompanhe status, candidaturas e ações sem expor sua identidade no payload.</p></div><a className="btn btn--primario" href="/vagas/nova">Publicar vaga</a></header>
        <nav className="management-filters" aria-label="Filtrar minhas vagas">{FILTERS.map((item) => <button key={item.id} type="button" aria-pressed={filter === item.id} onClick={() => setFilter(item.id)}>{item.label}<span>{vacancies.filter((vacancy) => item.matches(vacancy, applicationCounts)).length}</span></button>)}</nav>
        {error ? <section className="management-state management-state--error" role="alert"><h2>Não foi possível carregar</h2><p>{error}</p><button className="btn btn--primario" type="button" onClick={vacancies.length ? loadMore : loadInitial}>Tentar novamente</button></section> : null}
        {!error && !loading && filtered.length === 0 ? <section className="management-state"><h2>Nenhuma vaga neste filtro</h2><p>Publique uma oportunidade ou escolha outro filtro.</p></section> : null}
        <section className="management-vacancy-list" aria-live="polite">{filtered.map((vacancy) => <VacancyOwnerCard key={vacancy.id} vacancy={vacancy} applications={applicationCounts[vacancy.id] || 0} />)}</section>
        {loading ? <p className="management-loading" role="status">Carregando vagas…</p> : null}
        {!loading && hasMore ? <button className="btn management-load-more" type="button" onClick={loadMore}>Carregar mais</button> : null}
      </main>
    </ContractorVacancyLayout>
  );
}

export { FILTERS as MY_VACANCY_FILTERS, mergeById };
