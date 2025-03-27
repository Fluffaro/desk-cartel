package com.ticket.desk_cartel.services;

import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.exceptions.ResourceAlreadyExistsException;
import com.ticket.desk_cartel.exceptions.ResourceNotFoundException;
import com.ticket.desk_cartel.repositories.PriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PriorityServiceTest {

    @Mock
    private PriorityRepository priorityRepository;

    @InjectMocks
    private PriorityService priorityService;

    private Priority priority;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        priority = new Priority("HIGH", 30, 24);
    }

    @Test
    void testInitializeDefaultPriorities() {
        when(priorityRepository.count()).thenReturn(0L);
        priorityService.initializeDefaultPriorities();
        verify(priorityRepository, times(5)).save(any(Priority.class));
    }

    @Test
    void testGetAllPriorities() {
        Priority priority1 = new Priority("LOW", 10, 4);
        Priority priority2 = new Priority("Medium", 20, 8);

        when(priorityRepository.findAll()).thenReturn(Arrays.asList(priority1, priority2));

        assertEquals(2, priorityService.getAllPriorities().size());
        assertTrue(priorityService.getAllPriorities().contains(priority1));
        assertTrue(priorityService.getAllPriorities().contains(priority2));
    }

    @Test
    void testGetPriorityById_found() {
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        Priority foundPriority = priorityService.getPriorityById(1L);

        assertNotNull(foundPriority);
        assertEquals("HIGH", foundPriority.getName());
    }

    @Test
    void testGetPriorityByName_found() {
        when(priorityRepository.findByName("HIGH")).thenReturn(Optional.of(priority));
        assertThrows(ResourceNotFoundException.class, () -> priorityService.getPriorityById(1L));
    }

    @Test
    void testGetPriorityByName_notFound() {
        when(priorityRepository.findByName("HIGH")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> priorityService.getPriorityByName("HIGH"));
    }
    @Test
    void testCreatePriority_success() {
        when(priorityRepository.existsByName("HIGH")).thenReturn(false);
        when(priorityRepository.save(priority)).thenReturn(priority);

        Priority createdPriority = priorityService.createPriority(priority);

        assertNotNull(createdPriority);
        assertEquals("HIGH", createdPriority.getName());
        verify(priorityRepository, times(1)).save(priority);
    }

    @Test
    void testCreatePriority_alreadyExists() {
        when(priorityRepository.existsByName("HIGH")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> priorityService.createPriority(priority));
    }

    @Test
    void testUpdatePriority_alreadyExists() {
        when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));
        when(priorityRepository.existsByName("UPDATED")).thenReturn(true);

        Priority updatedPriority = new Priority("UPDATED", 40, 12);

        assertThrows(ResourceAlreadyExistsException.class, () -> priorityService.updatePriority(1L, updatedPriority));
    }

    @Test
    void testDeletePriority_success() {
        when(priorityRepository.existsById(1L)).thenReturn(true);

        priorityService.deletePriority(1L);

        verify(priorityRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeletePriority_notFound() {
        when(priorityRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> priorityService.deletePriority(1L));
    }
}