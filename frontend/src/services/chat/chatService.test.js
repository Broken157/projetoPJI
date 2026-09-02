import apiClient from '../api/apiClient';
import {
  createRoom,
  deleteMessage,
  editMessage,
  listMessages,
  listRooms,
  markRoomRead,
  messagesUrl,
  sendMessage,
} from './chatService';

jest.mock('../api/apiClient', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), patch: jest.fn(), delete: jest.fn() },
}));

beforeEach(() => jest.clearAllMocks());

test('usa apiClient e o contrato REST real do chat', async () => {
  apiClient.get.mockResolvedValue({ content: [] });
  apiClient.post.mockResolvedValue({ salaId: 7 });
  apiClient.patch.mockResolvedValue(null);
  apiClient.delete.mockResolvedValue(null);

  await listRooms(1, 20);
  await createRoom(12);
  await listMessages(7, 2, 20);
  await sendMessage(7, 'Olá');
  await markRoomRead(7);
  await editMessage(31, 'Editada');
  await deleteMessage(31);

  expect(apiClient.get).toHaveBeenNthCalledWith(1, '/chat/salas?page=1&size=20');
  expect(apiClient.post).toHaveBeenNthCalledWith(1, '/chat/salas', { usuarioDestinoId: 12 });
  expect(apiClient.get).toHaveBeenNthCalledWith(2, '/chat/salas/7/mensagens?page=2&size=20');
  expect(apiClient.post).toHaveBeenNthCalledWith(2, '/chat/salas/7/mensagens', { texto: 'Olá' });
  expect(apiClient.patch).toHaveBeenNthCalledWith(1, '/chat/salas/7/lidas');
  expect(apiClient.patch).toHaveBeenNthCalledWith(2, '/chat/mensagens/31', { texto: 'Editada' });
  expect(apiClient.delete).toHaveBeenCalledWith('/chat/mensagens/31');
  expect(messagesUrl(7)).toBe('/mensagens?sala=7');
});

test('rejeita IDs inválidos antes de chamar a API', () => {
  expect(() => createRoom('terceiro')).toThrow('Usuário de destino inválido');
  expect(() => listMessages('../9')).toThrow('Sala inválido');
  expect(() => deleteMessage(0)).toThrow('Mensagem inválido');
  expect(apiClient.get).not.toHaveBeenCalled();
  expect(apiClient.post).not.toHaveBeenCalled();
});
