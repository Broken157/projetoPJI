function validId(value) {
  return typeof value === 'string' && /^[1-9][0-9]{0,18}$/.test(value)
    && (value.length < 19 || value <= '9223372036854775807');
}

// Normalize only known contexts. Never turn an arbitrary internal URL into a link.
export function notificationContext(value, origin = window.location.origin) {
  if (typeof value !== 'string' || !value || /[\s\\%]/.test(value)) return null;
  try {
    const url = new URL(value, `${origin}/`);
    if (url.origin !== origin || !['http:', 'https:'].includes(url.protocol)
      || url.username || url.password || url.hash) return null;
    const parameters = [...url.searchParams.entries()];
    const singleId = (name) => parameters.length === 1
      && parameters[0][0] === name && validId(parameters[0][1]);
    if (['/dashboard', '/dashboard-contratante.html'].includes(url.pathname)) {
      return parameters.length ? null : '/dashboard';
    }
    if (['/detalhe-vaga.html', '/detalhe-vaga-proprietario.html'].includes(url.pathname)) {
      return singleId('id')
        ? `/vagas/${url.searchParams.get('id')}${url.pathname.includes('proprietario') ? '/gerenciar' : ''}` : null;
    }
    if (url.pathname === '/mensagens') {
      return singleId('sala') ? `/mensagens?sala=${url.searchParams.get('sala')}` : null;
    }
    const vacancy = url.pathname.match(/^\/vagas\/([^/]+)(\/gerenciar)?$/);
    if (vacancy && !parameters.length && validId(vacancy[1])) return url.pathname;
    const profile = url.pathname.match(/^\/perfis\/(ARTISTA|CONTRATANTE)\/([^/]+)$/);
    if (profile && !parameters.length && validId(profile[2])) return url.pathname;
    if (url.pathname === '/perfil-publico.html' && parameters.length === 2
      && ['ARTISTA', 'CONTRATANTE'].includes(url.searchParams.get('tipo'))
      && validId(url.searchParams.get('id'))) {
      return `/perfis/${url.searchParams.get('tipo')}/${url.searchParams.get('id')}`;
    }
  } catch {
    return null;
  }
  return null;
}
