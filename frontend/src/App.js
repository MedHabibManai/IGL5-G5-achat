import React from 'react';
import ProduitList from './components/Produit/ProduitList';
import './App.css';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>🏪 Achat Application</h1>
        <p>Gestion des Achats - Full Stack Application</p>
      </header>
      <main>
        <ProduitList />
      </main>
      <footer className="App-footer">
        <p>© 2025 IGL5-G5-achat | Spring Boot + React + MySQL</p>
      </footer>
    </div>
  );
}

export default App;
