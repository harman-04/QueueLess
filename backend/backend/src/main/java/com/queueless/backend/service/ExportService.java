package com.queueless.backend.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.QueueToken;
import com.queueless.backend.enums.TokenStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced service class for exporting queue data to PDF and Excel formats.
 * Produces professional, well-structured reports with comprehensive statistics.
 */
@Slf4j
@Service
public class ExportService {

    // Font definitions for PDF
    private static final com.itextpdf.text.Font PDF_TITLE_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
    private static final com.itextpdf.text.Font PDF_HEADER_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
    private static final com.itextpdf.text.Font PDF_SUBHEADER_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
    private static final com.itextpdf.text.Font PDF_NORMAL_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, BaseColor.BLACK);
    private static final com.itextpdf.text.Font PDF_BOLD_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.BLACK);
    private static final com.itextpdf.text.Font PDF_FOOTER_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BaseColor HEADER_BG_COLOR = new BaseColor(79, 129, 189);
    private static final BaseColor SUBHEADER_BG_COLOR = new BaseColor(220, 230, 241);

    /**
     * Exports queue data to a professionally formatted PDF document.
     */
    public byte[] exportQueueToPdf(Queue queue, String reportType, Boolean includeUserDetails) throws DocumentException {
        log.info("Starting PDF export for Queue ID: {} with report type: {}", queue.getId(), reportType);

        if (queue == null) {
            log.error("Attempted PDF export with a null Queue object.");
            throw new IllegalArgumentException("Queue object cannot be null for PDF export.");
        }

        Document document = new Document(PageSize.A4.rotate()); // Landscape for better table viewing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            // Add header and footer
            HeaderFooter event = new HeaderFooter(queue.getServiceName());
            writer.setPageEvent(event);

            document.open();

            // Add title section
            addTitleSection(document, queue);

            // Add summary statistics
            addSummarySection(document, queue);

            // Add content based on report type
            switch (reportType.toLowerCase()) {
                case "tokens":
                    addTokensSection(document, queue, includeUserDetails);
                    break;
                case "statistics":
                    addStatisticsSection(document, queue);
                    break;
                case "full":
                    addTokensSection(document, queue, includeUserDetails);
                    document.newPage();
                    addStatisticsSection(document, queue);
                    break;
                default:
                    log.error("Invalid report type '{}' requested for PDF export", reportType);
                    throw new IllegalArgumentException("Invalid report type: " + reportType);
            }

            document.close();
            log.info("PDF export completed successfully for Queue ID: {}", queue.getId());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error during PDF generation for Queue ID: {}", queue.getId(), e);
            throw new DocumentException("Failed to generate PDF: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /**
     * Exports queue data to a professionally formatted Excel document.
     */
    public byte[] exportQueueToExcel(Queue queue, String reportType, Boolean includeUserDetails) throws IOException {
        log.info("Starting Excel export for Queue ID: {} with report type: {}", queue.getId(), reportType);

        if (queue == null) {
            log.error("Attempted Excel export with a null Queue object.");
            throw new IllegalArgumentException("Queue object cannot be null for Excel export.");
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create different sheets based on report type
            switch (reportType.toLowerCase()) {
                case "tokens":
                    createTokensSheet(workbook, queue, includeUserDetails);
                    break;
                case "statistics":
                    createStatisticsSheet(workbook, queue);
                    break;
                case "full":
                    createTokensSheet(workbook, queue, includeUserDetails);
                    createStatisticsSheet(workbook, queue);
                    break;
                default:
                    log.error("Invalid report type '{}' requested for Excel export", reportType);
                    throw new IllegalArgumentException("Invalid report type: " + reportType);
            }

            workbook.write(outputStream);
            log.info("Excel export completed successfully for Queue ID: {}", queue.getId());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error during Excel generation for Queue ID: {}", queue.getId(), e);
            throw new IOException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }

    // ==================== PDF HELPER METHODS ====================

    private void addTitleSection(Document document, Queue queue) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("QUEUE MANAGEMENT REPORT", PDF_TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Queue info table
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 2});

        addInfoRow(infoTable, "Queue Name:", queue.getServiceName());
        addInfoRow(infoTable, "Queue ID:", queue.getId());
        addInfoRow(infoTable, "Status:", queue.getIsActive() ? "Active" : "Inactive");
        addInfoRow(infoTable, "Report Generated:", LocalDateTime.now().format(DATE_FORMATTER));

        document.add(infoTable);
        document.add(Chunk.NEWLINE);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, PDF_BOLD_FONT));
        labelCell.setBackgroundColor(SUBHEADER_BG_COLOR);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", PDF_NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSummarySection(Document document, Queue queue) throws DocumentException {
        // Calculate statistics
        List<QueueToken> tokens = queue.getTokens();
        long totalTokens = tokens.size();
        long waitingCount = tokens.stream().filter(t -> t.getStatus().equals(TokenStatus.WAITING.toString())).count();
        long inServiceCount = tokens.stream().filter(t -> t.getStatus().equals(TokenStatus.IN_SERVICE.toString())).count();
        long completedCount = tokens.stream().filter(t -> t.getStatus().equals(TokenStatus.COMPLETED.toString())).count();

        Paragraph summaryHeader = new Paragraph("Summary Statistics", PDF_SUBHEADER_FONT);
        summaryHeader.setSpacingAfter(10);
        document.add(summaryHeader);

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{1, 1, 1, 1});

        addSummaryCell(summaryTable, "Total Tokens", String.valueOf(totalTokens));
        addSummaryCell(summaryTable, "Waiting", String.valueOf(waitingCount));
        addSummaryCell(summaryTable, "In Service", String.valueOf(inServiceCount));
        addSummaryCell(summaryTable, "Completed", String.valueOf(completedCount));

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);
    }

    private void addSummaryCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);

        Paragraph labelPara = new Paragraph(label, PDF_NORMAL_FONT);
        labelPara.setAlignment(Element.ALIGN_CENTER);

        Paragraph valuePara = new Paragraph(value, PDF_BOLD_FONT);
        valuePara.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(labelPara);
        cell.addElement(valuePara);
        table.addCell(cell);
    }

    private void addTokensSection(Document document, Queue queue, Boolean includeUserDetails) throws DocumentException {
        Paragraph tokensHeader = new Paragraph("Token Details", PDF_SUBHEADER_FONT);
        tokensHeader.setSpacingAfter(10);
        document.add(tokensHeader);

        if (queue.getTokens().isEmpty()) {
            document.add(new Paragraph("No tokens found in this queue.", PDF_NORMAL_FONT));
            return;
        }

        // Create tokens table
        int columnCount = includeUserDetails ? 9 : 6;
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);

        // Table headers
        String[] headers = {"Token ID", "User ID", "Status", "Issue Time", "Service Time", "Complete Time"};
        if (includeUserDetails) {
            String[] userDetailHeaders = {"Purpose", "Condition", "Notes"};
            for (String header : userDetailHeaders) {
                headers = java.util.Arrays.copyOf(headers, headers.length + 1);
                headers[headers.length - 1] = header;
            }
        }

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, PDF_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Table data
        for (QueueToken token : queue.getTokens()) {
            addTokenRow(table, token, includeUserDetails);
        }

        document.add(table);
    }

    private void addTokenRow(PdfPTable table, QueueToken token, Boolean includeUserDetails) {
        table.addCell(createCell(token.getTokenId()));
        table.addCell(createCell(token.getUserId()));
        table.addCell(createCell(token.getStatus()));
        table.addCell(createCell(formatDate(token.getIssuedAt())));
        table.addCell(createCell(formatDate(token.getServedAt())));
        table.addCell(createCell(formatDate(token.getCompletedAt())));

        if (includeUserDetails && token.getUserDetails() != null) {
            table.addCell(createCell(token.getUserDetails().getPurpose()));
            table.addCell(createCell(token.getUserDetails().getCondition()));
            table.addCell(createCell(token.getUserDetails().getNotes()));
        } else if (includeUserDetails) {
            // Add empty cells if no user details
            table.addCell(createCell(""));
            table.addCell(createCell(""));
            table.addCell(createCell(""));
        }
    }

    private PdfPCell createCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "", PDF_NORMAL_FONT));
        cell.setPadding(5);
        return cell;
    }

    private void addStatisticsSection(Document document, Queue queue) throws DocumentException {
        Paragraph statsHeader = new Paragraph("Detailed Statistics", PDF_SUBHEADER_FONT);
        statsHeader.setSpacingAfter(10);
        document.add(statsHeader);

        // Calculate detailed statistics
        List<QueueToken> tokens = queue.getTokens();
        Map<String, Long> statusCounts = tokens.stream()
                .collect(Collectors.groupingBy(QueueToken::getStatus, Collectors.counting()));

        long avgWaitTime = calculateAverageWaitTime(tokens);

        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(50);
        statsTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        addStatRow(statsTable, "Total Tokens", String.valueOf(tokens.size()));

        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            addStatRow(statsTable, entry.getKey() + " Tokens", String.valueOf(entry.getValue()));
        }

        addStatRow(statsTable, "Average Wait Time", avgWaitTime + " minutes");
        addStatRow(statsTable, "Estimated Wait Time", queue.getEstimatedWaitTime() + " minutes");

        document.add(statsTable);
    }

    private void addStatRow(PdfPTable table, String metric, String value) {
        PdfPCell metricCell = new PdfPCell(new Phrase(metric, PDF_BOLD_FONT));
        metricCell.setBackgroundColor(SUBHEADER_BG_COLOR);
        metricCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, PDF_NORMAL_FONT));
        valueCell.setPadding(5);

        table.addCell(metricCell);
        table.addCell(valueCell);
    }

    private long calculateAverageWaitTime(List<QueueToken> tokens) {
        long totalWaitTime = 0;
        int count = 0;

        for (QueueToken token : tokens) {
            if (token.getServedAt() != null && token.getIssuedAt() != null) {
                long waitTime = java.time.Duration.between(token.getIssuedAt(), token.getServedAt()).toMinutes();
                totalWaitTime += waitTime;
                count++;
            }
        }

        return count > 0 ? totalWaitTime / count : 0;
    }

    // ==================== EXCEL HELPER METHODS ====================

    private void createTokensSheet(Workbook workbook, Queue queue, Boolean includeUserDetails) {
        Sheet sheet = workbook.createSheet("Token Details");

        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);

        // Add title row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Queue: " + queue.getServiceName());
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, includeUserDetails ? 8 : 5));

        // Add subtitle row
        Row subtitleRow = sheet.createRow(1);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Generated: " + LocalDateTime.now().format(DATE_FORMATTER));
        subtitleCell.setCellStyle(createSubtitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, includeUserDetails ? 8 : 5));

        // Add header row
        Row headerRow = sheet.createRow(3);
        String[] headers = {"Token ID", "User ID", "Status", "Issue Time", "Service Time", "Complete Time"};

        if (includeUserDetails) {
            headers = new String[]{"Token ID", "User ID", "Status", "Issue Time", "Service Time", "Complete Time",
                    "Purpose", "Condition", "Notes"};
        }

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 4;
        for (QueueToken token : queue.getTokens()) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(token.getTokenId());
            row.createCell(1).setCellValue(token.getUserId());
            row.createCell(2).setCellValue(token.getStatus());
            row.createCell(3).setCellValue(formatDate(token.getIssuedAt()));
            row.createCell(4).setCellValue(formatDate(token.getServedAt()));
            row.createCell(5).setCellValue(formatDate(token.getCompletedAt()));

            if (includeUserDetails) {
                if (token.getUserDetails() != null) {
                    row.createCell(6).setCellValue(token.getUserDetails().getPurpose());
                    row.createCell(7).setCellValue(token.getUserDetails().getCondition());
                    row.createCell(8).setCellValue(token.getUserDetails().getNotes());
                } else {
                    row.createCell(6).setCellValue("");
                    row.createCell(7).setCellValue("");
                    row.createCell(8).setCellValue("");
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createStatisticsSheet(Workbook workbook, Queue queue) {
        Sheet sheet = workbook.createSheet("Statistics");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        // Add title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Queue Statistics: " + queue.getServiceName());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Add summary header
        Row summaryHeaderRow = sheet.createRow(2);
        summaryHeaderRow.createCell(0).setCellValue("Metric");
        summaryHeaderRow.createCell(1).setCellValue("Value");
        summaryHeaderRow.getCell(0).setCellStyle(headerStyle);
        summaryHeaderRow.getCell(1).setCellStyle(headerStyle);

        // Calculate statistics
        List<QueueToken> tokens = queue.getTokens();
        Map<String, Long> statusCounts = tokens.stream()
                .collect(Collectors.groupingBy(QueueToken::getStatus, Collectors.counting()));

        long avgWaitTime = calculateAverageWaitTime(tokens);

        // Add summary data
        int rowNum = 3;
        addStatRow(sheet, rowNum++, "Total Tokens", String.valueOf(tokens.size()), boldStyle);

        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            addStatRow(sheet, rowNum++, entry.getKey() + " Tokens", String.valueOf(entry.getValue()), null);
        }

        addStatRow(sheet, rowNum++, "Average Wait Time", avgWaitTime + " minutes", null);
        addStatRow(sheet, rowNum++, "Estimated Wait Time", queue.getEstimatedWaitTime() + " minutes", null);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void addStatRow(Sheet sheet, int rowNum, String metric, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell metricCell = row.createCell(0);
        metricCell.setCellValue(metric);

        if (style != null) {
            metricCell.setCellStyle(style);
        }

        row.createCell(1).setCellValue(value);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    // ==================== UTILITY METHODS ====================

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }

    // ==================== PDF HEADER/FOOTER CLASS ====================

    class HeaderFooter extends PdfPageEventHelper {
        private String queueName;

        public HeaderFooter(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                // Add header
                PdfPTable header = new PdfPTable(1);
                header.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                header.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                header.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

                Paragraph headerText = new Paragraph(queueName, PDF_HEADER_FONT);
                headerText.getFont().setColor(BaseColor.DARK_GRAY);
                PdfPCell cell = new PdfPCell(headerText);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                header.addCell(cell);

                header.writeSelectedRows(0, -1, document.leftMargin(), document.getPageSize().getHeight() - 20, writer.getDirectContent());

                // Add footer
                PdfPTable footer = new PdfPTable(1);
                footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);
                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

                Paragraph footerText = new Paragraph("Page " + writer.getPageNumber(), PDF_FOOTER_FONT);
                cell = new PdfPCell(footerText);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                footer.addCell(cell);

                footer.writeSelectedRows(0, -1, document.leftMargin(), 30, writer.getDirectContent());
            } catch (Exception e) {
                log.error("Error adding header/footer to PDF", e);
            }
        }
    }
}