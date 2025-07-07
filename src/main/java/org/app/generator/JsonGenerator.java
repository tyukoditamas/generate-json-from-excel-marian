package org.app.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.app.model.ExcelDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JsonGenerator {
    private final ObjectMapper MAPPER = new ObjectMapper();
    private Consumer<String> log;
    private Path outputDir;

    /**
     * dtos     = list of all rows read from Excel
     * excelFile = the File the user selected (so we can resolve its parent folder)
     **/
    public static void generate(List<ExcelDto> dtos, File excelFile, Consumer<String> log) throws IOException {
        JsonGenerator gen = new JsonGenerator();
        gen.log = log;
        gen.outputDir = excelFile.toPath().getParent();
        gen.doGenerate(dtos);
        log.accept("All JSONs written.");
    }

    private void doGenerate(List<ExcelDto> dtos) throws IOException {
        // group by the four columns
        Map<GroupKey, List<ExcelDto>> groups = dtos.stream().collect(
                Collectors.groupingBy(d -> new GroupKey(
                        d.getShipperName(),
                        d.getShipperCity(),
                        d.getImporterName(),
                        d.getImporterCity()
                ))
        );

        for (List<ExcelDto> group : groups.values()) {
            int total = group.size();
            int partNo = 1;

            // split into sublists of max size 3
            for (int i = 0; i < total; i += 3) {
                List<ExcelDto> subList = group.subList(i, Math.min(i + 3, total));

                // build filename: join all trackingNr’s with a space
                String joinedTracking = subList.stream()
                        .map(ExcelDto::getTrackingNr)
                        .collect(Collectors.joining(" "));

                String fileName = joinedTracking + ".json";
                Path out = outputDir.resolve(fileName);

                // Sum weights for all entries (convert to double, skip blank/null)
                double totalGrossMass = group.stream()
                        .mapToDouble(d -> {
                            try {
                                return Double.parseDouble(d.getWeight());
                            } catch (Exception e) {
                                return 0.0;
                            }
                        })
                        .sum();

                int totalPackages = subList.stream()
                        .mapToInt(d -> {
                            try {
                                return Integer.parseInt(d.getNrOfPackages());
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .sum();

                // build the JSON tree
                ObjectNode root = MAPPER.createObjectNode();

                // --- ensFilingInformation ---
                ObjectNode filing = root.putObject("ensFilingInformation");
                filing.put("specificCircumstanceIndicator", "F33");
                filing.put("addressedMemberStateCountry", "RO");
                ObjectNode transportMeans = filing.putObject("activeBorderTransportMeans");
                transportMeans.put("transportMode", "4");

                // --- ensActors ---
                ObjectNode actors = root.putObject("ensActors");
                // declarant
                ObjectNode declarant = actors.putObject("declarant");
                declarant.put("name", "UPS MOLDOVA SRL");
                declarant.put("identificationNumber", "RO13191000");
                ObjectNode decAddr = declarant.putObject("address");
                decAddr.put("street", "AUREL VLAICU");
                decAddr.putNull("streetAdditionalLine");
                decAddr.put("number", "11C");
                decAddr.put("postCode", "075100");
                decAddr.put("city", "OTOPENI");
                decAddr.putNull("subDivision");
                decAddr.put("country", "RO");
                decAddr.put("togglePoBox", false);
                ArrayNode decComm = declarant.putArray("communication");
                ObjectNode decEmail = decComm.addObject();
                decEmail.put("identifier", "dkurteanu@ups.com");
                decEmail.put("type", "EM");

                // representative
                ObjectNode rep = actors.putObject("representative");
                rep.put("name", "MARIANS TRADING SRL");
                rep.put("identificationNumber", "RO15467129");
                ObjectNode repAddr = rep.putObject("address");
                repAddr.put("street", "MEDITATIEI");
                repAddr.putNull("streetAdditionalLine");
                repAddr.put("number", "7");
                repAddr.put("postCode", "76500");
                repAddr.put("city", "BUCURESTI");
                repAddr.putNull("subDivision");
                repAddr.put("country", "RO");
                repAddr.put("togglePoBox", false);
                ArrayNode repComm = rep.putArray("communication");
                ObjectNode repEmail = repComm.addObject();
                repEmail.put("identifier", "UPS@MTRADING.RO");
                repEmail.put("type", "EM");
                rep.put("status", "2");

                // --- houseConsignments (always a single-element array) ---
                ArrayNode hcArray = root.putArray("houseConsignments");
                ObjectNode hc = hcArray.addObject();

                // transportDocument
                ObjectNode td = hc.putObject("transportDocument");
                td.put("type", "N741");
                td.put("documentNumber", joinedTracking);

                // transportDocumentMaster
                ObjectNode tdm = hc.putObject("transportDocumentMaster");
                tdm.put("documentNumber", subList.get(0).getMasterDocument());
                tdm.put("type", "N740");

                // containersInformation
                ObjectNode ci = hc.putObject("containersInformation");
                ci.put("containerIndicator", false);
                ci.putArray("transportEquipment");

                // additionalFiscalReferences
                ObjectNode afr = hc.putObject("additionalFiscalReferences");
                afr.putNull("role");
                afr.putNull("VATIdentificationNumber");

                hc.putNull("receptacleIdentificationNumber");
                hc.putNull("totalAmountInvoiced");
                hc.putNull("additionsAndDeductionsAmount");
                hc.putNull("postalCharges");
                hc.putArray("additionalInformation");

                // additionalSupplyChainActor
                ArrayNode actors2 = hc.putArray("additionalSupplyChainActor");
                ObjectNode asa = actors2.addObject();
                asa.put("identificationNumber", "RO13191000");
                asa.put("role", "CR");

                hc.put("totalGrossMass", String.valueOf(totalGrossMass));
                hc.put("referenceNumber", subList.get(0).getMasterDocument());

                // consignor
                ObjectNode consignor = hc.putObject("consignor");
                consignor.put("name", subList.get(0).getShipperName());
                consignor.putNull("identificationNumber");
                ObjectNode cAddr = consignor.putObject("address");
                cAddr.put("city", subList.get(0).getShipperCity());
                cAddr.put("country", "MD");
                cAddr.putNull("subDivision");
                cAddr.put("street", subList.get(0).getShipperAddress());
                cAddr.put("postCode", "MD2005");
                cAddr.putNull("streetAdditionalLine");
                cAddr.put("number", "30");
                cAddr.putNull("poBox");
                ArrayNode cComm = consignor.putArray("communication");
                ObjectNode cEmail = cComm.addObject();
                cEmail.put("identifier", "dkurteanu@ups.com");
                cEmail.put("type", "EM");
                consignor.put("typeOfPerson", "2");

                // consignee
                ObjectNode consignee = hc.putObject("consignee");
                consignee.put("name", subList.get(0).getImporterName());
                consignee.putNull("identificationNumber");
                ObjectNode coAddr = consignee.putObject("address");
                coAddr.put("city", subList.get(0).getImporterCity());
                coAddr.put("country", subList.get(0).getImporterCountry());
                coAddr.putNull("subDivision");
                coAddr.put("street", subList.get(0).getImporterAddress());
                coAddr.put("postCode", subList.get(0).getImporterPostCode());
                coAddr.putNull("streetAdditionalLine");
                coAddr.put("number", "5");
                coAddr.putNull("poBox");
                ArrayNode coComm = consignee.putArray("communication");
                ObjectNode coEmail = coComm.addObject();
                coEmail.put("identifier", "dkurteanu@ups.com");
                coEmail.put("type", "EM");
                consignee.put("typeOfPerson", "2");

                hc.putNull("notifyParty");
                hc.put("transportCharges", "Z");
                hc.put("carrierIdentificationNumber", "RO13191000");

                // supplementaryDeclarant
                ArrayNode sup = hc.putArray("supplementaryDeclarant");
                ObjectNode sd = sup.addObject();
                sd.put("identificationNumber", "RO15467129");
                sd.put("supplementaryFilingType", "1");

                // supportingDocuments
                ArrayNode docs = hc.putArray("supportingDocuments");
                ObjectNode doc0 = docs.addObject();
                doc0.put("referenceNumber", subList.get(0).getMasterDocument());
                doc0.put("type", "N741");

                // placeOfAcceptance
                ObjectNode poa = hc.putObject("placeOfAcceptance");
                poa.put("location", "CHISINAU");
                poa.put("addressCountry", "MD");
                poa.putNull("unlocode");

                // placeOfDelivery
                ObjectNode pod = hc.putObject("placeOfDelivery");
                pod.put("location", "OTOPENI");
                pod.put("addressCountry", "RO");
                pod.putNull("unlocode");

                // countriesOfRoutingOfConsignment
                ArrayNode cor = hc.putArray("countriesOfRoutingOfConsignment");
                for (String ctry : subList.get(0).getCountriesOfRoutingOfConsignment()) {
                    cor.add(ctry);
                }

                // goodsItems
                ArrayNode giArr = hc.putArray("goodsItems");
                ObjectNode gi = giArr.addObject();
                gi.put("goodsItemNumber", "1");
                ObjectNode cc = gi.putObject("commodityCode");
                cc.put("harmonizedSystemSubHeadingCode", subList.get(0).getMasterAwb());
                cc.putNull("combinedNomenclatureCode");
                gi.putNull("CUSCode");
                gi.putArray("UNDangerousGoods");
                gi.put("descriptionOfGoods", subList.get(0).getDescriptionOfGoods());
                gi.put("grossMass", String.valueOf(totalGrossMass));
                gi.putArray("transportEquipment");
                gi.putArray("additionalSupplyChainActor");
                ArrayNode supDocs2 = gi.putArray("supportingDocuments");
                ObjectNode sd2 = supDocs2.addObject();
                sd2.put("referenceNumber", subList.get(0).getMasterDocument());
                sd2.put("type", "N741");
                gi.putArray("additionalInformation");
                ArrayNode pack = gi.putArray("packaging");
                ObjectNode pk = pack.addObject();
                pk.put("typeOfPackages", "PC");
                pk.put("numberOfPackages", totalPackages);
                pk.put("shippingMarks", "FARA");

                // write it out
                MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValue(out.toFile(), root);
                log.accept("Written JSON: " + fileName);
            }
        }
    }

    private static class GroupKey {
        final String sName, sCity, iName, iCity;

        GroupKey(String sName, String sCity, String iName, String iCity) {
            this.sName = sName;
            this.sCity = sCity;
            this.iName = iName;
            this.iCity = iCity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupKey)) return false;
            GroupKey g = (GroupKey) o;
            return Objects.equals(sName, g.sName)
                    && Objects.equals(sCity, g.sCity)
                    && Objects.equals(iName, g.iName)
                    && Objects.equals(iCity, g.iCity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sName, sCity, iName, iCity);
        }
    }
}
