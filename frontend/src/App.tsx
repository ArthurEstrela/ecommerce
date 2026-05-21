import React from 'react';
import ProductList from './components/ProductList';
import Cart from './components/Cart';

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-blue-600 text-white p-6 shadow-md">
        <h1 className="text-3xl font-bold text-center">E-commerce Distribuído</h1>
      </header>
      <main className="container mx-auto mt-8">
        <ProductList />
        <Cart />
      </main>
      <footer className="bg-gray-800 text-white p-4 mt-20 text-center">
        <p>&copy; 2026 E-commerce Microservices Demo</p>
      </footer>
    </div>
  );
}

export default App;
