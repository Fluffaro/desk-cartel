package com.ticket.desk_cartel.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.ticket.desk_cartel.entities.*;
import com.ticket.desk_cartel.repositories.AgentRepository;
import com.ticket.desk_cartel.repositories.TicketRepository;
import com.ticket.desk_cartel.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating various reports about the ticket system.
 * Includes PDF report generation for tickets, agent performance, and system statistics.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    // Define common colors
    private static final Color LIGHT_GRAY = new Color(230, 230, 230);
    private static final Color GRAY = new Color(128, 128, 128);
    
    private final TicketRepository ticketRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    public ReportService(
            TicketRepository ticketRepository,
            AgentRepository agentRepository,
            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Generates a PDF report for all tickets in the system or filtered by criteria.
     * 
     * @param category Optional category filter
     * @param priority Optional priority filter
     * @param status Optional status filter
     * @return Byte array containing the PDF report
     * @throws Exception if PDF generation fails
     */
    public byte[] generateTicketsReport(Category category, Priority priority, Status status) throws Exception {
        List<Ticket> tickets = ticketService.filterTickets(category, priority, status);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // Add report title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
            Paragraph title = new Paragraph("Ticket Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);
            
            // Add generation timestamp
            Font timestampFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);
            Paragraph timestamp = new Paragraph("Generated on: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                    timestampFont);
            timestamp.setAlignment(Element.ALIGN_RIGHT);
            document.add(timestamp);
            document.add(Chunk.NEWLINE);
            
            // Add filter information if any
            if (category != null || priority != null || status != null) {
                Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
                Paragraph filters = new Paragraph("Filters applied:", filterFont);
                if (category != null) filters.add(" Category: " + category.getName());
                if (priority != null) filters.add(" Priority: " + priority.getName());
                if (status != null) filters.add(" Status: " + status.name());
                document.add(filters);
                document.add(Chunk.NEWLINE);
            }
            
            // Create the tickets table
            PdfPTable table = new PdfPTable(6); // 6 columns
            table.setWidthPercentage(100);
            
            // Set column widths
            float[] columnWidths = {1f, 2.5f, 1.5f, 1.5f, 1.5f, 2f};
            table.setWidths(columnWidths);
            
            // Add table headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL);
            addCell(table, "ID", headerFont, true);
            addCell(table, "Title", headerFont, true);
            addCell(table, "Priority", headerFont, true);
            addCell(table, "Status", headerFont, true);
            addCell(table, "Category", headerFont, true);
            addCell(table, "Created", headerFont, true);
            
            // Add ticket data to the table
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            for (Ticket ticket : tickets) {
                addCell(table, ticket.getTicketId().toString(), dataFont, false);
                addCell(table, ticket.getTitle(), dataFont, false);
                addCell(table, ticket.getPriority() != null ? ticket.getPriority().getName() : "N/A", dataFont, false);
                addCell(table, ticket.getStatus() != null ? ticket.getStatus().name() : "N/A", dataFont, false);
                addCell(table, ticket.getCategory() != null ? ticket.getCategory().getName() : "N/A", dataFont, false);
                addCell(table, ticket.getDate_started() != null ? 
                        ticket.getDate_started().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A", dataFont, false);
            }
            
            document.add(table);
            
            // Add statistics section
            document.add(Chunk.NEWLINE);
            Font statsTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL);
            document.add(new Paragraph("Ticket Statistics", statsTitleFont));
            document.add(Chunk.NEWLINE);
            
            // Count tickets by status
            Map<Status, Long> ticketsByStatus = tickets.stream()
                    .filter(t -> t.getStatus() != null)
                    .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));
            
            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(60);
            
            // Add status count headers
            addCell(statsTable, "Status", headerFont, true);
            addCell(statsTable, "Count", headerFont, true);
            
            // Add status counts
            for (Status s : Status.values()) {
                addCell(statsTable, s.name(), dataFont, false);
                addCell(statsTable, String.valueOf(ticketsByStatus.getOrDefault(s, 0L)), dataFont, false);
            }
            
            document.add(statsTable);
            
            // Add total count
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Total Tickets: " + tickets.size(), 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL)));
            
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Helper method to add a cell to a table with consistent styling
     */
    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (isHeader) {
            cell.setBackgroundColor(LIGHT_GRAY);
        }
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        // Add visible borders
        cell.setBorderWidth(1);
        table.addCell(cell);
    }
    
    /**
     * Calculates the average completion time of tickets in hours.
     * Can be filtered by category, priority, or within a date range.
     * 
     * @param category Optional category filter
     * @param priority Optional priority filter
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return A DTO containing average completion time and statistics
     */
    public Map<String, Object> calculateAverageCompletionTime(
            Category category, 
            Priority priority, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        // Get completed tickets
        List<Ticket> completedTickets = ticketRepository.findByStatus(Status.COMPLETED);
        
        // Apply filters
        if (category != null) {
            completedTickets = completedTickets.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().equals(category))
                .collect(Collectors.toList());
        }
        
        if (priority != null) {
            completedTickets = completedTickets.stream()
                .filter(t -> t.getPriority() != null && t.getPriority().equals(priority))
                .collect(Collectors.toList());
        }
        
        if (startDate != null) {
            completedTickets = completedTickets.stream()
                .filter(t -> t.getDate_started() != null && !t.getDate_started().isBefore(startDate))
                .collect(Collectors.toList());
        }
        
        if (endDate != null) {
            completedTickets = completedTickets.stream()
                .filter(t -> t.getCompletion_date() != null && !t.getCompletion_date().isAfter(endDate))
                .collect(Collectors.toList());
        }
        
        // Calculate statistics
        Map<String, Object> result = new HashMap<>();
        result.put("totalCompletedTickets", completedTickets.size());
        
        // Filter out tickets with no start or completion date
        List<Ticket> validTickets = completedTickets.stream()
            .filter(t -> t.getDate_started() != null && t.getCompletion_date() != null)
            .collect(Collectors.toList());
        
        if (validTickets.isEmpty()) {
            result.put("averageCompletionTimeHours", 0.0);
            result.put("minCompletionTimeHours", 0.0);
            result.put("maxCompletionTimeHours", 0.0);
            return result;
        }
        
        // Calculate average completion time in hours
        double avgCompletionTimeHours = validTickets.stream()
            .mapToDouble(t -> {
                LocalDateTime start = t.getDate_started();
                LocalDateTime end = t.getCompletion_date();
                // Duration in hours
                return java.time.Duration.between(start, end).toMinutes() / 60.0;
            })
            .average()
            .orElse(0.0);
        
        result.put("averageCompletionTimeHours", Math.round(avgCompletionTimeHours * 100.0) / 100.0);
        
        // Calculate min completion time
        double minCompletionTimeHours = validTickets.stream()
            .mapToDouble(t -> {
                LocalDateTime start = t.getDate_started();
                LocalDateTime end = t.getCompletion_date();
                return java.time.Duration.between(start, end).toMinutes() / 60.0;
            })
            .min()
            .orElse(0.0);
        
        result.put("minCompletionTimeHours", Math.round(minCompletionTimeHours * 100.0) / 100.0);
        
        // Calculate max completion time
        double maxCompletionTimeHours = validTickets.stream()
            .mapToDouble(t -> {
                LocalDateTime start = t.getDate_started();
                LocalDateTime end = t.getCompletion_date();
                return java.time.Duration.between(start, end).toMinutes() / 60.0;
            })
            .max()
            .orElse(0.0);
        
        result.put("maxCompletionTimeHours", Math.round(maxCompletionTimeHours * 100.0) / 100.0);
        
        // Add average completion time by priority
        Map<Priority, Double> avgByPriority = validTickets.stream()
            .filter(t -> t.getPriority() != null)
            .collect(Collectors.groupingBy(
                Ticket::getPriority,
                Collectors.averagingDouble(t -> {
                    LocalDateTime start = t.getDate_started();
                    LocalDateTime end = t.getCompletion_date();
                    return java.time.Duration.between(start, end).toMinutes() / 60.0;
                })
            ));
        
        Map<String, Double> avgByPriorityResult = new HashMap<>();
        avgByPriority.forEach((key, value) -> 
            avgByPriorityResult.put(key.getName(), Math.round(value * 100.0) / 100.0));
        
        result.put("averageCompletionTimeByPriority", avgByPriorityResult);
        
        // Add average completion time by category
        Map<Category, Double> avgByCategory = validTickets.stream()
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(
                Ticket::getCategory,
                Collectors.averagingDouble(t -> {
                    LocalDateTime start = t.getDate_started();
                    LocalDateTime end = t.getCompletion_date();
                    return java.time.Duration.between(start, end).toMinutes() / 60.0;
                })
            ));
        
        Map<String, Double> avgByCategoryResult = new HashMap<>();
        avgByCategory.forEach((key, value) -> 
            avgByCategoryResult.put(key.getName(), Math.round(value * 100.0) / 100.0));
        
        result.put("averageCompletionTimeByCategory", avgByCategoryResult);
        
        return result;
    }
    
    /**
     * Generates a PDF report for average ticket completion time statistics.
     * 
     * @param category Optional category filter
     * @param priority Optional priority filter
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Byte array containing the PDF report
     * @throws Exception if PDF generation fails
     */
    public byte[] generateCompletionTimeReport(
            Category category, 
            Priority priority, 
            LocalDateTime startDate, 
            LocalDateTime endDate) throws Exception {
        
        Map<String, Object> completionTimeStats = calculateAverageCompletionTime(
                category, priority, startDate, endDate);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // Add report title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
            Paragraph title = new Paragraph("Ticket Completion Time Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);
            
            // Add generation timestamp
            Font timestampFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);
            Paragraph timestamp = new Paragraph("Generated on: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                    timestampFont);
            timestamp.setAlignment(Element.ALIGN_RIGHT);
            document.add(timestamp);
            document.add(Chunk.NEWLINE);
            
            // Add filter information if any
            if (category != null || priority != null || startDate != null || endDate != null) {
                Font filterFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
                Paragraph filters = new Paragraph("Filters applied:", filterFont);
                if (category != null) filters.add(" Category: " + category.getName());
                if (priority != null) filters.add(" Priority: " + priority.getName());
                if (startDate != null) filters.add(" From: " + startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                if (endDate != null) filters.add(" To: " + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                document.add(filters);
                document.add(Chunk.NEWLINE);
            }
            
            // Add summary statistics
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL);
            document.add(new Paragraph("Completion Time Summary", sectionTitleFont));
            document.add(Chunk.NEWLINE);
            
            // Create a table for summary statistics
            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(70);
            
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL);
            
            // Add total count
            addCell(statsTable, "Total Completed Tickets", labelFont, false);
            addCell(statsTable, completionTimeStats.get("totalCompletedTickets").toString(), dataFont, false);
            
            // Add average completion time
            addCell(statsTable, "Average Completion Time (hours)", labelFont, false);
            addCell(statsTable, completionTimeStats.get("averageCompletionTimeHours").toString(), dataFont, false);
            
            // Add min completion time
            addCell(statsTable, "Minimum Completion Time (hours)", labelFont, false);
            addCell(statsTable, completionTimeStats.get("minCompletionTimeHours").toString(), dataFont, false);
            
            // Add max completion time
            addCell(statsTable, "Maximum Completion Time (hours)", labelFont, false);
            addCell(statsTable, completionTimeStats.get("maxCompletionTimeHours").toString(), dataFont, false);
            
            document.add(statsTable);
            document.add(Chunk.NEWLINE);
            
            // Add breakdown by priority
            @SuppressWarnings("unchecked")
            Map<String, Double> avgByPriority = (Map<String, Double>) completionTimeStats.get("averageCompletionTimeByPriority");
            
            if (avgByPriority != null && !avgByPriority.isEmpty()) {
                document.add(new Paragraph("Average Completion Time by Priority", sectionTitleFont));
                document.add(Chunk.NEWLINE);
                
                PdfPTable priorityTable = new PdfPTable(2);
                priorityTable.setWidthPercentage(70);
                
                // Add headers
                addCell(priorityTable, "Priority", labelFont, true);
                addCell(priorityTable, "Avg. Hours", labelFont, true);
                
                // Add data
                for (Map.Entry<String, Double> entry : avgByPriority.entrySet()) {
                    addCell(priorityTable, entry.getKey(), dataFont, false);
                    addCell(priorityTable, entry.getValue().toString(), dataFont, false);
                }
                
                document.add(priorityTable);
                document.add(Chunk.NEWLINE);
            }
            
            // Add breakdown by category
            @SuppressWarnings("unchecked")
            Map<String, Double> avgByCategory = (Map<String, Double>) completionTimeStats.get("averageCompletionTimeByCategory");
            
            if (avgByCategory != null && !avgByCategory.isEmpty()) {
                document.add(new Paragraph("Average Completion Time by Category", sectionTitleFont));
                document.add(Chunk.NEWLINE);
                
                PdfPTable categoryTable = new PdfPTable(2);
                categoryTable.setWidthPercentage(70);
                
                // Add headers
                addCell(categoryTable, "Category", labelFont, true);
                addCell(categoryTable, "Avg. Hours", labelFont, true);
                
                // Add data
                for (Map.Entry<String, Double> entry : avgByCategory.entrySet()) {
                    addCell(categoryTable, entry.getKey(), dataFont, false);
                    addCell(categoryTable, entry.getValue().toString(), dataFont, false);
                }
                
                document.add(categoryTable);
            }
            
        } finally {
            document.close();
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Other report generation methods would be implemented similarly.
     * For brevity, only the ticket report and completion time report are shown.
     */
} 