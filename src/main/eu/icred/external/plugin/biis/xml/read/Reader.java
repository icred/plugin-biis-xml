package eu.icred.external.plugin.biis.xml.read;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import eu.icred.model.datatype.enumeration.AreaMeasurement;
import eu.icred.model.datatype.enumeration.ConstructionPhase;
import eu.icred.model.datatype.enumeration.Country;
import eu.icred.model.datatype.enumeration.InteriorQuality;
import eu.icred.model.datatype.enumeration.ObjectCondition;
import eu.icred.model.datatype.enumeration.OwnershipType;
import eu.icred.model.datatype.enumeration.RetailLocationType;
import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.datatype.enumeration.UseType;
import eu.icred.model.datatype.enumeration.ValuationType1;
import eu.icred.model.datatype.enumeration.ValuationType2;
import eu.icred.model.node.Container;
import eu.icred.model.node.Data;
import eu.icred.model.node.Meta;
import eu.icred.model.node.entity.Property;
import eu.icred.model.node.entity.Valuation;
import eu.icred.model.node.group.Address;
import eu.icred.plugin.PluginComponent;
import eu.icred.plugin.worker.WorkerConfiguration;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.input.ImportWorkerConfiguration;

public class Reader implements IImportWorker {
    private static Logger logger = Logger.getLogger(Reader.class);

    public static final Subset[] SUPPORTED_SUBSETS = { Subset.S5_7 };
    private static String PARAMETER_NAME = "biis-file";

    private Container container = null;
    private XMLStreamReader xmlStream = null;

    private Stack<String> nodeStack = new Stack<String>();

    @Override
    public List<Subset> getSupportedSubsets() {
        return Arrays.asList(SUPPORTED_SUBSETS);
    }

    @Override
    public void load(WorkerConfiguration config) {
        throw new RuntimeException("not allowed");
    }

    @Override
    public void unload() {
        try {
            xmlStream.close();
        } catch (Throwable t) {
        }
        xmlStream = null;
    }

    @Override
    public void load(ImportWorkerConfiguration config) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            xmlStream = factory.createXMLStreamReader(config.getStreams().get(PARAMETER_NAME), "UTF-8");

            container = new Container();
            Meta meta = container.getMeta();
            meta.setCreator("icred with biis-xml plugin");
            meta.setProcess(Subset.S5_7);
            meta.setFormat("XML");
            meta.setVersion("1-0.6.2");

            Data data = container.getMaindata();

            Property prop = new Property();
            Valuation val = new Valuation();
            Address valAddress = new Address();
            val.setAddress(valAddress);

            Currency mainCurrency = null;
            AreaMeasurement mainAreaMeasurement = null;

