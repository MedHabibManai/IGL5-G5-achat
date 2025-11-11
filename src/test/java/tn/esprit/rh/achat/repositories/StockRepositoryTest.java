package tn.esprit.rh.achat.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tn.esprit.rh.achat.entities.Stock;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StockRepository stockRepository;

    private Stock stock1;
    private Stock stock2;
    private Stock stock3;

    @BeforeEach
    void setUp() {
        // Create test stocks
        stock1 = new Stock();
        stock1.setLibelleStock("Stock A - Normal");
        stock1.setQte(100);
        stock1.setQteMin(20);
        entityManager.persist(stock1);

        stock2 = new Stock();
        stock2.setLibelleStock("Stock B - Normal");
        stock2.setQte(50);
        stock2.setQteMin(30);
        entityManager.persist(stock2);

        stock3 = new Stock();
        stock3.setLibelleStock("Stock C - Low");
        stock3.setQte(10);
        stock3.setQteMin(50);
        entityManager.persist(stock3);

        entityManager.flush();
    }

    @Test
    void testRetrieveStatusStock_FindsLowStocks() {
        // Act
        List<Stock> lowStocks = stockRepository.retrieveStatusStock();

        // Assert
        assertNotNull(lowStocks);
        assertEquals(1, lowStocks.size());
        assertEquals("Stock C - Low", lowStocks.get(0).getLibelleStock());
        assertEquals(10, lowStocks.get(0).getQte());
        assertEquals(50, lowStocks.get(0).getQteMin());
    }

    @Test
    void testRetrieveStatusStock_MultipleLowStocks() {
        // Arrange - Add another low stock
        Stock stock4 = new Stock();
        stock4.setLibelleStock("Stock D - Low");
        stock4.setQte(5);
        stock4.setQteMin(25);
        entityManager.persist(stock4);
        entityManager.flush();

        // Act
        List<Stock> lowStocks = stockRepository.retrieveStatusStock();

        // Assert
        assertNotNull(lowStocks);
        assertEquals(2, lowStocks.size());
    }

    @Test
    void testRetrieveStatusStock_NoLowStocks() {
        // Arrange - Remove the low stock
        entityManager.remove(stock3);
        entityManager.flush();

        // Act
        List<Stock> lowStocks = stockRepository.retrieveStatusStock();

        // Assert
        assertNotNull(lowStocks);
        assertEquals(0, lowStocks.size());
    }

    @Test
    void testSaveStock() {
        // Arrange
        Stock newStock = new Stock();
        newStock.setLibelleStock("New Stock");
        newStock.setQte(200);
        newStock.setQteMin(40);

        // Act
        Stock savedStock = stockRepository.save(newStock);

        // Assert
        assertNotNull(savedStock);
        assertNotNull(savedStock.getIdStock());
        assertEquals("New Stock", savedStock.getLibelleStock());
        assertEquals(200, savedStock.getQte());
        assertEquals(40, savedStock.getQteMin());
    }

    @Test
    void testFindById_Success() {
        // Act
        Optional<Stock> found = stockRepository.findById(stock1.getIdStock());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Stock A - Normal", found.get().getLibelleStock());
    }

    @Test
    void testFindById_NotFound() {
        // Act
        Optional<Stock> found = stockRepository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        // Act
        List<Stock> stocks = (List<Stock>) stockRepository.findAll();

        // Assert
        assertNotNull(stocks);
        assertEquals(3, stocks.size());
    }

    @Test
    void testDeleteStock() {
        // Arrange
        Long stockId = stock1.getIdStock();

        // Act
        stockRepository.deleteById(stockId);
        entityManager.flush();

        // Assert
        Optional<Stock> deleted = stockRepository.findById(stockId);
        assertFalse(deleted.isPresent());
    }

    @Test
    void testUpdateStock() {
        // Arrange
        stock1.setQte(150);
        stock1.setLibelleStock("Updated Stock A");

        // Act
        Stock updated = stockRepository.save(stock1);
        entityManager.flush();

        // Assert
        assertNotNull(updated);
        assertEquals(150, updated.getQte());
        assertEquals("Updated Stock A", updated.getLibelleStock());
    }

    @Test
    void testRetrieveStatusStock_EdgeCase_EqualQuantities() {
        // Arrange - Create stock where qte equals qteMin
        Stock stock4 = new Stock();
        stock4.setLibelleStock("Stock D - Equal");
        stock4.setQte(30);
        stock4.setQteMin(30);
        entityManager.persist(stock4);
        entityManager.flush();

        // Act
        List<Stock> lowStocks = stockRepository.retrieveStatusStock();

        // Assert - Should not include stock4 since qte is not less than qteMin
        assertNotNull(lowStocks);
        assertEquals(1, lowStocks.size());
        assertEquals("Stock C - Low", lowStocks.get(0).getLibelleStock());
    }
}

