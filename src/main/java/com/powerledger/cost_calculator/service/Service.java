package com.powerledger.cost_calculator.service;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        List<MonthCost> months = monthCostRepository.findByYearValue(dto.getYear().intValue());
        months.sort(Comparator.comparingInt(m -> monthToInt(m.getMonth())));
        List<MonthYearStatResponseDTO> result = new ArrayList<>();
        MonthCost prev = null;
        for (MonthCost current : months) {
            MonthYearStatResponseDTO response = getMonthYearStatResponseDTO(current, prev);
            result.add(response);
            prev = current;
        }

        return result;
    }

    private static @NonNull MonthYearStatResponseDTO getMonthYearStatResponseDTO(MonthCost current, MonthCost prev) {
        MonthYearStatResponseDTO response = new MonthYearStatResponseDTO();
        response.setMonth(current.getMonth());

        if (prev == null) {
            response.setMessage("N/A");
        } else {
            double prevCost = prev.getCost();
            double currCost = current.getCost();
            double percentage = ((currCost - prevCost) / prevCost) * 100;
            String msg = percentage >= 0
                    ? String.format("Increased by %.2f%% compared to %s", percentage, prev.getMonth())
                    : String.format("Decreased by %.2f%% compared to %s", Math.abs(percentage), prev.getMonth());

            response.setMessage(msg);
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
}
