import axios from 'axios';

// Todas as requisições passam pelo API Gateway (ponto único de entrada)
const api = axios.create({ baseURL: 'http://localhost:8080/api' });

export const getProdutos = () => api.get('/produtos');
export const getCarrinho = (usuarioId: number) => api.get(`/carrinho/${usuarioId}`);
export const adicionarAoCarrinho = (usuarioId: number, item: any) => api.post(`/carrinho/${usuarioId}/adicionar`, item);
export const alterarQuantidadeItem = (usuarioId: number, itemId: number, quantidade: number) =>
  api.put(`/carrinho/${usuarioId}/itens/${itemId}?quantidade=${quantidade}`);
export const removerItemCarrinho = (usuarioId: number, itemId: number) =>
  api.delete(`/carrinho/${usuarioId}/itens/${itemId}`);
export const checkout = (usuarioId: number) => api.post(`/carrinho/${usuarioId}/checkout`);
export const processarPagamento = (pedidoId: number) =>
  api.post(`/pagamento/processar?pedidoId=${pedidoId}`);
