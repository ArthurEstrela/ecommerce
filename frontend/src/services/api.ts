import axios from 'axios';

// Todas as requisições passam pelo API Gateway (ponto único de entrada)
const api = axios.create({ baseURL: 'http://localhost:8080/api' });

export const getProdutos = () => api.get('/produtos');
export const getCarrinho = (usuarioId: number) => api.get(`/carrinho/${usuarioId}`);
export const adicionarAoCarrinho = (usuarioId: number, item: any) => api.post(`/carrinho/${usuarioId}/adicionar`, item);
export const checkout = (usuarioId: number) => api.post(`/carrinho/${usuarioId}/checkout`);

