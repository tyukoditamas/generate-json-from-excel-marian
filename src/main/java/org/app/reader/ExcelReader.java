package org.app.reader;

import lombok.var;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.app.model.ExcelDto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ExcelReader {
    public static List<ExcelDto> read(File excelFile) throws IOException {
        List<ExcelDto> excelDtos = new ArrayList<>();

        var fis = new FileInputStream(excelFile);
        Workbook wb = WorkbookFactory.create(fis);
        Sheet sheet = wb.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        rows.next(); // skip header
        DataFormatter fmt = new DataFormatter();

        while (rows.hasNext()) {
            Row row = rows.next();
            if (isEmpty(row, fmt)) continue;

            ExcelDto dto = new ExcelDto();

            for (Cell c : row) {
                String val = fmt.formatCellValue(c);
                switch (c.getColumnIndex()) {
                    case 0:  dto.setTrackingNr(val.trim()); break;
                    case 1:  dto.setShipperName(val.trim()); break;
                    case 2:  dto.setShipperAddress(val.trim()); break;
                    case 3:  dto.setShipperCity(val.trim()); break;
                    case 4:  dto.setImporterCountry(val.trim()); break;
                    case 5:  dto.setImporterName(val.trim()); break;
                    case 6:  dto.setImporterAddress(val.trim()); break;
                    case 7:  dto.setImporterCity(val.trim()); break;
                    case 8:  dto.setImporterPostCode(val.trim()); break;
                    case 9:  dto.setNrOfPackages(val.trim()); break;
                    case 10: dto.setWeight(val.trim()); break;
                    case 11: dto.setDescriptionOfGoods(val.trim()); break;
                    case 12: dto.setMasterAwb(val.trim()); break;
                    case 13: dto.setMasterDocument(val.trim()); break;
                }

            }

            String[] currentCountries = dto.getCountriesOfRoutingOfConsignment();
            String importerCountry = dto.getImporterCountry();

            if (!Arrays.asList(currentCountries).contains(importerCountry)) {
                dto.setCountriesOfRoutingOfConsignment(ArrayUtils.add(currentCountries, importerCountry));
            }

            excelDtos.add(dto);
        }
        return excelDtos;
    }

    private static boolean isEmpty(Row row, DataFormatter fmt) {
        for (Cell c : row)
            if (!fmt.formatCellValue(c).trim().isEmpty())
                return false;
        return true;
    }
}
