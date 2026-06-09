import React, { useCallback, useState } from 'react';
import ProductList from './components/ProductList';
import Cart from './components/Cart';

function App() {
  const [cartCount, setCartCount] = useState(0);
  const [usuarioId, setUsuarioId] = useState(1);

  const handleAddToCart = useCallback(() => {
    setCartCount(c => c + 1);
  }, []);

  const handleCartUpdate = useCallback((count: number) => {
    setCartCount(count);
  }, []);

  const handleUsuarioChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const nextUsuarioId = Math.max(1, Number(event.target.value) || 1);
    setUsuarioId(nextUsuarioId);
    setCartCount(0);
  };

  return (
    <div className="app-container">
      <div className="bg-orbs"></div>
      
      <header className="navbar">
        <div className="navbar-inner">
          <div className="navbar-brand">
            <div className="navbar-logo">SF</div>
            <div>
              <h1 className="navbar-title">ShopFlow</h1>
              <span className="navbar-subtitle">E-commerce Distribuído</span>
            </div>
          </div>
          
          <div className="navbar-actions">
            <label className="user-switcher">
              <span>Usuário</span>
              <input
                type="number"
                min="1"
                value={usuarioId}
                onChange={handleUsuarioChange}
                aria-label="ID do usuário"
              />
            </label>

            <div className="navbar-status">
              <div className="status-dot"></div>
              <span>Sistema Online</span>
            </div>
            
            <div className="cart-badge">
              <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path>
              </svg>
              <span>Carrinho</span>
              <span className="cart-count">{cartCount}</span>
            </div>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="arch-banner">
          <div className="arch-item">
            <div className="arch-icon arch-icon-react">
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"></path></svg>
            </div>
            <div>
              <div className="arch-label">Frontend</div>
              <div className="arch-value">React + UI Premium</div>
            </div>
          </div>
          <div className="arch-item">
            <div className="arch-icon arch-icon-grpc">
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            </div>
            <div>
              <div className="arch-label">Comunicação</div>
              <div className="arch-value">gRPC (Alta Performance)</div>
            </div>
          </div>
          <div className="arch-item">
            <div className="arch-icon arch-icon-rabbit">
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"></path></svg>
            </div>
            <div>
              <div className="arch-label">Mensageria</div>
              <div className="arch-value">RabbitMQ (Filas/PubSub)</div>
            </div>
          </div>
          <div className="arch-item">
            <div className="arch-icon arch-icon-eureka">
              <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9"></path></svg>
            </div>
            <div>
              <div className="arch-label">Discovery</div>
              <div className="arch-value">Netflix Eureka</div>
            </div>
          </div>
        </div>

        <div className="layout-two-col">
          <ProductList usuarioId={usuarioId} onAddToCart={handleAddToCart} />
          <Cart usuarioId={usuarioId} onCartUpdate={handleCartUpdate} />
        </div>
      </main>

      <footer className="footer">
        <div className="footer-text">
          <p>© 2026 ShopFlow. Projeto acadêmico de Sistemas Distribuídos.</p>
          <p style={{ marginTop: '0.5rem', color: 'var(--text-muted)' }}>Demonstrando Arquitetura de Microsserviços com gRPC, RabbitMQ e Service Discovery.</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
