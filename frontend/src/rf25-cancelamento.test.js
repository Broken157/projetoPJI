import fs from 'fs';
import path from 'path';

test('cancelamento exige motivo e envia confirmação com motivo normalizado', () => {
  document.body.innerHTML = `
    <button data-abrir-modal data-vaga-id="7">Cancelar</button>
    <div data-modal-exclusao hidden>
      <input data-modal-input>
      <textarea data-modal-motivo></textarea>
      <button data-modal-confirmar disabled>Confirmar</button>
    </div>
  `;
  window.alert = jest.fn();
  window.fetch = jest.fn(() => new Promise(() => {}));

  const script = fs.readFileSync(
    path.join(process.cwd(), 'public', 'js', 'main.js'),
    'utf8'
  );
  window.eval(script);
  document.dispatchEvent(new Event('DOMContentLoaded'));

  document.querySelector('[data-abrir-modal]').click();
  const confirmacaoVisual = document.querySelector('[data-modal-input]');
  const motivo = document.querySelector('[data-modal-motivo]');
  const confirmar = document.querySelector('[data-modal-confirmar]');

  confirmacaoVisual.value = 'DELETAR';
  confirmacaoVisual.dispatchEvent(new Event('input'));
  expect(confirmar).not.toBeDisabled();

  confirmar.click();
  expect(window.alert).toHaveBeenCalledWith('Informe o motivo do cancelamento.');
  expect(window.fetch).not.toHaveBeenCalled();

  motivo.value = '  Projeto cancelado.  ';
  confirmar.click();

  expect(window.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/vagas/7',
    expect.objectContaining({
      method: 'DELETE',
      body: JSON.stringify({ confirmacao: true, motivo: 'Projeto cancelado.' })
    })
  );
});
