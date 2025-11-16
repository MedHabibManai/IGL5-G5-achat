import React, { useState } from 'react';
import ProduitList from './components/Produit/ProduitList';
import StockList from './components/Stock/StockList';
import FactureList from './components/Facture/FactureList';
import FournisseurList from './components/Fournisseur/FournisseurList';
import OperateurList from './components/Operateur/OperateurList';
import CategorieProduitList from './components/CategorieProduit/CategorieProduitList';
import ReglementList from './components/Reglement/ReglementList';
import SecteurActiviteList from './components/SecteurActivite/SecteurActiviteList';
import './App.css';

function App() {
  const [activeModule, setActiveModule] = useState('produit');

  const modules = [
    { id: 'produit', name: '🛍️ Produits', component: ProduitList },
    { id: 'stock', name: '📦 Stocks', component: StockList },
    { id: 'facture', name: '🧾 Factures', component: FactureList },
    { id: 'fournisseur', name: '🏢 Fournisseurs', component: FournisseurList },
    { id: 'operateur', name: '👤 Opérateurs', component: OperateurList },
    { id: 'categorie', name: '🏷️ Catégories', component: CategorieProduitList },
    { id: 'reglement', name: '💰 Règlements', component: ReglementList },
    { id: 'secteur', name: '🏭 Secteurs', component: SecteurActiviteList },
  ];

  const ActiveComponent = modules.find(m => m.id === activeModule)?.component || ProduitList;

  return (
    <div className="App">
      <header className="App-header">
        <h1>🏪 Achat Application</h1>
        <p>Gestion des Achats - Full Stack Application</p>
      </header>
      <nav className="App-nav">
        <div className="nav-container">
          {modules.map((module) => (
            <button
              key={module.id}
              className={`nav-button ${activeModule === module.id ? 'active' : ''}`}
              onClick={() => setActiveModule(module.id)}
            >
              {module.name}
            </button>
          ))}
        </div>
      </nav>
      <main>
        <ActiveComponent />
      </main>
      <footer className="App-footer">
        <p>© 2025 IGL5-G5-achat | Spring Boot + React + MySQL</p>
      </footer>
    </div>
  );
}

export default App;
