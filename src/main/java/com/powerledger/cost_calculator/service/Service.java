package com.powerledger.cost_calculator.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.powerledger.cost_calculator.dto.MonthCostDTO;
import com.powerledger.cost_calculator.dto.MonthYearStatDTO;
import com.powerledger.cost_calculator.dto.MonthYearStatResponseDTO;
import com.powerledger.cost_calculator.model.MonthCost;
import com.powerledger.cost_calculator.model.Year;
import com.powerledger.cost_calculator.repository.MonthCostRepository;
import com.powerledger.cost_calculator.repository.YearRepository;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@NoArgsConstructor
public class Service {

    @Autowired
    private YearRepository yearRepository;
    @Autowired
    private MonthCostRepository monthCostRepository;

    public boolean saveMonthCostByYear(List<MonthCostDTO> dto) {
        try {
            for (MonthCostDTO monthCostDTO : dto) {
                saveCost(monthCostDTO);
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void saveCost(MonthCostDTO dto) {

        Long yearId;
        if (!yearRepository.existByYear(dto.getYear())) {
            Year newYear = new Year();
            newYear.setYear(dto.getYear());
            Year savedYear = yearRepository.save(newYear);
            yearId = savedYear.getId();
        } else {
            yearId = yearRepository.findByYear(dto.getYear());
        }
        if (!monthCostRepository.existByMonthAndYear(yearId, dto.getMonth())) {
            MonthCost monthCost = new MonthCost();
            monthCost.setMonth(dto.getMonth());
            monthCost.setCost(dto.getCost());
            monthCost.setYear(yearRepository.findById(yearId).get());
            monthCostRepository.save(monthCost);
        }
    }


    public List<MonthYearStatResponseDTO> getMonthYearStat(MonthYearStatDTO dto) {
        int currentYear = dto.getYear().intValue();
        int previousYear = dto.getYear().intValue() - 1;
        List<MonthCost> months = monthCostRepository.findByYearValue(currentYear, previousYear);
        List<MonthCost> filteredMonths = new ArrayList<>();
        months.stream()
                .filter(m -> m.getYear().getYear() == currentYear)
                .forEach(filteredMonths::add);
        months.stream()
                .filter(m -> m.getYear().getYear() == previousYear && "DECEMBER".equalsIgnoreCase(m.getMonth()))
                .forEach(filteredMonths::add);

        filteredMonths.sort((m1, m2) -> {
            int y1 = m1.getYear().getYear();
            int y2 = m2.getYear().getYear();
            int month1 = monthToInt(m1.getMonth());
            int month2 = monthToInt(m2.getMonth());

            if (y1 != y2) {
                return Integer.compare(y1, y2);
            } else {
                return Integer.compare(month1, month2);
            }
        });

        List<MonthYearStatResponseDTO> result = new ArrayList<>();
        MonthCost prev = null;
        HashMap<String, Double> minMaxCost = new HashMap<>();
        for (MonthCost current : filteredMonths) {
            if (current.getYear().getYear() == currentYear) {
                minMaxCost.put(current.getMonth(), current.getCost());
            }
            MonthYearStatResponseDTO response = getMonthYearStatResponseDTO(current, prev);
            if (response.getMonth() != null && response.getMessage() != null) {
                result.add(response);
            }
            prev = current;
        }
        calculateMinMaxCost(result, minMaxCost,dto.getYear().intValue());
        return result;
    }

    private void calculateMinMaxCost(List<MonthYearStatResponseDTO> result, HashMap<String, Double> minMaxCost,int year) {
        Map.Entry<String, Double> minEntry = minMaxCost.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);
        MonthYearStatResponseDTO minResponse = new MonthYearStatResponseDTO();
        minResponse.setMonth(minEntry.getKey());
        minResponse.setMessage("Minimum cost month " + String.valueOf(minEntry.getValue()));
        minResponse.setYear(year);
        result.add(minResponse);
        Map.Entry<String, Double> maxEntry = minMaxCost.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        MonthYearStatResponseDTO maxResponse = new MonthYearStatResponseDTO();
        maxResponse.setMonth(maxEntry.getKey());
        maxResponse.setMessage("Maximum cost month " + String.valueOf(maxEntry.getValue()));
        maxResponse.setYear(year);
        result.add(maxResponse);
    }

    private static @NonNull MonthYearStatResponseDTO getMonthYearStatResponseDTO(MonthCost current, MonthCost prev) {
        MonthYearStatResponseDTO response = new MonthYearStatResponseDTO();

        if (prev != null) {
            response.setMonth(current.getMonth());
            double prevCost = prev.getCost();
            double currCost = current.getCost();
            double percentage = ((currCost - prevCost) / prevCost) * 100;
            String msg = percentage >= 0
                    ? String.format("Increased by %.2f%% compared to %s, %d (%.2f) and this month cost is %.2f", percentage, prev.getMonth(), prev.getYear().getYear(),prev.getCost(), current.getCost())
                    : String.format("Decreased by %.2f%% compared to %s, %d (%.2f) and this month cost is %.2f", Math.abs(percentage), prev.getMonth(), prev.getYear().getYear(),prev.getCost(), current.getCost());

            response.setMessage(msg);
            response.setYear(current.getYear().getYear());
        }
        return response;
    }


    private int monthToInt(String month) {
        return switch (month.toUpperCase()) {
            case "JANUARY" -> 1;
            case "FEBRUARY" -> 2;
            case "MARCH" -> 3;
            case "APRIL" -> 4;
            case "MAY" -> 5;
            case "JUNE" -> 6;
            case "JULY" -> 7;
            case "AUGUST" -> 8;
            case "SEPTEMBER" -> 9;
            case "OCTOBER" -> 10;
            case "NOVEMBER" -> 11;
            case "DECEMBER" -> 12;
            default -> 0;
        };
    }

    public ByteArrayInputStream generatePdf(List<MonthYearStatResponseDTO> data, MonthYearStatDTO yearStatDTO) {

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph(
                    "Monthly Cost Analysis Report " + yearStatDTO.getYear(),
                    titleFont
            );
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Create bold font for header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Year header
            PdfPCell yearHeader = new PdfPCell(new Phrase("Year", headerFont));
            yearHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            yearHeader.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(yearHeader);

            // Month header
            PdfPCell monthHeader = new PdfPCell(new Phrase("Month", headerFont));
            monthHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            monthHeader.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(monthHeader);

            // Message header
            PdfPCell costHeader = new PdfPCell(new Phrase("Cost", headerFont));
            costHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            costHeader.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(costHeader);

            for (MonthYearStatResponseDTO dto : data) {
                table.addCell(String.valueOf(dto.getYear()));
                table.addCell(dto.getMonth());
                table.addCell(dto.getMessage());
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

}
