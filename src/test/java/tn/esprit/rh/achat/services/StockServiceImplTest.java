package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh.achat.entities.Stock;
import tn.esprit.rh.achat.repositories.StockRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock stock1;
    private Stock stock2;
    private Stock stock3;

    @BeforeEach
    void setUp() {
        stock1 = new Stock();
        stock1.setIdStock(1L);
        stock1.setLibelleStock("Stock A");
        stock1.setQte(100);
        stock1.setQteMin(20);

        stock2 = new Stock();
        stock2.setIdStock(2L);
        stock2.setLibelleStock("Stock B");
        stock2.setQte(50);
        stock2.setQteMin(30);

        stock3 = new Stock();
        stock3.setIdStock(3L);
        stock3.setLibelleStock("Stock C - Low");
        stock3.setQte(10);
        stock3.setQteMin(50);
    }

    @Test
    void testRetrieveAllStocks_Success() {
        // Arrange
        List<Stock> stocks = Arrays.asList(stock1, stock2, stock3);
        when(stockRepository.findAll()).thenReturn(stocks);

        // Act
        List<Stock> result = stockService.retrieveAllStocks();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(stockRepository, times(1)).findAll();
    }

    @Test
    void testRetrieveAllStocks_EmptyList() {
        // Arrange
        when(stockRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<Stock> result = stockService.retrieveAllStocks();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(stockRepository, times(1)).findAll();
    }

    @Test
    void testAddStock() {
        // Arrange
        when(stockRepository.save(any(Stock.class))).thenReturn(stock1);

        // Act
        Stock result = stockService.addStock(stock1);

        // Assert
        assertNotNull(result);
        assertEquals("Stock A", result.getLibelleStock());
        assertEquals(100, result.getQte());
        assertEquals(20, result.getQteMin());
        verify(stockRepository, times(1)).save(stock1);
    }

    @Test
    void testDeleteStock() {
        // Arrange
        doNothing().when(stockRepository).deleteById(anyLong());

        // Act
        stockService.deleteStock(1L);

        // Assert
        verify(stockRepository, times(1)).deleteById(1L);
    }

    @Test
    void testUpdateStock() {
        // Arrange
        stock1.setQte(150);
        when(stockRepository.save(any(Stock.class))).thenReturn(stock1);

        // Act
        Stock result = stockService.updateStock(stock1);

        // Assert
        assertNotNull(result);
        assertEquals(150, result.getQte());
        verify(stockRepository, times(1)).save(stock1);
    }

    @Test
    void testRetrieveStock_Success() {
        // Arrange
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock1));

        // Act
        Stock result = stockService.retrieveStock(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdStock());
        assertEquals("Stock A", result.getLibelleStock());
        verify(stockRepository, times(1)).findById(1L);
    }

    @Test
    void testRetrieveStock_NotFound() {
        // Arrange
        when(stockRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Stock result = stockService.retrieveStock(999L);

        // Assert
        assertNull(result);
        verify(stockRepository, times(1)).findById(999L);
    }

    @Test
    void testRetrieveStatusStock_WithLowStocks() {
        // Arrange
        List<Stock> lowStocks = Arrays.asList(stock3);
        when(stockRepository.retrieveStatusStock()).thenReturn(lowStocks);

        // Act
        String result = stockService.retrieveStatusStock();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Stock C - Low"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("50"));
        verify(stockRepository, times(1)).retrieveStatusStock();
    }

    @Test
    void testRetrieveStatusStock_NoLowStocks() {
        // Arrange
        when(stockRepository.retrieveStatusStock()).thenReturn(new ArrayList<>());

        // Act
        String result = stockService.retrieveStatusStock();

        // Assert
        assertNotNull(result);
        assertEquals("", result);
        verify(stockRepository, times(1)).retrieveStatusStock();
    }

    @Test
    void testRetrieveStatusStock_MultipleLowStocks() {
        // Arrange
        Stock stock4 = new Stock();
        stock4.setIdStock(4L);
        stock4.setLibelleStock("Stock D - Low");
        stock4.setQte(5);
        stock4.setQteMin(25);

        List<Stock> lowStocks = Arrays.asList(stock3, stock4);
        when(stockRepository.retrieveStatusStock()).thenReturn(lowStocks);

        // Act
        String result = stockService.retrieveStatusStock();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Stock C - Low"));
        assertTrue(result.contains("Stock D - Low"));
        verify(stockRepository, times(1)).retrieveStatusStock();
    }
}

