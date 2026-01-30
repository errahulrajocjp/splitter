package onevz.soe.smbenrollment.helper;

// ============================================================================
// FIX #5 (Line 7): Replaced wildcard import with explicit imports
// FIX #4 (Line 27): Removed duplicate Collectors import
// ============================================================================
import onevz.soe.smbenrollment.model.CustomerAddress;
import onevz.soe.smbenrollment.requests.spectrumadapter.FiveGCoverageCheckRequest;
import onevz.soe.smbenrollment.requests.spectrumadapter.NautilusQualificationRequest;
import onevz.soe.smbenrollment.requests.spectrumadapter.SplitAddressRequest;
import onevz.soe.smbenrollment.responses.SmbResponseWrapper;
import onevz.soe.smbenrollment.responses.SoeDataWrapper;
import onevz.soe.smbenrollment.responses.spectrumadapter.Address;
import onevz.soe.smbenrollment.responses.spectrumadapter.AddressInfo;
import onevz.soe.smbenrollment.responses.spectrumadapter.AvailableCapacityInfo;
import onevz.soe.smbenrollment.responses.spectrumadapter.BulkAddressQualificationResponse;
import onevz.soe.smbenrollment.responses.spectrumadapter.BundleInfo;
import onevz.soe.smbenrollment.responses.spectrumadapter.Eligibilities;
import onevz.soe.smbenrollment.responses.spectrumadapter.FiveGCoverageCheckResponse;
import onevz.soe.smbenrollment.responses.spectrumadapter.NautilusQualificationResponse;
import onevz.soe.smbenrollment.responses.spectrumadapter.PriorQualification;
import onevz.soe.smbenrollment.responses.spectrumadapter.SplitAddressResponse;
import onevz.soe.smbenrollment.constants.SmbConstants;
import onevz.soe.smbenrollment.utils.ValidationUtils;
import onevz.soe.util.CollectionUtilities;
import onevz.soe.util.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ValidateBusinessAccountHelper {

    private static final Logger logger = LoggerFactory.getLogger(ValidateBusinessAccountHelper.class);

    // FIX #8: Constants moved to SmbConstants class

    private final FiveGCoverageCheckHelper fiveGCoverageCheckHelper;
    private final CxpSpectrumAdapterService spectrumAdapterService;

    public ValidateBusinessAccountHelper(FiveGCoverageCheckHelper fiveGCoverageCheckHelper,
                                         CxpSpectrumAdapterService spectrumAdapterService) {
        this.fiveGCoverageCheckHelper = fiveGCoverageCheckHelper;
        this.spectrumAdapterService = spectrumAdapterService;
    }

    /**
     * Main entry point for 5G coverage check.
     */
    public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> check5GCoverageForAddresses(
            FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {

        if (hasInvalidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
            // ============================================================================
            // FIX #6 (Line 73): Changed logger.info() to logger.error() for error scenario
            // ============================================================================
            logger.error("Customer addresses are invalid. Returning error response.");
            return buildErrorResponse(new IllegalArgumentException(SmbConstants.ERROR_INVALID_ADDRESS));
        }

        return Flux.fromIterable(fiveGCoverageCheckRequest.getCustomerAddress())
                .flatMap(this::processSplitAddressForCustomer)
                .collectList()
                // ============================================================================
                // FIX #7 (Lines 218-221): Added check for empty list before calling Nautilus
                // ============================================================================
                .flatMap(validatedAddresses -> {
                    if (CollectionUtilities.isEmptyOrNull(validatedAddresses)) {
                        logger.error("No addresses available after split validation");
                        return buildErrorResponse(new IllegalStateException(SmbConstants.ERROR_ALL_ADDRESSES_FAILED_SPLIT));
                    }

                    // Check if ALL addresses failed split validation
                    boolean allFailed = validatedAddresses.stream()
                            .allMatch(addr -> addr.getStatusMsg() != null &&
                                    addr.getStatusMsg().startsWith("Address validation failed"));

                    if (allFailed) {
                        logger.warn("All {} addresses failed split address validation, skipping Nautilus call",
                                validatedAddresses.size());
                        // Return response with failed addresses without calling Nautilus
                        FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
                        response.setCustomerAddress(validatedAddresses);
                        return Mono.just(response);
                    }

                    // FIX #9: Changed logger.info() to logger.debug()
                    logger.debug("Proceeding to Nautilus with {} validated addresses", validatedAddresses.size());
                    return processValidateCustomerAccountForFiveGCoverage(validatedAddresses);
                })
                .map(this::buildSuccessResponse)
                .onErrorResume(this::buildErrorResponse);
    }

    /**
     * Processes Split Address service for a single customer address.
     */
    private Mono<CustomerAddress> processSplitAddressForCustomer(CustomerAddress customerAddress) {
        SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);

        return spectrumAdapterService.splitAddress(splitAddressRequest)
                .map(addressResponse -> {
                    if (isValidSplitAddressResponse(addressResponse)) {
                        Address address = extractSplitAddress(addressResponse);

                        if (address != null) {
                            CustomerAddress updatedAddress = createUpdatedAddressFromSplit(customerAddress, address);
                            // ============================================================================
                            // FIX #2 (Line 237): REMOVED updatedAddress.setQualified(true);
                            // Qualification should ONLY come from Nautilus service, not split address
                            // ============================================================================
                            updatedAddress.setStatusMsg(SmbConstants.STATUS_MSG_ADDRESS_VALIDATED);
                            // FIX #9: Changed logger.info() to logger.debug()
                            logger.debug("Address validated successfully for: {}", customerAddress.getAddressLine1());
                            return updatedAddress;
                        }
                    }

                    CustomerAddress failedAddress = createCustomerAddressCopy(customerAddress);
                    failedAddress.setQualified(false);
                    failedAddress.setStatusMsg(SmbConstants.ERROR_SPLIT_ADDRESS_FAILED);
                    return failedAddress;
                })
                .doOnError(e -> logger.error("Error occurred from split address service for address: {}. Error: {}",
                        customerAddress.getAddressLine1(), e.getMessage(), e))
                .onErrorResume(error -> {
                    logger.error("Handling error gracefully for address: {}", customerAddress.getAddressLine1());
                    CustomerAddress errorAddress = createCustomerAddressCopy(customerAddress);
                    errorAddress.setQualified(false);
                    errorAddress.setStatusMsg(SmbConstants.ERROR_SPLIT_ADDRESS_SERVICE + error.getMessage());
                    return Mono.just(errorAddress);
                });
    }

    /**
     * Processes Nautilus Qualification service.
     */
    private Mono<FiveGCoverageCheckResponse> processValidateCustomerAccountForFiveGCoverage(
            List<CustomerAddress> customerAddresses) {

        if (CollectionUtilities.isEmptyOrNull(customerAddresses)) {
            logger.warn("No customer addresses to process for Nautilus qualification");
            return Mono.just(createEmptyResponse());
        }

        // FIX #9: Changed logger.info() to logger.debug()
        logger.debug("Building Nautilus qualification request for {} addresses", customerAddresses.size());

        NautilusQualificationRequest nautilusRequest =
                fiveGCoverageCheckHelper.buildNautilusQualificationRequest(customerAddresses);

        return spectrumAdapterService.checkAddressQualification(nautilusRequest)
                .flatMap(nautilusResponse ->
                        mapNautilusResponseToFiveGCoverageResponse(customerAddresses, nautilusResponse))
                .doOnSuccess(response ->
                        logger.debug("Successfully processed Nautilus qualification for {} addresses",
                                response.getCustomerAddress() != null ? response.getCustomerAddress().size() : 0))
                .doOnError(e ->
                        logger.error("Error calling Nautilus service: {}", e.getMessage(), e))
                .onErrorResume(error ->
                        handleNautilusServiceError(customerAddresses, error));
    }

    /**
     * Maps Nautilus response to FiveGCoverageCheckResponse.
     */
    private Mono<FiveGCoverageCheckResponse> mapNautilusResponseToFiveGCoverageResponse(
            List<CustomerAddress> customerAddresses,
            NautilusQualificationResponse nautilusResponse) {

        if (!isValidNautilusResponse(nautilusResponse)) {
            logger.warn("Nautilus response is null or empty");
            return Mono.just(createFailureResponseForAllAddresses(customerAddresses, SmbConstants.ERROR_NO_QUALIFICATION_DATA));
        }

        List<BulkAddressQualificationResponse> qualificationResponses =
                nautilusResponse.getData().getBulkAddressQualificationResponse();

        Map<String, BulkAddressQualificationResponse> qualificationMap = buildQualificationMap(qualificationResponses);

        List<CustomerAddress> enrichedAddresses = new ArrayList<>(customerAddresses.size());

        for (int index = 0; index < customerAddresses.size(); index++) {
            CustomerAddress originalAddress = customerAddresses.get(index);
            // Record identifier is 1-based (matches FiveGCoverageCheckHelper)
            String recordIdentifier = String.valueOf(index + 1);

            BulkAddressQualificationResponse qualification = qualificationMap.get(recordIdentifier);

            CustomerAddress enrichedAddress = mapQualificationToCustomerAddress(originalAddress, qualification, index);
            enrichedAddresses.add(enrichedAddress);
        }

        FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
        response.setCustomerAddress(enrichedAddresses);
        response.setStatus(SmbConstants.SUCCESS_STATUS);
        response.setErrors(Collections.emptyList());
        response.setBypassAddressValidation(false);

        logger.debug("Successfully mapped {} addresses with Nautilus qualification data", enrichedAddresses.size());
        return Mono.just(response);
    }

    /**
     * Maps qualification response to CustomerAddress.
     */
    private CustomerAddress mapQualificationToCustomerAddress(
            CustomerAddress originalAddress,
            BulkAddressQualificationResponse qualification,
            int addressIndex) {

        CustomerAddress enrichedAddress = createCustomerAddressCopy(originalAddress);

        if (qualification == null) {
            logger.warn("No qualification response found for address at index {}: {}",
                    addressIndex, originalAddress.getAddressLine1());
            enrichedAddress.setQualified(false);
            enrichedAddress.setStatusMsg(SmbConstants.ERROR_NO_QUALIFICATION_FOR_ADDRESS);
            return enrichedAddress;
        }

        boolean isServiceCallSuccessful = SmbConstants.SUCCESS_RETURN_CODE.equals(qualification.getReturnCode());

        enrichedAddress.setQualified(qualification.isFiveGHomeQualified());
        enrichedAddress.setStatusMsg(buildStatusMessage(qualification, isServiceCallSuccessful));

        mapAddressIdentificationInfo(enrichedAddress, qualification.getAddressInfo());
        mapCapacityInfo(enrichedAddress, qualification.getAvailableCapacityInfo());

        enrichedAddress.setQualifiedCBand(qualification.isCBandQualified());
        enrichedAddress.setQualified4GHome(qualification.isLTEQualified());

        mapEligibilities(enrichedAddress, qualification.getEligibilities());

        // ============================================================================
        // FIX #1 (Lines 352-354): Added null check for getPriorQualification()
        // ============================================================================
        mapPriorQualificationFlags(enrichedAddress, qualification.getPriorQualification());

        enrichedAddress.setCbandBYODLine(false);
        enrichedAddress.setFloorPlanAvl(false);

        return enrichedAddress;
    }

    /**
     * FIX #1: Extracted method with null check for PriorQualification mapping.
     * This fixes the NPE issue at lines 352-354.
     */
    private void mapPriorQualificationFlags(CustomerAddress enrichedAddress, PriorQualification priorQualification) {
        if (priorQualification == null) {
            logger.debug("PriorQualification is null, setting default values for wifi backup flags");
            enrichedAddress.setWifiBackupCbandqualified(false);
            enrichedAddress.setWifiBackupLteQualified(false);
            enrichedAddress.setVHILiteQualified(false);
            return;
        }

        enrichedAddress.setWifiBackupCbandqualified(priorQualification.isWifiBackupCbandQualified());
        enrichedAddress.setWifiBackupLteQualified(priorQualification.isWifiBackupLteQualified());
        enrichedAddress.setVHILiteQualified(priorQualification.isVHILiteQualified());
    }

    /**
     * Maps address identification info with null checks.
     * FIX #3 (Lines 441-442): Added null checks for latitude/longitude parsing
     */
    private void mapAddressIdentificationInfo(CustomerAddress enrichedAddress, AddressInfo addressInfo) {
        if (addressInfo == null) {
            logger.debug("AddressInfo is null, setting default values");
            enrichedAddress.setFuzeSiteId(0);
            enrichedAddress.setSector(0);
            enrichedAddress.setLatitude(0.0);
            enrichedAddress.setLongitude(0.0);
            return;
        }

        enrichedAddress.setAddressId(addressInfo.getAddressId());

        String subLocationId = StringUtilities.isNotEmptyOrNull(addressInfo.getLocationId())
                ? addressInfo.getLocationId()
                : addressInfo.getBaseLocationId();
        enrichedAddress.setSubLocationId(subLocationId);

        enrichedAddress.setBuildingId(addressInfo.getBuildingId());
        enrichedAddress.setFloor(addressInfo.getFloor());

        // ============================================================================
        // FIX #3 (Lines 441-442): Parse latitude/longitude WITH null checks
        // ============================================================================
        enrichedAddress.setLatitude(parseDoubleSafely(addressInfo.getLatitude(), "latitude", 0.0));
        enrichedAddress.setLongitude(parseDoubleSafely(addressInfo.getLongitude(), "longitude", 0.0));

        enrichedAddress.setFuzeSiteId(parseIntegerSafely(addressInfo.getFuzeSiteId(), "fuzeSiteId", 0));
        enrichedAddress.setSector(parseIntegerSafely(addressInfo.getSector(), "sector", 0));
    }

    /**
     * FIX #3: Safe double parsing with null check to prevent NPE.
     */
    private double parseDoubleSafely(String value, String fieldName, double defaultValue) {
        if (StringUtilities.isEmptyOrNull(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse {} value '{}' to Double, using default: {}", fieldName, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Maps capacity information.
     */
    private void mapCapacityInfo(CustomerAddress enrichedAddress, AvailableCapacityInfo capacityInfo) {
        if (capacityInfo == null) {
            logger.debug("AvailableCapacityInfo is null, setting default capacity values");
            enrichedAddress.setAvailableCapacityCBand(0);
            enrichedAddress.setAvailableCapacity4GHome(0);
            return;
        }

        enrichedAddress.setAvailableCapacityCBand(
                parseDoubleToIntSafely(capacityInfo.getCbandCapacity(), "cbandCapacity", 0));
        enrichedAddress.setAvailableCapacity4GHome(
                parseDoubleToIntSafely(capacityInfo.getLteCapacity(), "lteCapacity", 0));
    }

    /**
     * Maps eligibilities from qualification response.
     */
    private void mapEligibilities(CustomerAddress enrichedAddress, Eligibilities eligibilities) {
        if (eligibilities == null) {
            logger.debug("Eligibilities is null, skipping eligibility mapping");
            return;
        }

        List<String> allBundleNames = new ArrayList<>();

        if (!CollectionUtilities.isEmptyOrNull(eligibilities.getFiveGHomeBundle())) {
            eligibilities.getFiveGHomeBundle().stream()
                    .filter(bundle -> bundle != null && StringUtilities.isNotEmptyOrNull(bundle.getBundleName()))
                    .map(BundleInfo::getBundleName)
                    .forEach(allBundleNames::add);
        }

        // cbandBundle is now List<BundleInfo> (fixed from String)
        if (!CollectionUtilities.isEmptyOrNull(eligibilities.getCbandBundle())) {
            eligibilities.getCbandBundle().stream()
                    .filter(bundle -> bundle != null && StringUtilities.isNotEmptyOrNull(bundle.getBundleName()))
                    .map(BundleInfo::getBundleName)
                    .forEach(allBundleNames::add);
        }

        if (!allBundleNames.isEmpty()) {
            enrichedAddress.setBundleList(allBundleNames);
        }

        if (StringUtilities.isNotEmptyOrNull(eligibilities.getAvailableSpeedTier())) {
            enrichedAddress.setMaxSpeed(eligibilities.getAvailableSpeedTier());
        }
    }

    // ==================================================================================
    // HELPER METHODS
    // ==================================================================================

    private boolean isValidNautilusResponse(NautilusQualificationResponse nautilusResponse) {
        return nautilusResponse != null
                && nautilusResponse.getData() != null
                && !CollectionUtilities.isEmptyOrNull(nautilusResponse.getData().getBulkAddressQualificationResponse());
    }

    private Map<String, BulkAddressQualificationResponse> buildQualificationMap(
            List<BulkAddressQualificationResponse> qualificationResponses) {

        return qualificationResponses.stream()
                .filter(q -> q != null && q.getRecordIdentifier() != null)
                .collect(Collectors.toMap(
                        BulkAddressQualificationResponse::getRecordIdentifier,
                        Function.identity(),
                        (existing, replacement) -> {
                            logger.warn("Duplicate recordIdentifier found: {}, keeping first occurrence",
                                    existing.getRecordIdentifier());
                            return existing;
                        }
                ));
    }

    /**
     * FIX #8: Use constants from SmbConstants for status messages
     */
    private String buildStatusMessage(BulkAddressQualificationResponse qualification, boolean isServiceCallSuccessful) {
        if (isServiceCallSuccessful && qualification.isFiveGHomeQualified()) {
            return SmbConstants.STATUS_MSG_QUALIFIED;
        }

        if (StringUtilities.isNotEmptyOrNull(qualification.getReturnMessage())) {
            return qualification.getReturnMessage();
        }

        return SmbConstants.STATUS_MSG_NOT_QUALIFIED;
    }

    private Integer parseIntegerSafely(String value, String fieldName, int defaultValue) {
        if (StringUtilities.isEmptyOrNull(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse {} value '{}' to Integer, using default: {}", fieldName, value, defaultValue);
            return defaultValue;
        }
    }

    private Integer parseDoubleToIntSafely(String value, String fieldName, int defaultValue) {
        if (StringUtilities.isEmptyOrNull(value)) {
            return defaultValue;
        }
        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse {} value '{}' to Integer, using default: {}", fieldName, value, defaultValue);
            return defaultValue;
        }
    }

    // ==================================================================================
    // ERROR HANDLING
    // ==================================================================================

    private Mono<FiveGCoverageCheckResponse> handleNautilusServiceError(
            List<CustomerAddress> customerAddresses, Throwable error) {

        logger.error("Handling Nautilus error gracefully: {}", error.getMessage());
        return Mono.just(createFailureResponseForAllAddresses(customerAddresses,
                SmbConstants.ERROR_5G_COVERAGE_CHECK_FAILED + error.getMessage()));
    }

    private FiveGCoverageCheckResponse createFailureResponseForAllAddresses(
            List<CustomerAddress> customerAddresses, String errorMessage) {

        List<CustomerAddress> failedAddresses = customerAddresses.stream()
                .map(addr -> {
                    CustomerAddress copy = createCustomerAddressCopy(addr);
                    copy.setQualified(false);
                    copy.setStatusMsg(errorMessage);
                    return copy;
                })
                .collect(Collectors.toList());

        FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
        response.setCustomerAddress(failedAddresses);
        response.setStatus(SmbConstants.SUCCESS_STATUS);
        response.setErrors(Collections.emptyList());
        response.setBypassAddressValidation(false);
        return response;
    }

    private FiveGCoverageCheckResponse createEmptyResponse() {
        FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
        response.setCustomerAddress(Collections.emptyList());
        response.setStatus(SmbConstants.SUCCESS_STATUS);
        response.setErrors(Collections.emptyList());
        response.setBypassAddressValidation(false);
        return response;
    }

    // ==================================================================================
    // ADDRESS COPY UTILITIES
    // ==================================================================================

    /**
     * Creates a deep copy of CustomerAddress to maintain immutability in reactive streams.
     * This is CRITICAL - never mutate the original address in reactive chains.
     */
    private CustomerAddress createCustomerAddressCopy(CustomerAddress original) {
        if (original == null) {
            return new CustomerAddress();
        }

        CustomerAddress copy = new CustomerAddress();

        // Copy basic address fields
        copy.setAddressLine1(original.getAddressLine1());
        copy.setAddressLine2(original.getAddressLine2());
        copy.setCity(original.getCity());
        copy.setState(original.getState());
        copy.setZipCode(original.getZipCode());
        copy.setZipCodePlus4(original.getZipCodePlus4());
        copy.setCountry(original.getCountry());
        copy.setAddressType(original.getAddressType());

        // Copy parsed address components
        copy.setStreetNum(original.getStreetNum());
        copy.setStreetName(original.getStreetName());
        copy.setType(original.getType());
        copy.setDir(original.getDir());
        copy.setAptNumber(original.getAptNumber());
        copy.setPoBoxNo(original.getPoBoxNo());

        // Copy address descriptors (shallow copy of list)
        if (original.getAddressDesc() != null) {
            copy.setAddressDesc(new ArrayList<>(original.getAddressDesc()));
        }

        // Copy status fields
        copy.setQualified(original.isQualified());
        copy.setStatusMsg(original.getStatusMsg());

        return copy;
    }

    /**
     * Creates updated CustomerAddress from Split Address response.
     */
    private CustomerAddress createUpdatedAddressFromSplit(
            CustomerAddress original,
            Address validatedAddress) {

        CustomerAddress updated = new CustomerAddress();

        // Preserve original metadata
        updated.setAddressType(original.getAddressType());
        updated.setCountry(original.getCountry());
        updated.setAddressDesc(original.getAddressDesc());

        // Set validated/parsed address fields from Split Address response
        updated.setStreetNum(validatedAddress.getStreetNum());
        updated.setStreetName(validatedAddress.getStreetName());
        updated.setAptNumber(validatedAddress.getAptNum());
        updated.setPoBoxNo(validatedAddress.getPobox());
        updated.setType(validatedAddress.getType());
        updated.setDir(validatedAddress.getDir());

        // Reconstruct addressLine1 from parsed components
        String addressLine1 = buildAddressLine1(validatedAddress);
        updated.setAddressLine1(addressLine1);

        // Set addressLine2 - prefer aptNum, fallback to original
        updated.setAddressLine2(StringUtilities.isNotEmptyOrNull(validatedAddress.getAptNum())
                ? validatedAddress.getAptNum()
                : original.getAddressLine2());

        // Set location fields
        updated.setCity(validatedAddress.getCity());
        updated.setState(validatedAddress.getState());
        updated.setZipCode(validatedAddress.getZipCode());
        updated.setZipCodePlus4(validatedAddress.getZipCode4());

        return updated;
    }

    /**
     * Builds addressLine1 from parsed address components.
     */
    private String buildAddressLine1(Address address) {
        StringBuilder sb = new StringBuilder();

        if (StringUtilities.isNotEmptyOrNull(address.getStreetNum())) {
            sb.append(address.getStreetNum()).append(" ");
        }
        if (StringUtilities.isNotEmptyOrNull(address.getStreetName())) {
            sb.append(address.getStreetName()).append(" ");
        }
        if (StringUtilities.isNotEmptyOrNull(address.getType())) {
            sb.append(address.getType());
        }

        return sb.toString().trim().replaceAll("\\s{2,}", " ");
    }

    // ==================================================================================
    // VALIDATION METHODS
    // ==================================================================================

    public boolean hasInvalidCustomerAddress(List<CustomerAddress> customerAddresses) {
        return CollectionUtilities.isEmptyOrNull(customerAddresses) ||
                customerAddresses.stream().anyMatch(address ->
                        address == null ||
                                StringUtilities.isEmptyOrNull(address.getAddressLine1()) ||
                                StringUtilities.isEmptyOrNull(address.getZipCode())
                );
    }

    public boolean isValidSplitAddressResponse(SplitAddressResponse splitAddressResponse) {
        return splitAddressResponse != null
                && splitAddressResponse.getData() != null
                && splitAddressResponse.getData().getAddressSplit() != null
                && splitAddressResponse.getData().getAddressSplit().getResponse() != null
                && splitAddressResponse.getData().getAddressSplit().getResponse().getAddress() != null;
    }

    private Address extractSplitAddress(SplitAddressResponse splitAddressResponse) {
        return splitAddressResponse.getData().getAddressSplit().getResponse().getAddress();
    }

    // ==================================================================================
    // RESPONSE BUILDERS
    // ==================================================================================

    private SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> buildSuccessResponse(
            FiveGCoverageCheckResponse response) {

        SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> wrapper = new SoeDataWrapper<>();
        SmbResponseWrapper<FiveGCoverageCheckResponse> smbWrapper = new SmbResponseWrapper<>();
        smbWrapper.setResponse(response);
        smbWrapper.setStatusCode(SmbConstants.SUCCESS_STATUS);
        wrapper.setData(smbWrapper);
        return wrapper;
    }

    private Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> buildErrorResponse(Throwable error) {
        logger.error("Failed to process 5G coverage check: {}", error.getMessage(), error);
        SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> errorWrapper = new SoeDataWrapper<>();
        SmbResponseWrapper<FiveGCoverageCheckResponse> smbWrapper = new SmbResponseWrapper<>();
        smbWrapper.setStatusCode(SmbConstants.FAILURE_STATUS);
        smbWrapper.setErrors(ValidationUtils.formatErrorMessage(error.getMessage()));
        errorWrapper.setData(smbWrapper);
        return Mono.just(errorWrapper);
    }
}
