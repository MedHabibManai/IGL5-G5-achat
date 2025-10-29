package tn.esprit.rh.achat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Reglement;
import tn.esprit.rh.achat.services.IReglementService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ReglementRestController
 * Tests all REST endpoints for Reglement management
 */
@WebMvcTest(ReglementRestController.class)
class ReglementRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IReglementService reglementService;

    private Reglement reglement1;
    private Reglement reglement2;
    private Facture facture;

    @BeforeEach
    void setUp() {
        // Initialize test facture
        facture = new Facture();
        facture.setIdFacture(1L);
        facture.setMontantFacture(1000f);

        // Initialize test reglements
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15);

        reglement1 = new Reglement();
        reglement1.setIdReglement(1L);
        reglement1.setMontantPaye(500f);
        reglement1.setMontantRestant(500f);
        reglement1.setPayee(false);
        reglement1.setDateReglement(cal.getTime());
        reglement1.setFacture(facture);

        cal.set(2024, Calendar.FEBRUARY, 15);
        reglement2 = new Reglement();
        reglement2.setIdReglement(2L);
        reglement2.setMontantPaye(500f);
        reglement2.setMontantRestant(0f);
        reglement2.setPayee(true);
        reglement2.setDateReglement(cal.getTime());
        reglement2.setFacture(facture);
    }

    @Test
    void testGetReglement_Success() throws Exception {
        // Arrange
        List<Reglement> reglements = new ArrayList<>();
        reglements.add(reglement1);
        reglements.add(reglement2);
        when(reglementService.retrieveAllReglements()).thenReturn(reglements);

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieve-all-reglements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idReglement", is(1)))
                .andExpect(jsonPath("$[0].montantPaye", is(500.0)))
                .andExpect(jsonPath("$[1].idReglement", is(2)))
                .andExpect(jsonPath("$[1].payee", is(true)));

        verify(reglementService, times(1)).retrieveAllReglements();
    }

    @Test
    void testGetReglement_EmptyList() throws Exception {
        // Arrange
        when(reglementService.retrieveAllReglements()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieve-all-reglements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(reglementService, times(1)).retrieveAllReglements();
    }

    @Test
    void testRetrieveReglement_Success() throws Exception {
        // Arrange
        when(reglementService.retrieveReglement(1L)).thenReturn(reglement1);

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieve-reglement/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idReglement", is(1)))
                .andExpect(jsonPath("$.montantPaye", is(500.0)))
                .andExpect(jsonPath("$.montantRestant", is(500.0)));

        verify(reglementService, times(1)).retrieveReglement(1L);
    }

    @Test
    void testRetrieveReglement_NotFound() throws Exception {
        // Arrange
        when(reglementService.retrieveReglement(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieve-reglement/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(reglementService, times(1)).retrieveReglement(999L);
    }

    @Test
    void testAddReglement_Success() throws Exception {
        // Arrange
        Reglement newReglement = new Reglement();
        newReglement.setMontantPaye(300f);
        newReglement.setMontantRestant(200f);
        newReglement.setPayee(false);

        Reglement savedReglement = new Reglement();
        savedReglement.setIdReglement(3L);
        savedReglement.setMontantPaye(300f);
        savedReglement.setMontantRestant(200f);
        savedReglement.setPayee(false);

        when(reglementService.addReglement(any(Reglement.class))).thenReturn(savedReglement);

        // Act & Assert
        mockMvc.perform(post("/reglement/add-reglement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReglement)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.idReglement", is(3)))
                .andExpect(jsonPath("$.montantPaye", is(300.0)))
                .andExpect(jsonPath("$.montantRestant", is(200.0)));

        verify(reglementService, times(1)).addReglement(any(Reglement.class));
    }

    @Test
    void testRetrieveReglementByFacture_Success() throws Exception {
        // Arrange
        List<Reglement> reglements = new ArrayList<>();
        reglements.add(reglement1);
        reglements.add(reglement2);
        when(reglementService.retrieveReglementByFacture(1L)).thenReturn(reglements);

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieveReglementByFacture/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idReglement", is(1)))
                .andExpect(jsonPath("$[1].idReglement", is(2)));

        verify(reglementService, times(1)).retrieveReglementByFacture(1L);
    }

    @Test
    void testRetrieveReglementByFacture_NoReglements() throws Exception {
        // Arrange
        when(reglementService.retrieveReglementByFacture(999L)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/reglement/retrieveReglementByFacture/999"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(reglementService, times(1)).retrieveReglementByFacture(999L);
    }

    @Test
    void testGetChiffreAffaireEntreDeuxDate_Success() throws Exception {
        // Arrange
        when(reglementService.getChiffreAffaireEntreDeuxDate(any(Date.class), any(Date.class))).thenReturn(1000f);

        // Act & Assert
        mockMvc.perform(get("/reglement/getChiffreAffaireEntreDeuxDate/2024-01-01/2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.0"));

        verify(reglementService, times(1)).getChiffreAffaireEntreDeuxDate(any(Date.class), any(Date.class));
    }

    @Test
    void testGetChiffreAffaireEntreDeuxDate_Exception() throws Exception {
        // Arrange
        when(reglementService.getChiffreAffaireEntreDeuxDate(any(Date.class), any(Date.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/reglement/getChiffreAffaireEntreDeuxDate/2024-01-01/2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));

        verify(reglementService, times(1)).getChiffreAffaireEntreDeuxDate(any(Date.class), any(Date.class));
    }
}

