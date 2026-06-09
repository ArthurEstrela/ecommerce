import React, { useEffect, useState } from 'react';
import { getProdutos, adicionarAoCarrinho } from '../services/api';

interface Produto {
  id: number;
  nome: string;
  descricao: string;
  preco: number;
  estoque: number;
}

interface ProductListProps {
  usuarioId: number;
  onAddToCart: () => void;
}

// Map product ID to an emoji for better visual
const getProductEmoji = (id: number) => {
  const emojis = ['📱', '💻', '🎧', '⌚', '🎮', '📷', '🖥️', '⌨️', '🖱️'];
  return emojis[(id - 1) % emojis.length] || '📦';
};

const ProductList: React.FC<ProductListProps> = ({ usuarioId, onAddToCart }) => {
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [loading, setLoading] = useState(true);
  const [addingId, setAddingId] = useState<number | null>(null);

  useEffect(() => {
    getProdutos()
      .then(res => {
        setProdutos(res.data);
        setLoading(false);
      })
      .catch(err => {
        console.error("Erro ao buscar produtos", err);
        setLoading(false);
      });
  }, []);

  const showToast = (message: string, type: 'success' | 'info' | 'error') => {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type === 'error' ? 'info' : type}`;
    
    const icon = type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ');
    
    toast.innerHTML = `
      <div class="toast-icon">${icon}</div>
      <div>${message}</div>
    `;
    
    toastContainer.appendChild(toast);
    
    setTimeout(() => {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 3000);
  };

  const handleAddToCart = (produto: Produto) => {
    if (produto.estoque <= 0) {
      showToast(`${produto.nome} está sem estoque`, 'info');
      return;
    }

    setAddingId(produto.id);
    adicionarAoCarrinho(usuarioId, {
      produtoId: produto.id,
      quantidade: 1
    }).then(() => {
      onAddToCart();
      showToast(`${produto.nome} adicionado ao carrinho!`, 'success');
    }).catch(err => {
      showToast(`Erro ao adicionar ${produto.nome}`, 'error');
    }).finally(() => {
      setAddingId(null);
    });
  };

  return (
    <section>
      <div className="section-header">
        <h2 className="section-title">Nossos <span>Produtos</span></h2>
        <p className="section-description">Catálogo distribuído com alta disponibilidade.</p>
      </div>

      {loading ? (
        <div className="product-grid">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <div key={i} className="product-card">
              <div className="skeleton" style={{ width: '60px', height: '60px', marginBottom: '1rem', borderRadius: '12px' }}></div>
              <div className="skeleton" style={{ width: '80%', height: '24px', marginBottom: '0.5rem' }}></div>
              <div className="skeleton" style={{ width: '100%', height: '16px', marginBottom: '1rem' }}></div>
              <div className="skeleton" style={{ width: '50%', height: '32px', marginTop: 'auto' }}></div>
            </div>
          ))}
        </div>
      ) : (
        <div className="product-grid">
          {produtos.map(p => (
            <div key={p.id} className="product-card">
              <span className="product-emoji">{getProductEmoji(p.id)}</span>
              <h3 className="product-name">{p.nome}</h3>
              <p className="product-description">{p.descricao}</p>
              
              <div className="product-footer">
                <div>
                  <div className="product-price-label">Preço Unitário</div>
                  <div className="product-price">R$ {p.preco.toFixed(2)}</div>
                </div>
                <div className="product-stock">
                  <div className={`stock-indicator ${p.estoque <= 0 ? 'stock-indicator-empty' : ''}`}></div>
                  {p.estoque > 0 ? `${p.estoque} em estoque` : 'Sem estoque'}
                </div>
              </div>
              
              <button 
                onClick={() => handleAddToCart(p)}
                disabled={addingId === p.id || p.estoque <= 0}
                className="btn btn-add-cart"
              >
                {addingId === p.id ? (
                  <span className="spinner" style={{ width: '16px', height: '16px', borderWidth: '2px', margin: '0 auto' }}></span>
                ) : (
                  <>
                    <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
                    Adicionar
                  </>
                )}
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
};

export default ProductList;
