package org.app.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.app.model.ExcelDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.app.generator.CountryCapital.getCapital;

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
        // Group by shipper/importer as before
        Map<GroupKey, List<ExcelDto>> groups = dtos.stream().collect(
                Collectors.groupingBy(d -> new GroupKey(
                        d.getShipperName(),
                        d.getShipperCity(),
                        d.getImporterName(),
                        d.getImporterCity()
                ))
        );

        for (List<ExcelDto> group : groups.values()) {
            // Split into chunks of 3 rows each (if needed)
            for (int i = 0; i < group.size(); i += 3) {
                List<ExcelDto> subList = group.subList(i, Math.min(i + 3, group.size()));

                // Compute aggregates
                double totalGrossMass = subList.stream()
                        .mapToDouble(d -> {
                            try { return Double.parseDouble(d.getWeight()); }
                            catch (Exception e) { return 0.0; }
                        }).sum();
                int totalPackages = subList.stream()
                        .mapToInt(d -> {
                            try { return Integer.parseInt(d.getNrOfPackages()); }
                            catch (Exception e) { return 0; }
                        }).sum();
                String joinedTracking = subList.stream()
                        .map(ExcelDto::getTrackingNr)
                        .collect(Collectors.joining(" "));

                // Prepare root node
                ObjectNode root = MAPPER.createObjectNode();
                // Top‑level metadata
                root.put("type", "IE3F33");
                root.put("version", "2.0");
                root.putNull("draftId");
                root.put("lrn", "F33BIS"); // or derive dynamically
                root.putNull("referralRequestReference");
                root.putNull("attachments");
                String now = Instant.now().toString();
                root.put("documentIssueDate", now);

                // Data section
                ObjectNode data = root.putObject("data");
                data.put("LRN", "F33BIS");
                ObjectNode di = data.putObject("documentIssueDate");
                di.put("DateTime", now);
                data.put("SpecificCircumstanceIndicator", "F33");

                ObjectNode addrMember = data.putObject("addressedMemberState");
                addrMember.put("country", "RO");

                // Representative (static)
                ObjectNode rep = data.putObject("representative");
                rep.put("name", "MARIANS TRADING SRL");
                rep.put("identificationNumber", "RO15467129");
                rep.put("status", "2");
                ObjectNode repAddr = rep.putObject("address");
                repAddr.put("city", "Bucuresti");
                repAddr.put("country", "RO");
                repAddr.put("street", "MEDITATIEI");
                repAddr.put("postCode", "1111");
                repAddr.put("number", "7");
                ArrayNode repComm = rep.putArray("communication");
                repComm.addObject()
                        .put("identifier", "UPS@MTRADING.RO")
                        .put("type", "EM");

                // Active transport means
                ObjectNode transport = data.putObject("activeBorderTransportMeans");
                transport.put("ModeOfTransport", "4");

                // Consignment master level
                ObjectNode cml = data.putObject("consignmentMasterLevel");
                ArrayNode chl = cml.putArray("consignmentHouseLevel");
                ObjectNode house = chl.addObject();
                house.put("containerIndicator", "0");
                house.put("totalGrossMass", totalGrossMass);

                // Place of acceptance (example: static or from DTO)
                ObjectNode poa = house.putObject("placeOfAcceptance");
                poa.put("location", "OTOPENI");
                poa.putObject("address").put("country", "RO");

                // Transport document master level
                ObjectNode tdm = house.putObject("transportDocumentMasterLevel");
                tdm.put("documentNumber", subList.get(0).getMasterDocument());
                tdm.put("type", "N741");

                // Carrier
                house.putObject("carrier")
                        .put("identificationNumber", "RO13191000");

                // Consignee
                ObjectNode consignee = house.putObject("consignee");
                consignee.put("name", subList.get(0).getImporterName());
                consignee.put("typeOfPerson", "2");
                ObjectNode coAddr = consignee.putObject("address");
                coAddr.put("city", subList.get(0).getImporterCity());
                coAddr.put("country", subList.get(0).getImporterCountry());
                coAddr.put("street", subList.get(0).getImporterAddress());
                coAddr.put("postCode", subList.get(0).getImporterPostCode());
                coAddr.put("number", "5");
                consignee.putArray("communication")
                        .addObject()
                        .put("identifier", "dkurteanu@ups.com")
                        .put("type", "EM");

                // Goods items (single)
                ArrayNode goods = house.putArray("goodsItem");
                ObjectNode gi = goods.addObject();
                gi.put("goodsItemNumber", 1);
                ObjectNode comm = gi.putObject("commodity");
                comm.put("descriptionOfGoods", subList.get(0).getDescriptionOfGoods());
                comm.putObject("commodityCode")
                        .put("harmonizedSystemSubHeadingCode", subList.get(0).getMasterAwb());
                gi.putObject("weight").put("grossMass", totalGrossMass);
                ArrayNode packaging = gi.putArray("packaging");
                packaging.addObject()
                        .put("typeOfPackages", "PC")
                        .put("numberOfPackages", totalPackages)
                        .put("shippingMarks", "FARA MARCA");

                // Consignor
                ObjectNode consignor = house.putObject("consignor");
                consignor.put("name", subList.get(0).getShipperName());
                consignor.put("typeOfPerson", "2");
                ObjectNode cAddr = consignor.putObject("address");
                cAddr.put("city", subList.get(0).getShipperCity());
                cAddr.put("country", "MD");
                cAddr.put("street", subList.get(0).getShipperAddress());
                cAddr.put("postCode", "2005");
                cAddr.put("number", "30");
                consignor.putArray("communication")
                        .addObject()
                        .put("identifier", "dkurteanu@ups.ro")
                        .put("type", "EM");

                // Transport charges
                house.putObject("transportCharges")
                        .put("methodOfPayment", "Z");

                // Place of delivery
                ObjectNode pod = house.putObject("placeOfDelivery");
                pod.put("location", getCapital(subList.get(0).getImporterCountry()));
                pod.putObject("address").put("country", subList.get(0).getImporterCountry());

                // Routing countries
                ArrayNode routing = house.putArray("countriesOfRoutingOfConsignment");
                int seq = 1;
                for (String c : subList.get(0).getCountriesOfRoutingOfConsignment()) {
                    routing.addObject()
                            .put("sequenceNumber", seq++)
                            .put("country", c);
                }

                // Transport document house level
                ObjectNode tdh = house.putObject("transportDocumentHouseLevel");
                tdh.put("documentNumber", joinedTracking);
                tdh.put("type", "N740");

                // Reference UCR
                house.putObject("referenceNumberUCR")
                        .put("referenceNumberUCR", subList.get(0).getMasterDocument());

                // Declarant (static)
                ObjectNode decl = data.putObject("declarant");
                decl.put("name", "UPS ROMANIA");
                decl.put("identificationNumber", "RO13191000");
                ObjectNode dAddr = decl.putObject("address");
                dAddr.put("city", "OTOPENI");
                dAddr.put("country", "RO");
                dAddr.put("street", "AUREL VLAICU");
                dAddr.put("postCode", "075100");
                dAddr.put("number", "11C");
                ArrayNode dComm = decl.putArray("communication");
                dComm.addObject()
                        .put("identifier", "dkurteanu@ups.ro")
                        .put("type", "EM");

                // Write JSON to file
                String fileName = joinedTracking + ".json";
                Path out = outputDir.resolve(fileName);
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
