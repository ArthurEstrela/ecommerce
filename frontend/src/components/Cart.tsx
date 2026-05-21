import React, { useEffect, useState } from 'react';
import { getCarrinho, checkout } from '../services/api';

interface Item {
  id: number;
  produtoId: number;
  quantidade: number;
  precoUnitario: number;
}

const Cart: React.FC = () => {
  const [itens, setItens] = useState<Item[]>([]);
  const [msg, setMsg] = useState('');
  const usuarioId = 1;

  useEffect(() => {
    getCarrinho(usuarioId).then(res => setItens(res.data.itens));
  }, []);

  const handleCheckout = () => {
    checkout(usuarioId).then(res => {
      setMsg(res.data);
      setItens([]);
    });
  };

  return (
    <div className="p-6 border-t mt-10">
      <h2 className="text-2xl font-bold mb-4">Meu Carrinho</h2>
      {itens.length === 0 ? (
        <p className="text-gray-500">O carrinho está vazio.</p>
      ) : (
        <div>
          <ul className="space-y-4">
            {itens.map(item => (
              <li key={item.id} className="flex justify-between border-b pb-2">
                <span>Produto #{item.produtoId} (x{item.quantidade})</span>
                <span className="font-semibold">R$ {(item.precoUnitario * item.quantidade).toFixed(2)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-6 flex justify-between items-center">
            <span className="text-xl font-bold">Total: R$ {itens.reduce((acc, i) => acc + (i.precoUnitario * i.quantidade), 0).toFixed(2)}</span>
            <button 
              onClick={handleCheckout}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
            >
              Finalizar Compra
            </button>
          </div>
        </div>
      )}
      {msg && <div className="mt-4 p-4 bg-yellow-100 text-yellow-800 rounded">{msg}</div>}
    </div>
  );
};

export default Cart;
