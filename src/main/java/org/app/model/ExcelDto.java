package org.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExcelDto {
    private String trackingNr;
    private String weight;
    private String shipperName;
    private String shipperAddress;
    private String shipperCity;
    private String importerPostCode;
    private String importerName;
    private String importerAddress;
    private String importerCity;
    private String importerCountry;
    private String masterAwb;
    private String masterDocument;
    private String descriptionOfGoods;
    private String nrOfPackages;
    private String[] countriesOfRoutingOfConsignment = new String[]{"MD", "RO", "DE"};
}
