import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import VacancyForm from './VacancyForm';

function fillRequired() {
  fireEvent.change(screen.getByLabelText('Título da vaga'), { target: { value: 'Cantora para evento' } });
  fireEvent.change(screen.getByLabelText('Descrição'), { target: { value: 'Apresentação cultural' } });
  fireEvent.change(screen.getByLabelText('Requisitos'), { target: { value: 'Portfólio atualizado' } });
  fireEvent.change(screen.getByLabelText('Remuneração'), { target: { value: '800' } });
  fireEvent.change(screen.getByLabelText('Forma de pagamento'), { target: { value: 'Pix' } });
  fireEvent.change(screen.getByLabelText('Cidade'), { target: { value: 'Recife' } });
  fireEvent.change(screen.getByLabelText('Estado (UF)'), { target: { value: 'pe' } });
  fireEvent.change(screen.getByLabelText('Tipo de contrato'), { target: { value: 'Freelance' } });
}

test('renderiza campos atuais e tags vindas da API', () => {
  render(<VacancyForm tags={[{ id: 4, nome: 'Música' }]} submitLabel="Publicar" onSubmit={jest.fn()} />);
  expect(screen.getByLabelText('Título da vaga')).toHaveAttribute('maxlength', '150');
  expect(screen.getByLabelText('Música')).not.toBeChecked();
  expect(screen.getByRole('button', { name: 'Publicar' })).toBeInTheDocument();
});

test('bloqueia publicação inválida no frontend', () => {
  const onSubmit = jest.fn();
  render(<VacancyForm submitLabel="Publicar" onSubmit={onSubmit} />);
  fireEvent.click(screen.getByRole('button', { name: 'Publicar' }));
  expect(onSubmit).not.toHaveBeenCalled();
  expect(screen.getByRole('alert')).toHaveTextContent('Preencha todos os campos obrigatórios');
});

test('envia campos editáveis, tags e fotos normalizadas', async () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  render(<VacancyForm tags={[{ id: 4, nome: 'Música' }]} submitLabel="Publicar" onSubmit={onSubmit} />);
  fillRequired();
  fireEvent.click(screen.getByLabelText('Música'));
  fireEvent.change(screen.getByLabelText('Fotos (uma URL por linha)'), { target: { value: 'a.jpg\n\nb.jpg' } });
  fireEvent.click(screen.getByRole('button', { name: 'Publicar' }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ estado: 'PE', tagIds: [4], fotos: ['a.jpg', 'b.jpg'] }));
  expect(onSubmit.mock.calls[0][0]).not.toHaveProperty('status');
  expect(onSubmit.mock.calls[0][0]).not.toHaveProperty('contratanteId');
});

test('permite remover todas as tags na edição', async () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  const initialValue = { titulo: 'Vaga', descricao: 'Desc', requisitos: 'Req', remuneraValor: 1, formaPagamento: 'Pix', cidade: 'Recife', estado: 'PE', modeloTrabalho: 'REMOTO', tipoContrato: 'Freela', tagIds: [4] };
  render(<VacancyForm initialValue={initialValue} tags={[{ id: 4, nome: 'Música' }]} submitLabel="Salvar" onSubmit={onSubmit} />);
  fireEvent.click(screen.getByLabelText('Música'));
  fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ tagIds: [] })));
});

test('loading bloqueia duplo submit', async () => {
  let resolveSubmit;
  const onSubmit = jest.fn(() => new Promise((resolve) => { resolveSubmit = resolve; }));
  render(<VacancyForm submitLabel="Publicar" onSubmit={onSubmit} />);
  fillRequired();
  const form = screen.getByRole('button', { name: 'Publicar' }).closest('form');
  fireEvent.submit(form);
  fireEvent.submit(form);
  expect(onSubmit).toHaveBeenCalledTimes(1);
  expect(screen.getByRole('button', { name: 'Salvando…' })).toBeDisabled();
  await act(async () => resolveSubmit());
});
