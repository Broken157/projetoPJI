import { useRef, useState } from 'react';

export const EMPTY_VACANCY_FORM = {
  titulo: '',
  categoria: '',
  descricao: '',
  requisitos: '',
  remuneraValor: '',
  formaPagamento: '',
  cidade: '',
  estado: '',
  enderecoCompleto: '',
  beneficios: '',
  modeloTrabalho: 'PRESENCIAL',
  tipoContrato: '',
  experiencia: '',
  dataLimiteCandidatura: '',
  abrangencia: '',
  tagIds: [],
  fotosTexto: '',
};

export function vacancyToForm(vacancy = {}) {
  return {
    ...EMPTY_VACANCY_FORM,
    titulo: vacancy.titulo || '',
    categoria: vacancy.categoria || '',
    descricao: vacancy.descricao || '',
    requisitos: vacancy.requisitos || '',
    remuneraValor: vacancy.remuneraValor ?? '',
    formaPagamento: vacancy.formaPagamento || '',
    cidade: vacancy.cidade || '',
    estado: vacancy.estado || '',
    enderecoCompleto: vacancy.enderecoCompleto || '',
    beneficios: vacancy.beneficios || '',
    modeloTrabalho: vacancy.modeloTrabalho || 'PRESENCIAL',
    tipoContrato: vacancy.tipoContrato || '',
    experiencia: vacancy.experiencia || '',
    dataLimiteCandidatura: vacancy.dataLimiteCandidatura || '',
    abrangencia: vacancy.abrangencia || '',
    tagIds: Array.isArray(vacancy.tagIds) ? vacancy.tagIds.map(Number) : [],
    fotosTexto: Array.isArray(vacancy.fotos) ? vacancy.fotos.join('\n') : '',
  };
}

function validationMessage(form) {
  if (!form.titulo.trim() || !form.descricao.trim() || !form.requisitos.trim()
    || form.remuneraValor === '' || !form.formaPagamento.trim() || !form.cidade.trim()
    || !form.estado.trim() || !form.modeloTrabalho || !form.tipoContrato.trim()) {
    return 'Preencha todos os campos obrigatórios.';
  }
  if (!/^[A-Za-z]{2}$/.test(form.estado.trim())) return 'Informe o estado com uma UF válida de duas letras.';
  if (!Number.isFinite(Number(form.remuneraValor)) || Number(form.remuneraValor) < 0) return 'A remuneração não pode ser negativa.';
  if (form.fotosTexto.split(/\r?\n/).some((url) => url.trim().length > 500)) return 'Cada URL de foto deve ter no máximo 500 caracteres.';
  return '';
}

export default function VacancyForm({
  initialValue = EMPTY_VACANCY_FORM,
  tags = [],
  submitLabel,
  onSubmit,
  cancelHref = '/minhas-vagas',
}) {
  const [form, setForm] = useState(() => vacancyToForm(initialValue));
  const [validationError, setValidationError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name === 'estado' ? value.toUpperCase() : value,
    }));
  }

  function toggleTag(event) {
    const id = Number(event.target.value);
    setForm((current) => ({
      ...current,
      tagIds: event.target.checked
        ? [...new Set([...current.tagIds, id])]
        : current.tagIds.filter((tagId) => tagId !== id),
    }));
  }

  async function submit(event) {
    event.preventDefault();
    if (submittingRef.current) return;
    const error = validationMessage(form);
    if (error) {
      setValidationError(error);
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setValidationError('');
    try {
      await onSubmit({
        ...form,
        fotos: form.fotosTexto.split(/\r?\n/).map((url) => url.trim()).filter(Boolean),
      });
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <form className="management-form" onSubmit={submit} noValidate>
      <label className="campo"><span className="campo__rotulo">Título da vaga</span><input className="campo__input" name="titulo" maxLength={150} value={form.titulo} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Categoria artística</span><input className="campo__input" name="categoria" maxLength={100} value={form.categoria} onChange={updateField} /></label>
      <label className="campo management-form__wide"><span className="campo__rotulo">Descrição</span><textarea className="campo__input" name="descricao" value={form.descricao} onChange={updateField} required /></label>
      <label className="campo management-form__wide"><span className="campo__rotulo">Requisitos</span><textarea className="campo__input" name="requisitos" value={form.requisitos} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Remuneração</span><input className="campo__input" type="number" name="remuneraValor" min="0" step="0.01" inputMode="decimal" value={form.remuneraValor} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Forma de pagamento</span><input className="campo__input" name="formaPagamento" maxLength={100} value={form.formaPagamento} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Cidade</span><input className="campo__input" name="cidade" maxLength={100} value={form.cidade} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Estado (UF)</span><input className="campo__input" name="estado" maxLength={2} value={form.estado} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Endereço completo</span><input className="campo__input" name="enderecoCompleto" value={form.enderecoCompleto} onChange={updateField} /></label>
      <label className="campo"><span className="campo__rotulo">Benefícios</span><input className="campo__input" name="beneficios" value={form.beneficios} onChange={updateField} /></label>
      <label className="campo"><span className="campo__rotulo">Modelo de trabalho</span><select className="campo__select management-form__select" name="modeloTrabalho" value={form.modeloTrabalho} onChange={updateField}><option value="PRESENCIAL">Presencial</option><option value="REMOTO">Remoto</option><option value="HIBRIDO">Híbrido</option></select></label>
      <label className="campo"><span className="campo__rotulo">Tipo de contrato</span><input className="campo__input" name="tipoContrato" maxLength={100} value={form.tipoContrato} onChange={updateField} required /></label>
      <label className="campo"><span className="campo__rotulo">Experiência</span><input className="campo__input" name="experiencia" maxLength={100} value={form.experiencia} onChange={updateField} /></label>
      <label className="campo"><span className="campo__rotulo">Prazo para candidatura</span><input className="campo__input" type="date" name="dataLimiteCandidatura" value={form.dataLimiteCandidatura} onChange={updateField} /></label>
      <label className="campo"><span className="campo__rotulo">Abrangência</span><select className="campo__select management-form__select" name="abrangencia" value={form.abrangencia} onChange={updateField}><option value="">Não informada</option><option value="local">Local</option><option value="regional">Regional</option><option value="nacional">Nacional</option><option value="remoto">Remoto</option></select></label>
      <fieldset className="management-tags"><legend>Tags</legend>{tags.length ? tags.map((tag) => <label className="management-tag-option" key={tag.id}><input type="checkbox" value={tag.id} checked={form.tagIds.includes(Number(tag.id))} onChange={toggleTag} /> <span>{tag.nome}</span></label>) : <p>Nenhuma tag disponível.</p>}</fieldset>
      <label className="campo management-form__wide"><span className="campo__rotulo">Fotos (uma URL por linha)</span><textarea className="campo__input management-form__photos" name="fotosTexto" value={form.fotosTexto} onChange={updateField} /></label>
      {validationError ? <p className="management-feedback management-feedback--error management-form__wide" role="alert">{validationError}</p> : null}
      <div className="management-form__actions management-form__wide"><a className="btn management-button-secondary" href={cancelHref}>Cancelar</a><button className="btn btn--primario" type="submit" disabled={submitting}>{submitting ? 'Salvando…' : submitLabel}</button></div>
    </form>
  );
}

export { validationMessage };
