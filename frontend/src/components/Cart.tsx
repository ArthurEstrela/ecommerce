import React, { useEffect, useState, useCallback } from 'react';
import {
  alterarQuantidadeItem,
  checkout,
  getCarrinho,
  getProdutos,
  processarPagamento,
  removerItemCarrinho
} from '../services/api';

interface Item {
  id: number;
  produtoId: number;
  quantidade: number;
  precoUnitario: number;
}

interface Produto {
  id: number;
  nome: string;
}

interface CartProps {
  usuarioId: number;
  onCartUpdate: (count: number) => void;
}

const getProductEmoji = (id: number) => {
  const emojis = ['📱', '💻', '🎧', '⌚', '🎮', '📷', '🖥️', '⌨️', '🖱️'];
  return emojis[(id - 1) % emojis.length] || '📦';
};

const sortCartItems = (items: Item[]) => [...items].sort((a, b) => a.id - b.id);
const getPedidoIdFromMessage = (message: string) => {
  const match = message.match(/Pedido #(\d+)/);
  return match ? Number(match[1]) : null;
};

const Cart: React.FC<CartProps> = ({ usuarioId, onCartUpdate }) => {
  const [itens, setItens] = useState<Item[]>([]);
  const [produtos, setProdutos] = useState<Record<number, Produto>>({});
  const [loading, setLoading] = useState(true);
  const [checkoutLoading, setCheckoutLoading] = useState(false);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [successMsg, setSuccessMsg] = useState('');
  const [pendingPedidoId, setPendingPedidoId] = useState<number | null>(null);

  const loadCart = useCallback(() => {
    setLoading(true);
    getCarrinho(usuarioId)
      .then(res => {
        const cartItems = sortCartItems(res.data.itens || []);
        setItens(cartItems);
        onCartUpdate(cartItems.reduce((acc: number, item: Item) => acc + item.quantidade, 0));
        setLoading(false);
      })
      .catch(err => {
        console.error("Erro ao buscar carrinho", err);
        setLoading(false);
      });
  }, [onCartUpdate, usuarioId]);

  useEffect(() => {
    getProdutos()
      .then(res => {
        const productMap = (res.data || []).reduce((acc: Record<number, Produto>, produto: Produto) => {
          acc[produto.id] = produto;
          return acc;
        }, {});
        setProdutos(productMap);
      })
      .catch(err => console.error("Erro ao buscar produtos para o carrinho", err));
  }, []);

  useEffect(() => {
    setItens([]);
    setSuccessMsg('');
    setPendingPedidoId(null);
    onCartUpdate(0);
    loadCart();
    
    const interval = setInterval(() => {
      getCarrinho(usuarioId).then(res => {
        const cartItems = sortCartItems(res.data.itens || []);
        setItens(cartItems);
        onCartUpdate(cartItems.reduce((acc: number, item: Item) => acc + item.quantidade, 0));
      }).catch(() => {});
    }, 2000);
    
    return () => clearInterval(interval);
  }, [loadCart, usuarioId, onCartUpdate]);

  const handleCheckout = () => {
    setCheckoutLoading(true);
    checkout(usuarioId)
      .then(res => {
        setSuccessMsg(res.data);
        setPendingPedidoId(getPedidoIdFromMessage(res.data));
        setItens([]);
        onCartUpdate(0);
      })
      .catch(err => {
        alert("Erro no checkout: " + (err.response?.data || err.message));
      })
      .finally(() => {
        setCheckoutLoading(false);
      });
  };

  const handleQuantityChange = (item: Item, quantidade: number) => {
    if (quantidade <= 0) {
      handleRemoveItem(item.id);
      return;
    }

    setUpdatingItemId(item.id);
    alterarQuantidadeItem(usuarioId, item.id, quantidade)
      .then(res => {
        const cartItems = sortCartItems(res.data.itens || []);
        setItens(cartItems);
        onCartUpdate(cartItems.reduce((acc: number, cartItem: Item) => acc + cartItem.quantidade, 0));
      })
      .catch(err => {
        alert("Erro ao alterar quantidade: " + (err.response?.data?.message || err.response?.data || err.message));
      })
      .finally(() => setUpdatingItemId(null));
  };

  const handleRemoveItem = (itemId: number) => {
    setUpdatingItemId(itemId);
    removerItemCarrinho(usuarioId, itemId)
      .then(res => {
        const cartItems = sortCartItems(res.data.itens || []);
        setItens(cartItems);
        onCartUpdate(cartItems.reduce((acc: number, cartItem: Item) => acc + cartItem.quantidade, 0));
      })
      .catch(err => {
        alert("Erro ao remover item: " + (err.response?.data?.message || err.response?.data || err.message));
      })
      .finally(() => setUpdatingItemId(null));
  };

  const handleProcessPayment = () => {
    if (!pendingPedidoId) return;

    setPaymentLoading(true);
    processarPagamento(pendingPedidoId)
      .then(res => {
        setSuccessMsg(`${res.data} Pedido #${pendingPedidoId} atualizado para PAGO.`);
        setPendingPedidoId(null);
      })
      .catch(err => {
        alert("Erro ao processar pagamento: " + (err.response?.data?.message || err.response?.data || err.message));
      })
      .finally(() => setPaymentLoading(false));
  };

  const total = itens.reduce((acc, i) => acc + (i.precoUnitario * i.quantidade), 0);

  return (
    <aside>
      <div className="cart-section">
        <h2 className="section-title" style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Meu <span>Carrinho</span></h2>
        
        {loading && itens.length === 0 ? (
          <div className="cart-items">
            {[1, 2].map(i => (
              <div key={i} className="cart-item" style={{ border: 'none' }}>
                <div className="cart-item-info">
                  <div className="skeleton" style={{ width: '42px', height: '42px', borderRadius: '8px' }}></div>
                  <div>
                    <div className="skeleton" style={{ width: '100px', height: '16px', marginBottom: '4px' }}></div>
                    <div className="skeleton" style={{ width: '60px', height: '12px' }}></div>
                  </div>
                </div>
                <div className="skeleton" style={{ width: '50px', height: '20px' }}></div>
              </div>
            ))}
          </div>
        ) : itens.length === 0 ? (
          <div className="cart-empty">
            <div className="cart-empty-icon">🛒</div>
            <p className="cart-empty-text">Seu carrinho está vazio.<br/>Adicione produtos para continuar.</p>
          </div>
        ) : (
          <>
            <div className="cart-items">
              {itens.map(item => (
                <div key={item.id} className="cart-item">
                  <div className="cart-item-info">
                    <div className="cart-item-icon">{getProductEmoji(item.produtoId)}</div>
                    <div className="cart-item-details">
                      <h4>{produtos[item.produtoId]?.nome || `Produto #${item.produtoId}`}</h4>
                      <span>Produto #{item.produtoId}</span>
                      <div className="cart-item-controls">
                        <button
                          type="button"
                          className="quantity-btn"
                          onClick={() => handleQuantityChange(item, item.quantidade - 1)}
                          disabled={updatingItemId === item.id}
                          aria-label="Diminuir quantidade"
                        >
                          -
                        </button>
                        <span className="quantity-value">{item.quantidade}</span>
                        <button
                          type="button"
                          className="quantity-btn"
                          onClick={() => handleQuantityChange(item, item.quantidade + 1)}
                          disabled={updatingItemId === item.id}
                          aria-label="Aumentar quantidade"
                        >
                          +
                        </button>
                        <button
                          type="button"
                          className="remove-item-btn"
                          onClick={() => handleRemoveItem(item.id)}
                          disabled={updatingItemId === item.id}
                        >
                          Remover
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="cart-item-price">
                    R$ {(item.precoUnitario * item.quantidade).toFixed(2)}
                  </div>
                </div>
              ))}
            </div>
            
            <hr className="cart-divider" />
            
            <div className="cart-summary">
              <span className="cart-total-label">Total a pagar:</span>
              <span className="cart-total-value">R$ {total.toFixed(2)}</span>
            </div>
            
            <button 
              onClick={handleCheckout}
              disabled={checkoutLoading || itens.length === 0}
              className="btn btn-checkout"
            >
              {checkoutLoading ? (
                <>
                  <span className="spinner" style={{ width: '18px', height: '18px', marginRight: '8px' }}></span>
                  Processando via gRPC...
                </>
              ) : (
                <>
                  <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                  Finalizar Compra
                </>
              )}
            </button>
          </>
        )}
        
        {successMsg && (
          <div className="success-banner">
            <div className="success-banner-icon">🎉</div>
            <div className="success-banner-text">
              {successMsg}
              {pendingPedidoId && (
                <button
                  type="button"
                  className="btn btn-payment"
                  onClick={handleProcessPayment}
                  disabled={paymentLoading}
                >
                  {paymentLoading ? 'Processando pagamento...' : `Processar pagamento do pedido #${pendingPedidoId}`}
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </aside>
  );
};

export default Cart;
