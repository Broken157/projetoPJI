export function vacancyManagementError(error, fallback) {
  const byStatus = {
    403: 'Você não tem permissão para gerenciar esta vaga.',
    404: 'A vaga solicitada não foi encontrada.',
    409: 'A operação entrou em conflito com o estado atual da vaga.',
    422: error?.message || 'A operação não é permitida no estado atual da vaga.',
  };
  return byStatus[error?.status] || error?.message || fallback;
}
