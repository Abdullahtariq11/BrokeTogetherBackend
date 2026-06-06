package com.broketogether.api.service;

import com.broketogether.api.model.Expense;
import com.broketogether.api.model.Home;
import com.broketogether.api.model.User;
import com.broketogether.api.repository.ExpenseRepository;
import com.broketogether.api.repository.HomeRepository;
import com.broketogether.api.utility.Utility;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.security.auth.login.AccountNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ExportService extends Utility {

    private final HomeRepository homeRepository;
    private final ExpenseRepository expenseRepository;

    public ExportService(HomeRepository homeRepository, ExpenseRepository expenseRepository) {
        this.homeRepository = homeRepository;
        this.expenseRepository = expenseRepository;
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    public byte[] exportCsv(Long homeId) throws AccountNotFoundException, IOException {
        List<Expense> expenses = getExpensesForPremiumUser(homeId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .builder()
                .setHeader("Date", "Description", "Category", "Amount", "Paid By")
                .build());

        for (Expense expense : expenses) {
            printer.printRecord(
                    expense.getCreatedAt() != null ? expense.getCreatedAt().toLocalDate() : "N/A",
                    expense.getDescription(),
                    expense.getCategory() != null ? expense.getCategory() : "General",
                    expense.getAmount(),
                    expense.getPayer() != null ? expense.getPayer().getName() : "Unknown"
            );
        }

        printer.flush();
        return out.toByteArray();
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    public byte[] exportPdf(Long homeId) throws AccountNotFoundException {
        Home home = homeRepository.findById(homeId)
                .orElseThrow(() -> new RuntimeException("Home not found."));
        List<Expense> expenses = getExpensesForPremiumUser(homeId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(pdfWriter);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph(home.getName() + " — Expense Report")
                .setBold().setFontSize(18).setMarginBottom(10));

        // Table with 5 columns
        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 25, 15, 15, 25}))
                .useAllAvailableWidth();

        // Header row
        for (String header : new String[]{"Date", "Description", "Category", "Amount", "Paid By"}) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
        }

        // Data rows
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            table.addCell(expense.getCreatedAt() != null
                    ? expense.getCreatedAt().toLocalDate().toString() : "N/A");
            table.addCell(expense.getDescription() != null ? expense.getDescription() : "");
            table.addCell(expense.getCategory() != null ? expense.getCategory() : "General");
            table.addCell("$" + expense.getAmount());
            table.addCell(expense.getPayer() != null ? expense.getPayer().getName() : "Unknown");
            total = total.add(expense.getAmount());
        }

        document.add(table);

        // Total
        document.add(new Paragraph("Total: $" + total)
                .setBold().setFontSize(13).setMarginTop(10));

        document.close();
        return out.toByteArray();
    }

    // ── Shared helper ─────────────────────────────────────────────────────────

    private List<Expense> getExpensesForPremiumUser(Long homeId) throws AccountNotFoundException {
        User user = getUserDetails();

        if (!user.getPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Upgrade to Premium to export reports.");
        }

        Home home = homeRepository.findById(homeId)
                .orElseThrow(() -> new RuntimeException("Home not found."));

        if (!home.getMembers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a member of this home.");
        }

        return expenseRepository.findByHomeId(homeId);
    }
}
