import { useEffect, useRef, useState } from 'react';
import ErrorState from '../../components/common/ErrorState';
import LoadingState from '../../components/common/LoadingState';
import {
  GENERIC_RECOVERY_MESSAGE,
  requestPasswordRecovery,
} from '../../services/auth/passwordRecoveryService';
import PasswordRecoveryLayout from './PasswordRecoveryLayout';

const TECHNICAL_ERROR = new Error('Tente novamente em instantes.');

export default function ForgotPasswordPage() {
  const [status, setStatus] = useState('idle');
  const requestInFlight = useRef(false);

  useEffect(() => {
    document.title = 'Recuperar senha — Palco';
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    const form = event.currentTarget;

    if (requestInFlight.current || !form.checkValidity()) {
      if (!form.checkValidity()) form.reportValidity();
      return;
    }

    requestInFlight.current = true;
    setStatus('loading');

    try {
      await requestPasswordRecovery(form.elements.email.value.trim());
      setStatus('success');
    } catch (error) {
      setStatus('error');
    } finally {
      requestInFlight.current = false;
    }
  }

  const submitting = status === 'loading';

  return (
    <PasswordRecoveryLayout>
      <main className="login">
        <div className="card login__card">
          <h1 className="login__titulo">
            Recuperar <span className="destaque-magenta">senha</span>
          </h1>

          <form
            className="login__form"
            noValidate
            aria-busy={submitting}
            onSubmit={handleSubmit}
          >
            <div className="campo">
              <label className="campo__rotulo" htmlFor="email-recuperacao">
                E-mail
              </label>
              <div className="campo__controle">
                <input
                  className="campo__input"
                  type="email"
                  id="email-recuperacao"
                  name="email"
                  placeholder="E-mail@example.com"
                  autoComplete="email"
                  maxLength={150}
                  required
                />
              </div>
            </div>

            <button
              className="btn btn--primario login__entrar"
              type="submit"
              disabled={submitting || status === 'success'}
            >
              Enviar instruções
            </button>

            {submitting ? (
              <LoadingState
                message="Enviando solicitação…"
                className="auth-recovery__state"
                indicatorClassName="react-state__indicator"
              />
            ) : null}

            {status === 'success' ? (
              <p className="auth-recovery__message" role="status" aria-live="polite">
                {GENERIC_RECOVERY_MESSAGE}
              </p>
            ) : null}

            {status === 'error' ? (
              <ErrorState
                error={TECHNICAL_ERROR}
                title="Não foi possível enviar a solicitação."
                className="auth-recovery__state auth-recovery__state--error"
                headingLevel="h2"
              />
            ) : null}

            <p className="login__cadastro">
              <a href="/login.html">Voltar ao login</a>
            </p>
          </form>
        </div>
      </main>
    </PasswordRecoveryLayout>
  );
}
