package com.ticket.desk_cartel.controllers;

import com.ticket.desk_cartel.entities.Category;
import com.ticket.desk_cartel.entities.Priority;
import com.ticket.desk_cartel.entities.Status;
import com.ticket.desk_cartel.repositories.CategoryRepository;
import com.ticket.desk_cartel.repositories.PriorityRepository;
import com.ticket.desk_cartel.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for generating and retrieving various reports about the ticket system.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    private final ReportService reportService;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    
    @Autowired
    public ReportController(
            ReportService reportService,
            CategoryRepository categoryRepository,
            PriorityRepository priorityRepository) {
        this.reportService = reportService;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
    }
    
    /**
     * Generate a PDF report for tickets, with optional filtering by category, priority, and status.
     */
    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
        summary = "Generate ticket report",
        description = "Generates a PDF report of tickets, optionally filtered by category, priority, or status."
    )
    @ApiResponse(responseCode = "200", description = "PDF report generated successfully")
    @ApiResponse(responseCode = "500", description = "Error generating report")
    public ResponseEntity<byte[]> generateTicketsReport(
            @Parameter(description = "Category ID for filtering")
            @RequestParam(required = false) Long categoryId,
            
            @Parameter(description = "Priority ID for filtering")
            @RequestParam(required = false) Long priorityId,
            
            @Parameter(description = "Status for filtering")
            @RequestParam(required = false) Status status) {
        
        try {
            Category category = categoryId != null ? 
                    categoryRepository.findById(categoryId).orElse(null) : null;
            Priority priority = priorityId != null ? 
                    priorityRepository.findById(priorityId).orElse(null) : null;
            
            byte[] pdfContent = reportService.generateTicketsReport(category, priority, status);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "tickets-report.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating tickets PDF report", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Calculate and return average ticket completion time statistics.
     * Can be filtered by category, priority, or date range.
     */
    @GetMapping("/completion-time/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
        summary = "Get ticket completion time statistics",
        description = "Returns JSON statistics about average ticket completion time, optionally filtered."
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @ApiResponse(responseCode = "500", description = "Error retrieving statistics")
    public ResponseEntity<Map<String, Object>> getAverageCompletionTime(
            @Parameter(description = "Category ID for filtering")
            @RequestParam(required = false) Long categoryId,
            
            @Parameter(description = "Priority ID for filtering")
            @RequestParam(required = false) Long priorityId,
            
            @Parameter(description = "Start date for filtering (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date for filtering (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Category category = categoryId != null ? 
                    categoryRepository.findById(categoryId).orElse(null) : null;
            Priority priority = priorityId != null ? 
                    priorityRepository.findById(priorityId).orElse(null) : null;
            
            Map<String, Object> stats = reportService.calculateAverageCompletionTime(
                    category, priority, startDate, endDate);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error calculating average completion time", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Generate a PDF report for ticket completion time statistics.
     * Can be filtered by category, priority, or date range.
     */
    @GetMapping("/completion-time/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(
        summary = "Generate completion time PDF report",
        description = "Generates a PDF report about ticket completion time statistics, optionally filtered."
    )
    @ApiResponse(responseCode = "200", description = "PDF report generated successfully")
    @ApiResponse(responseCode = "500", description = "Error generating report")
    public ResponseEntity<byte[]> generateCompletionTimeReport(
            @Parameter(description = "Category ID for filtering")
            @RequestParam(required = false) Long categoryId,
            
            @Parameter(description = "Priority ID for filtering")
            @RequestParam(required = false) Long priorityId,
            
            @Parameter(description = "Start date for filtering (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date for filtering (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Category category = categoryId != null ? 
                    categoryRepository.findById(categoryId).orElse(null) : null;
            Priority priority = priorityId != null ? 
                    priorityRepository.findById(priorityId).orElse(null) : null;
            
            byte[] pdfContent = reportService.generateCompletionTimeReport(
                    category, priority, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "completion-time-report.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating completion time PDF report", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 