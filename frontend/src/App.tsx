import React from 'react';
import ProductList from './components/ProductList';
import Cart from './components/Cart';

function App() {
  return (
    <div style={{minHeight:'100vh', background:'#f8fafc'}}>
      <header style={{background:'#2563eb', color:'white', padding:'1.5rem', boxShadow:'0 2px 8px rgba(0,0,0,0.15)'}}>
        <h1 style={{textAlign:'center', fontSize:'2rem', fontWeight:'bold', margin:0}}>E-commerce Distribuido</h1>
      </header>
      <main style={{maxWidth:'1200px', margin:'2rem auto', padding:'0 1rem'}}>
        <ProductList />
        <Cart />
      </main>
      <footer style={{background:'#1e293b', color:'white', padding:'1rem', marginTop:'4rem', textAlign:'center'}}>
        <p>2026 E-commerce Microservices Demo</p>
      </footer>
    </div>
  );
}

export default App;