            while (xmlStream.hasNext()) {
                xmlStream.next();
                if (xmlStream.isStartElement()) {
                    String name = xmlStream.getLocalName();
                    nodeStack.push(name);

                    String xPath = nodeStack.toString().substring(1).replaceAll("\\]$", "").replaceAll(", ", "/");

                    if (xPath.equals("ValXML/Date")) {
                        // ignore - see field DateOfAppraisal

                    } else if (xPath.equals("ValXML/CompletionDate")) {
                        meta.setCreated(LocalDateTime.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/DataSupplier/Short")) {
                        val.setExpertId(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/DataSupplier/Name")) {
                        val.setExpertName(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/ArealUnit")) {
                        mainAreaMeasurement = biis2gif_AreaMeasureMent(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Address/Street")) {
                        valAddress.setStreet(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Address/PostCode")) {
                        valAddress.setZip(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Address/Town")) {
                        valAddress.setCity(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Address/Country")) {
                        String country = xmlStream.getElementText();
                        if (country != null && country.length() > 0)
                            valAddress.setCountry(Country.valueOf(country));

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Address/Text")) {
                        String label = xmlStream.getElementText();
                        prop.setLabel(label);
                        valAddress.setLabel(label);

                    } else if (xPath.equals("ValXML/BIISValuationData/General/Owner")) {
                        val.setOwner(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/General/ObjNoOwner")) {
                        String objId = xmlStream.getElementText();
                        prop.setObjectIdSender(objId);
                        prop.setObjectIdReceiver(objId);

                    } else if (xPath.equals("ValXML/BIISValuationData/General/ObjKoWGS84Longitude")) {
                        valAddress.setLongitude(biis2gif_Double(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/General/ObjKoWGS84Latitude")) {
                        valAddress.setLatitude(biis2gif_Double(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/Currency")) {
                        mainCurrency = Currency.getInstance(xmlStream.getElementText());
                        val.setCurrency(mainCurrency);

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/ShareAncillaryTypeOfUse")) {
                        val.setUseTypeSecondaryShare(biis2gif_Double(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/ShareMainTypeOfUse")) {
                        val.setUseTypePrimaryShare(biis2gif_Double(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/GroundLease")) {
                        val.setGroundLease(biis2gif_Boolean(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/MaintenanceBacklog")) {
                       val.setMaintenanceBacklog(biis2gif_Boolean(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/SingleTenant")) {
                        val.setSingleTenant(biis2gif_Boolean(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/SingleTenant")) {
                        val.setSingleTenant(biis2gif_Boolean(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/DateExchangeRate")) {
                        val.setExchangeRateDate(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/DateOfAppraisal")) {
                        val.setValidFrom(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/DateOfChangeForRemainingEconomicLife")) {
                        val.setChangeDateForRemainingEconomicLife(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/DateOfPurchase")) {
                        val.setPurchaseDate(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/DateOfSale")) {
                        val.setSaleDate(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/AncillaryTypeOfUse")) {
                        val.setUseTypeSecondary(biis2gif_UseType(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/FitOutQuality")) {
                        val.setInteriorQuality(biis2gif_InteriorQuality(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/Floors")) {
                        val.setFloorDescription(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/GroundLeaseRemarks")) {
                        val.setGroundLeaseRemarks(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/LocationQuality")) {
                        val.setRetailLocation(biis2gif_RetailLocationType(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/MainTypeOfUse")) {
                        val.setUseTypePrimary(biis2gif_UseType(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/OriginalYearOfConstruction")) {
                        val.setConstructionDate(LocalDate.parse(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/QualityDateOfAppraisal")) {
                        // ignore

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/RebaseObjAdditionalInformation")) {
                        val.setNote(xmlStream.getElementText());

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/RebaseType1")) {
                        val.setValuationType1(biis2gif_ValuationType1(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/RebaseType2")) {
                        val.setValuationType2(biis2gif_ValuationType2(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/StateOfCompletion")) {
                        val.setConstructionPhase(biis2gif_ConstructionPhase(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/StructuralCondition")) {
                        val.setCondition(biis2gif_Condition(xmlStream.getElementText()));

                    } else if (xPath.equals("ValXML/BIISValuationData/ValuationResults/TypeOfOwnership")) {
                        val.setOwnershipType(biis2gif_OwnershipType(xmlStream.getElementText()));

                    }

                    
                    /*
                     * 
                     * 



xs:decimal;CapitalizationRate
xs:decimal;CostApproach
xs:decimal;DeductionConstructionWorks
xs:decimal;DeductionForVacancy
xs:decimal;DiscountsPremiums
xs:decimal;ExchangeRate1EUR
xs:decimal;GrossFloorSpaceBelowGround
xs:decimal;GrossFloorSpaceOverground
xs:decimal;GroundRent
xs:decimal;LandSize
xs:decimal;LandValue
xs:decimal;MaintenanceExpenses
xs:decimal;ManagementCosts
xs:decimal;MarketValue
xs:decimal;OtherOperatingExpenses
xs:decimal;OthersDiscountsPremiums
xs:decimal;PriceOfSale
xs:decimal;PurchasePrice
xs:decimal;RemainingEconomicLife
xs:decimal;RemainingLifeOfGroundLease
xs:decimal;RentAllowance
xs:decimal;RentalSituationArchiveContractualAnnualRent
xs:decimal;RentalSituationArchiveEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationArchiveEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationArchiveLetArea
xs:decimal;RentalSituationArchiveVacantArea
xs:decimal;RentalSituationGastroContractualAnnualRent
xs:decimal;RentalSituationGastroEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationGastroEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationGastroLetArea
xs:decimal;RentalSituationGastroVacantArea
xs:decimal;RentalSituationHotelContractualAnnualRent
xs:decimal;RentalSituationHotelEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationHotelEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationHotelLetArea
xs:decimal;RentalSituationHotelVacantArea
xs:decimal;RentalSituationIndoorparkingContractualAnnualRent
xs:decimal;RentalSituationIndoorparkingEstimatedAnnualRentForLetNumbers
xs:decimal;RentalSituationIndoorparkingEstimatedAnnualRentForVacantNumbers
xs:decimal;RentalSituationIndoorparkingLetNumbers
xs:decimal;RentalSituationIndoorparkingVacantNumbers
xs:decimal;RentalSituationLeisureContractualAnnualRent
xs:decimal;RentalSituationLeisureEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationLeisureEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationLeisureLetArea
xs:decimal;RentalSituationLeisureVacantArea
xs:decimal;RentalSituationMiscArea1ContractualAnnualRent
xs:decimal;RentalSituationMiscArea1EstimatedAnnualRentForLetArea
xs:decimal;RentalSituationMiscArea1EstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationMiscArea1LetArea
xs:decimal;RentalSituationMiscArea1VacantArea
xs:decimal;RentalSituationMiscArea2ContractualAnnualRent
xs:decimal;RentalSituationMiscArea2EstimatedAnnualRentForLetArea
xs:decimal;RentalSituationMiscArea2EstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationMiscArea2LetArea
xs:decimal;RentalSituationMiscArea2VacantArea
xs:decimal;RentalSituationMiscnumbers1ContractualAnnualRent
xs:decimal;RentalSituationMiscnumbers1EstimatedAnnualRentForLetNumbers
xs:decimal;RentalSituationMiscnumbers1EstimatedAnnualRentForVacantNumbers
xs:decimal;RentalSituationMiscnumbers1LetNumbers
xs:decimal;RentalSituationMiscnumbers1VacantNumbers
xs:decimal;RentalSituationMiscnumbers2ContractualAnnualRent
xs:decimal;RentalSituationMiscnumbers2EstimatedAnnualRentForLetNumbers
xs:decimal;RentalSituationMiscnumbers2EstimatedAnnualRentForVacantNumbers
xs:decimal;RentalSituationMiscnumbers2LetNumbers
xs:decimal;RentalSituationMiscnumbers2VacantNumbers
xs:decimal;RentalSituationOfficeContractualAnnualRent
xs:decimal;RentalSituationOfficeEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationOfficeEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationOfficeLetArea
xs:decimal;RentalSituationOfficeVacantArea
xs:decimal;RentalSituationOutsideparkingContractualAnnualRent
xs:decimal;RentalSituationOutsideparkingEstimatedAnnualRentForLetNumbers
xs:decimal;RentalSituationOutsideparkingEstimatedAnnualRentForVacantNumbers
xs:decimal;RentalSituationOutsideparkingLetNumbers
xs:decimal;RentalSituationOutsideparkingVacantNumbers
xs:decimal;RentalSituationResidentialContractualAnnualRent
xs:decimal;RentalSituationResidentialEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationResidentialEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationResidentialLetArea
xs:decimal;RentalSituationResidentialVacantArea
xs:decimal;RentalSituationRetailContractualAnnualRent
xs:decimal;RentalSituationRetailEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationRetailEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationRetailLetArea
xs:decimal;RentalSituationRetailVacantArea
xs:decimal;RentalSituationStorageContractualAnnualRent
xs:decimal;RentalSituationStorageEstimatedAnnualRentForLetArea
xs:decimal;RentalSituationStorageEstimatedAnnualRentForVacantArea
xs:decimal;RentalSituationStorageLetArea
xs:decimal;RentalSituationStorageVacantArea
xs:decimal;RunningCosts
xs:decimal;SiteCoverageRatio
xs:decimal;TotalGrossFloorSpace
xs:decimal;TotalRentableArea
xs:decimal;ValueByIncomeApproach
xs:decimal;ValueByIncomeApproachWithoutPremiumsDiscounts
xs:double;FloorToAreaRatio
xs:integer;CalculatedYearOfConstruction
xs:integer;NormalTotalEconomicLife

                     */
                }

                if (xmlStream.isEndElement()) {
                    nodeStack.pop();
                }
            }

            Map<String, Valuation> valuations = new HashMap<String, Valuation>();
            valuations.put(val.getObjectIdSender(), val);
            prop.setValuations(valuations);
            data.getProperties().put(prop.getObjectIdSender(), prop);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private AreaMeasurement biis2gif_AreaMeasureMent(String biisValue) {
        if (biisValue.equals("sqft")) {
            return AreaMeasurement.SQFT;
        } else if (biisValue.equals("qm")) {
            return AreaMeasurement.SQM;
        } else if (biisValue.equals("tsubo") || biisValue.equals("pyeong")) {
            return AreaMeasurement.TSUBO;
        } else {
            return AreaMeasurement.NOT_SPECIFIED;
        }
    }

    private Double biis2gif_Double(String biisValue) {
        if (biisValue == null)
            return null;

        return Double.parseDouble(biisValue);
    }

    private Boolean biis2gif_Boolean(String value) {
        if (value == null)
            return null;

        if (value.toUpperCase().equals("TRUE")) {
            return true;
        }
        if (value.toUpperCase().equals("FALSE")) {
            return false;
        }

        return null;
    }


    private ConstructionPhase biis2gif_ConstructionPhase(String biisValue) {
        if (biisValue.equals("F")) {
            return ConstructionPhase.COMPLETED;
        } else if (biisValue.equals("I")) {
            return ConstructionPhase.IN_COMPLETION;
        } else if (biisValue.equals("P")) {
            return ConstructionPhase.PLANNED;
        } else if (biisValue.equals("0")) {
            return ConstructionPhase.OTHER;
        } else {
            return null;
        }
    }

    private ValuationType1 biis2gif_ValuationType1(String biisValue) {
        if (biisValue.equals("Fondsgutachten")) {
            return ValuationType1.FUND;
        } else if (biisValue.equals("Privatgutachten")) {
            return ValuationType1.PRIVATE;
        } else if (biisValue.equals("Gerichtsgutachten")) {
            return ValuationType1.COURT;
        } else if (biisValue.equals("Fremdgutachten")) {
            return ValuationType1.THIRD_PERSON;
        } else {
            return null;
        }
    }

    private ValuationType2 biis2gif_ValuationType2(String biisValue) {
        if (biisValue.equals("U")) {
            return ValuationType2.UNKNOWN;
        } else if (biisValue.equals("E")) {
            return ValuationType2.FIRST_VALUATION;
        } else if (biisValue.equals("N")) {
            return ValuationType2.REVALUATION;
        } else if (biisValue.equals("V")) {
            return ValuationType2.MARKET_VALUATION_REPORT;
        } else {
            return null;
        }
    }

    private UseType biis2gif_UseType(String biisValue) {
        if (biisValue.equals("Buero")) {
            return UseType.OFFICE;
        } else if (biisValue.equals("Handel")) {
            return UseType.RETAIL;
        } else if (biisValue.equals("Industrie(Lager,Hallen)")) {
            return UseType.INDUSTRY;
        } else if (biisValue.equals("Keller/Archiv")) {
            return UseType.OTHER;
        } else if (biisValue.equals("Gastronomie")) {
            return UseType.GASTRONOMY;
        } else if (biisValue.equals("Hotel")) {
            return UseType.HOTEL;
        } else if (biisValue.equals("Wohnen")) {
            return UseType.RESIDENTIAL;
        } else if (biisValue.equals("Freizeit")) {
            return UseType.LEISURE;
        } else if (biisValue.equals("Garage/TG")) {
            return UseType.PARKING;
        } else if (biisValue.equals("Aussenstellplaetze")) {
            return UseType.PARKING;
        } else if (biisValue.equals("unbekannt")) {
            return UseType.NOT_SPECIFIED;
        } else {
            return UseType.NOT_SPECIFIED;
        }
    }
    private OwnershipType biis2gif_OwnershipType(String biisValue) {
        if (biisValue.equals("U")) {
            return null;
        } else if (biisValue.equals("E")) {
            return null;
        } else if (biisValue.equals("N")) {
            return null;
        } else if (biisValue.equals("V")) {
            return null;
        } else {
            return null;
        }
    }

    private RetailLocationType biis2gif_RetailLocationType(String biisValue) {
        if (biisValue.equals("U")) {
            return null;
        } else if (biisValue.equals("E")) {
            return null;
        } else if (biisValue.equals("N")) {
            return null;
        } else if (biisValue.equals("V")) {
            return null;
        } else {
            return null;
        }
    }

    private ObjectCondition biis2gif_Condition(String biisValue) {
        if (biisValue.equals("U")) {
            return null;
        } else if (biisValue.equals("E")) {
            return null;
        } else if (biisValue.equals("N")) {
            return null;
        } else if (biisValue.equals("V")) {
            return null;
        } else {
            return null;
        }
    }

    private InteriorQuality biis2gif_InteriorQuality(String biisValue) {
        if (biisValue.equals("U")) {
            return null;
        } else if (biisValue.equals("E")) {
            return null;
        } else if (biisValue.equals("N")) {
            return null;
        } else if (biisValue.equals("V")) {
            return null;
        } else {
            return null;
        }
    }

    
    @Override
    public ImportWorkerConfiguration getRequiredConfigurationArguments() {
        return new ImportWorkerConfiguration() {
            {
                SortedMap<String, InputStream> streams = getStreams();
                streams.put(PARAMETER_NAME, null);
            }
        };
    }

    @Override
    public PluginComponent<ImportWorkerConfiguration> getConfigGui() {
        // null => DefaultConfigGui
        return null;
    }

    @Override
    public Container getContainer() {
        return container;
    }
}
