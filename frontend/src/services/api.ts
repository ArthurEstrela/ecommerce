import axios from 'axios';

const apiProduto = axios.create({ baseURL: 'http://localhost:8081/api' });
const apiCarrinho = axios.create({ baseURL: 'http://localhost:8083/api' });

export const getProdutos = () => apiProduto.get('/produtos');
export const getCarrinho = (usuarioId: number) => apiCarrinho.get(`/carrinho/${usuarioId}`);
export const adicionarAoCarrinho = (usuarioId: number, item: any) => apiCarrinho.post(`/carrinho/${usuarioId}/adicionar`, item);
export const checkout = (usuarioId: number) => apiCarrinho.post(`/carrinho/${usuarioId}/checkout`);
