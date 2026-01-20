package com.powerledger.cost_calculator.controller;

import com.powerledger.cost_calculator.dto.MonthCostDTO;
import com.powerledger.cost_calculator.dto.MonthYearStatDTO;
import com.powerledger.cost_calculator.dto.MonthYearStatResponseDTO;
import com.powerledger.cost_calculator.service.Service;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monthCost")
@NoArgsConstructor
public class Controller {

    @Autowired
    private Service service;

    @PostMapping
    private ResponseEntity<?> saveMonthCostByYear(@RequestBody List<MonthCostDTO> dto) {
        boolean result = service.saveMonthCostByYear(dto);
        if (result) {
            return new ResponseEntity<>("Save successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Save not successfully", HttpStatus.OK);
        }
    }

    @PostMapping("/getPerchantage")
    private ResponseEntity<?> getMonthYearStat(@RequestBody MonthYearStatDTO dto){
        List<MonthYearStatResponseDTO> responseDTOS=service.getMonthYearStat(dto);
        return new ResponseEntity<>(responseDTOS, HttpStatus.OK);
    }
}
