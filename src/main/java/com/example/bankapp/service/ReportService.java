package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.TransactionRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TransactionRepository transactionRepository;

    public byte[] generateCsv(Account account, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Transaction> txs = transactionRepository
                .findByAccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{"ID", "Timestamp", "Type", "Amount"});
            for (Transaction t : txs) {
                csv.writeNext(new String[]{
                        String.valueOf(t.getId()),
                        t.getTimestamp() != null ? t.getTimestamp().format(TS_FMT) : "",
                        t.getType() != null ? t.getType() : "",
                        t.getAmount() != null ? t.getAmount().toPlainString() : ""
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] generatePdf(Account account, LocalDate startDate, LocalDate endDate) {
        List<Transaction> txs = transactionRepository
                .findByAccountIdAndTimestampBetweenOrderByTimestampAsc(
                        account.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Paragraph title = new Paragraph("Transaction Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        document.add(new Paragraph("Account: " + account.getUsername() + " (#" + account.getId() + ")", metaFont));
        document.add(new Paragraph("Date range: " + startDate + " to " + endDate, metaFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1f, 3f, 3f, 2f});
        } catch (Exception ignored) {
        }

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        for (String h : new String[]{"ID", "Timestamp", "Type", "Amount"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, headFont));
            c.setBackgroundColor(new Color(122, 18, 22));
            c.setPadding(6f);
            table.addCell(c);
        }

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (Transaction t : txs) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(t.getId()), cellFont)));
            table.addCell(new PdfPCell(new Phrase(
                    t.getTimestamp() != null ? t.getTimestamp().format(TS_FMT) : "", cellFont)));
            table.addCell(new PdfPCell(new Phrase(t.getType() != null ? t.getType() : "", cellFont)));
            table.addCell(new PdfPCell(new Phrase(
                    t.getAmount() != null ? t.getAmount().toPlainString() : "", cellFont)));
        }

        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total transactions: " + txs.size(), metaFont));
        document.close();
        return baos.toByteArray();
    }
}
