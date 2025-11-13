import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import './App.css';
import Home from './components/Home';
import Produits from './components/Produits';
import Stocks from './components/Stocks';
import Fournisseurs from './components/Fournisseurs';
import Factures from './components/Factures';
import Operateurs from './components/Operateurs';
import Reglements from './components/Reglements';

function App() {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <div className="nav-container">
            <Link to="/" className="nav-logo">
              🛒 Achat E-Commerce
            </Link>
            <div className={menuOpen ? "nav-menu active" : "nav-menu"}>
              <Link to="/" className="nav-link" onClick={() => setMenuOpen(false)}>Home</Link>
              <Link to="/produits" className="nav-link" onClick={() => setMenuOpen(false)}>Produits</Link>
              <Link to="/stocks" className="nav-link" onClick={() => setMenuOpen(false)}>Stocks</Link>
              <Link to="/fournisseurs" className="nav-link" onClick={() => setMenuOpen(false)}>Fournisseurs</Link>
              <Link to="/factures" className="nav-link" onClick={() => setMenuOpen(false)}>Factures</Link>
              <Link to="/operateurs" className="nav-link" onClick={() => setMenuOpen(false)}>Opérateurs</Link>
              <Link to="/reglements" className="nav-link" onClick={() => setMenuOpen(false)}>Règlements</Link>
            </div>
            <div className="nav-icon" onClick={() => setMenuOpen(!menuOpen)}>
              <div className="bar"></div>
              <div className="bar"></div>
              <div className="bar"></div>
            </div>
          </div>
        </nav>

        <main className="main-content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/produits" element={<Produits />} />
            <Route path="/stocks" element={<Stocks />} />
            <Route path="/fournisseurs" element={<Fournisseurs />} />
            <Route path="/factures" element={<Factures />} />
            <Route path="/operateurs" element={<Operateurs />} />
            <Route path="/reglements" element={<Reglements />} />
          </Routes>
        </main>

        <footer className="footer">
          <p>&copy; 2025 Achat E-Commerce Platform | IGL5-G5 Team</p>
        </footer>
      </div>
    </Router>
  );
}

export default App;

