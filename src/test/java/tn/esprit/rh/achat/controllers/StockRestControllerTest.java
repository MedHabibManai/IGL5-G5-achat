package tn.esprit.rh.achat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rh.achat.entities.Stock;
import tn.esprit.rh.achat.services.IStockService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockRestController.class)
class StockRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IStockService stockService;

    @Autowired
    private ObjectMapper objectMapper;

    private Stock stock1;
    private Stock stock2;

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
    }

    @Test
    void testGetStocks_Success() throws Exception {
        // Arrange
        List<Stock> stocks = Arrays.asList(stock1, stock2);
        when(stockService.retrieveAllStocks()).thenReturn(stocks);

        // Act & Assert
        mockMvc.perform(get("/stock/retrieve-all-stocks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].libelleStock").value("Stock A"))
                .andExpect(jsonPath("$[0].qte").value(100))
                .andExpect(jsonPath("$[1].libelleStock").value("Stock B"));

        verify(stockService, times(1)).retrieveAllStocks();
    }

    @Test
    void testGetStocks_EmptyList() throws Exception {
        // Arrange
        when(stockService.retrieveAllStocks()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/stock/retrieve-all-stocks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(stockService, times(1)).retrieveAllStocks();
    }

    @Test
    void testRetrieveStock_Success() throws Exception {
        // Arrange
        when(stockService.retrieveStock(1L)).thenReturn(stock1);

        // Act & Assert
        mockMvc.perform(get("/stock/retrieve-stock/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idStock").value(1))
                .andExpect(jsonPath("$.libelleStock").value("Stock A"))
                .andExpect(jsonPath("$.qte").value(100))
                .andExpect(jsonPath("$.qteMin").value(20));

        verify(stockService, times(1)).retrieveStock(1L);
    }

    @Test
    void testRetrieveStock_NotFound() throws Exception {
        // Arrange
        when(stockService.retrieveStock(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/stock/retrieve-stock/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(stockService, times(1)).retrieveStock(999L);
    }

    @Test
    void testAddStock() throws Exception {
        // Arrange
        Stock newStock = new Stock();
        newStock.setLibelleStock("New Stock");
        newStock.setQte(200);
        newStock.setQteMin(40);

        Stock savedStock = new Stock();
        savedStock.setIdStock(3L);
        savedStock.setLibelleStock("New Stock");
        savedStock.setQte(200);
        savedStock.setQteMin(40);

        when(stockService.addStock(any(Stock.class))).thenReturn(savedStock);

        // Act & Assert
        mockMvc.perform(post("/stock/add-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStock)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idStock").value(3))
                .andExpect(jsonPath("$.libelleStock").value("New Stock"))
                .andExpect(jsonPath("$.qte").value(200))
                .andExpect(jsonPath("$.qteMin").value(40));

        verify(stockService, times(1)).addStock(any(Stock.class));
    }

    @Test
    void testRemoveStock() throws Exception {
        // Arrange
        doNothing().when(stockService).deleteStock(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/stock/remove-stock/1"))
                .andExpect(status().isOk());

        verify(stockService, times(1)).deleteStock(1L);
    }

    @Test
    void testModifyStock() throws Exception {
        // Arrange
        stock1.setQte(150);
        when(stockService.updateStock(any(Stock.class))).thenReturn(stock1);

        // Act & Assert
        mockMvc.perform(put("/stock/modify-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stock1)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idStock").value(1))
                .andExpect(jsonPath("$.qte").value(150));

        verify(stockService, times(1)).updateStock(any(Stock.class));
    }
}

