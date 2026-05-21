import React, { useEffect, useState } from 'react';
import { getProdutos, adicionarAoCarrinho } from '../services/api';

interface Produto {
  id: number;
  nome: string;
  descricao: string;
  preco: number;
}

const ProductList: React.FC = () => {
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const usuarioId = 1; // Simulação de usuário logado

  useEffect(() => {
    getProdutos().then(res => setProdutos(res.data));
  }, []);

  const handleAddToCart = (produto: Produto) => {
    adicionarAoCarrinho(usuarioId, {
      produtoId: produto.id,
      quantidade: 1,
      precoUnitario: produto.preco
    }).then(() => alert(`${produto.nome} adicionado ao carrinho!`));
  };

  return (
    <div className="p-6">
      <h2 className="text-2xl font-bold mb-4">Catálogo de Produtos</h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {produtos.map(p => (
          <div key={p.id} className="border p-4 rounded shadow-sm hover:shadow-md transition">
            <h3 className="font-semibold text-lg">{p.nome}</h3>
            <p className="text-gray-600">{p.descricao}</p>
            <p className="text-blue-600 font-bold mt-2">R$ {p.preco.toFixed(2)}</p>
            <button 
              onClick={() => handleAddToCart(p)}
              className="mt-4 w-full bg-green-500 text-white py-2 rounded hover:bg-green-600 transition"
            >
              Adicionar ao Carrinho
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProductList;
