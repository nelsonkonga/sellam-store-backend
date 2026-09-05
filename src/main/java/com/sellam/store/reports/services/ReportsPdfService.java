package com.sellam.store.reports.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sellam.store.reports.dto.ReportsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportsPdfService {

    private static final Logger log = LoggerFactory.getLogger(ReportsPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un PDF pour le rapport synthétique
     */
    public byte[] generateSummaryReportPdf(String shopName, LocalDate startDate, LocalDate endDate, ReportsDTO.SummaryReport report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Paragraph title = new Paragraph("Rapport Synthétique", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Période
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            // Tableau de résumé
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Chiffre d'affaires total", normalFont, true);
            addTableCell(table, formatCurrency(report.getTotalRevenue()), normalFont, false);
            addTableCell(table, "Marge totale", normalFont, true);
            addTableCell(table, formatCurrency(report.getTotalMargin()), normalFont, false);
            addTableCell(table, "Nombre de factures", normalFont, true);
            addTableCell(table, String.valueOf(report.getInvoiceCount()), normalFont, false);
            addTableCell(table, "Panier moyen", normalFont, true);
            addTableCell(table, formatCurrency(report.getAverageBasket()), normalFont, false);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF de rapport synthétique", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la performance des produits
     */
    public byte[] generateProductPerformancePdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.TopProductItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Performance des Produits", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Produit", titleFont, true);
            addTableCell(table, "Quantité vendue", titleFont, true);
            addTableCell(table, "Marge générée", titleFont, true);

            for (ReportsDTO.TopProductItem item : items) {
                addTableCell(table, item.getProductName(), normalFont, false);
                addTableCell(table, item.getQuantitySold().toString(), normalFont, false);
                addTableCell(table, formatCurrency(item.getMarginGenerated()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF de performance produits", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la performance par catégorie
     */
    public byte[] generateCategoryPerformancePdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.CategoryPerformanceItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Performance par Catégorie", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            addTableCell(table, "Catégorie", titleFont, true);
            addTableCell(table, "Chiffre d'affaires", titleFont, true);
            addTableCell(table, "Marge", titleFont, true);
            addTableCell(table, "Quantité vendue", titleFont, true);

            for (ReportsDTO.CategoryPerformanceItem item : items) {
                addTableCell(table, item.getCategory(), normalFont, false);
                addTableCell(table, formatCurrency(item.getRevenue()), normalFont, false);
                addTableCell(table, formatCurrency(item.getMargin()), normalFont, false);
                addTableCell(table, item.getQuantitySold().toString(), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF de performance catégories", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la performance des employés
     */
    public byte[] generateEmployeePerformancePdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.EmployeePerformanceItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Performance des Employés", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Employé", titleFont, true);
            addTableCell(table, "Chiffre d'affaires", titleFont, true);
            addTableCell(table, "Nombre de factures", titleFont, true);

            for (ReportsDTO.EmployeePerformanceItem item : items) {
                addTableCell(table, item.getEmployeeName(), normalFont, false);
                addTableCell(table, formatCurrency(item.getTotalRevenue()), normalFont, false);
                addTableCell(table, String.valueOf(item.getInvoiceCount()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF de performance employés", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la fiabilité de caisse
     */
    public byte[] generateCashReliabilityPdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.CashReliabilityItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Fiabilité de Caisse", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Date", titleFont, true);
            addTableCell(table, "Écart de caisse", titleFont, true);

            for (ReportsDTO.CashReliabilityItem item : items) {
                addTableCell(table, item.getDate().format(DATE_FORMATTER), normalFont, false);
                addTableCell(table, formatCurrency(item.getDiscrepancy()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF de fiabilité caisse", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour les ventes à marge négative
     */
    public byte[] generateNegativeMarginSalesPdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.NegativeMarginSaleItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph("Ventes à Marge Négative", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Parge: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);

            addTableCell(table, "Produit", headerFont, true);
            addTableCell(table, "Quantité", headerFont, true);
            addTableCell(table, "Prix vente", headerFont, true);
            addTableCell(table, "Prix achat", headerFont, true);
            addTableCell(table, "Marge", headerFont, true);
            addTableCell(table, "Date", headerFont, true);

            for (ReportsDTO.NegativeMarginSaleItem item : items) {
                addTableCell(table, item.getProductName(), normalFont, false);
                addTableCell(table, item.getQuantity().toString(), normalFont, false);
                addTableCell(table, formatCurrency(item.getUnitSellingPrice()), normalFont, false);
                addTableCell(table, formatCurrency(item.getUnitPurchasePrice()), normalFont, false);
                addTableCell(table, formatCurrency(item.getMargin()), normalFont, false);
                addTableCell(table, item.getDate(), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF ventes marge négative", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour le stock mort
     */
    public byte[] generateDeadStockPdf(String shopName, List<ReportsDTO.DeadStockItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Stock Mort (Produits inactifs)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph shop = new Paragraph("Boutique: " + shopName, normalFont);
            document.add(shop);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Produit", titleFont, true);
            addTableCell(table, "Stock disponible", titleFont, true);
            addTableCell(table, "Jours depuis dernière vente", titleFont, true);

            for (ReportsDTO.DeadStockItem item : items) {
                addTableCell(table, item.getProductName(), normalFont, false);
                addTableCell(table, item.getStockQuantity().toString(), normalFont, false);
                addTableCell(table, item.getDaysSinceLastSale() + " jours", normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF stock mort", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la performance par moyen de paiement
     */
    public byte[] generatePaymentMethodPerformancePdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.PaymentMethodItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Performance par Moyen de Paiement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Moyen de paiement", titleFont, true);
            addTableCell(table, "Chiffre d'affaires", titleFont, true);
            addTableCell(table, "Nombre de factures", titleFont, true);

            for (ReportsDTO.PaymentMethodItem item : items) {
                addTableCell(table, item.getMethod(), normalFont, false);
                addTableCell(table, formatCurrency(item.getTotalRevenue()), normalFont, false);
                addTableCell(table, String.valueOf(item.getInvoiceCount()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF moyens de paiement", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour les alertes de rupture de stock
     */
    public byte[] generateStockOutAlertsPdf(String shopName, List<ReportsDTO.StockOutAlertItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph("Alertes de Rupture de Stock", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph shop = new Paragraph("Boutique: " + shopName, normalFont);
            document.add(shop);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            addTableCell(table, "Produit", headerFont, true);
            addTableCell(table, "Stock actuel", headerFont, true);
            addTableCell(table, "Vitesse quotidienne", headerFont, true);
            addTableCell(table, "Jours restants", headerFont, true);

            for (ReportsDTO.StockOutAlertItem item : items) {
                addTableCell(table, item.getProductName(), normalFont, false);
                addTableCell(table, item.getCurrentStock().toString(), normalFont, false);
                addTableCell(table, item.getDailyVelocity().toString(), normalFont, false);
                addTableCell(table, item.getEstimatedDaysRemaining() + " jours", normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF alertes rupture stock", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour les produits achetés ensemble
     */
    public byte[] generateFrequentlyBoughtTogetherPdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.FrequentlyBoughtTogetherItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Produits Fréquemment Achetés Ensemble", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Produit A", titleFont, true);
            addTableCell(table, "Produit B", titleFont, true);
            addTableCell(table, "Occurrences", titleFont, true);

            for (ReportsDTO.FrequentlyBoughtTogetherItem item : items) {
                addTableCell(table, item.getProductA(), normalFont, false);
                addTableCell(table, item.getProductB(), normalFont, false);
                addTableCell(table, String.valueOf(item.getPairOccurrences()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF produits achetés ensemble", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour les anomalies de ventes
     */
    public byte[] generateSalesAnomaliesPdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.SalesAnomalyItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph("Anomalies de Ventes", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            addTableCell(table, "Date", headerFont, true);
            addTableCell(table, "Chiffre d'affaires", headerFont, true);
            addTableCell(table, "Moyenne", headerFont, true);
            addTableCell(table, "Type d'anomalie", headerFont, true);

            for (ReportsDTO.SalesAnomalyItem item : items) {
                addTableCell(table, item.getDate(), normalFont, false);
                addTableCell(table, formatCurrency(item.getRevenue()), normalFont, false);
                addTableCell(table, formatCurrency(item.getAverageRevenue()), normalFont, false);
                addTableCell(table, item.getAnomalyType(), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF anomalies ventes", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la projection de trésorerie
     */
    public byte[] generateCashflowProjectionPdf(String shopName, ReportsDTO.CashflowProjection projection) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Projection de Trésorerie", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph shop = new Paragraph("Boutique: " + shopName, normalFont);
            document.add(shop);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Chiffre d'affaires (30 derniers jours)", titleFont, true);
            addTableCell(table, formatCurrency(projection.getPast30DaysRevenue()), normalFont, false);
            addTableCell(table, "Projection (30 prochains jours)", titleFont, true);
            addTableCell(table, formatCurrency(projection.getProjectedNext30DaysRevenue()), normalFont, false);
            addTableCell(table, "Moyenne quotidienne", titleFont, true);
            addTableCell(table, formatCurrency(projection.getAverageDailyRevenue()), normalFont, false);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF projection trésorerie", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la valeur du stock dormant
     */
    public byte[] generateDormantStockValuePdf(String shopName, ReportsDTO.DormantStockValue value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Valeur du Stock Dormant", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph shop = new Paragraph("Boutique: " + shopName, normalFont);
            document.add(shop);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Valeur totale bloquée", titleFont, true);
            addTableCell(table, value.getTotalDormantValue().toString(), normalFont, false);
            addTableCell(table, "Nombre de produits dormants", titleFont, true);
            addTableCell(table, String.valueOf(value.getDormantProductCount()), normalFont, false);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF valeur stock dormant", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    /**
     * Génère un PDF pour la performance client
     */
    public byte[] generateCustomerPerformancePdf(String shopName, LocalDate startDate, LocalDate endDate, List<ReportsDTO.CustomerPerformanceItem> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Performance Clients", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph period = new Paragraph(
                    "Boutique: " + shopName + "\n" +
                    "Période: " + startDate.format(DATE_FORMATTER) + " au " + endDate.format(DATE_FORMATTER),
                    normalFont
            );
            document.add(period);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            addTableCell(table, "Client", titleFont, true);
            addTableCell(table, "Chiffre d'affaires", titleFont, true);
            addTableCell(table, "Nombre de factures", titleFont, true);

            for (ReportsDTO.CustomerPerformanceItem item : items) {
                addTableCell(table, item.getCustomerName(), normalFont, false);
                addTableCell(table, formatCurrency(item.getTotalRevenue()), normalFont, false);
                addTableCell(table, String.valueOf(item.getInvoiceCount()), normalFont, false);
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de génération du PDF performance clients", e);
            throw new RuntimeException("Impossible de générer le PDF", e);
        }
    }

    // Méthodes utilitaires

    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (isHeader) {
            cell.setBackgroundColor(new java.awt.Color(200, 200, 200));
        }
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 FCFA";
        }
        return amount.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString() + " FCFA";
    }
}
