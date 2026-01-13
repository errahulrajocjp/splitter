package org.example;

import onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress;
import onevz.soe.smbenrollment.requests.spectrumadapter.SplitAddressRequest;
import onevz.soe.smbenrollment.responses.SmbResponseWrapper;
import onevz.soe.smbenrollment.responses.SoeDataWrapper;
import onevz.soe.smbenrollment.responses.spectrumadapter.Address;
import onevz.soe.smbenrollment.responses.spectrumadapter.FiveGCoverageCheckResponse;
import onevz.soe.smbenrollment.responses.spectrumadapter.SplitAddressResponse;
import onevz.soe.smbenrollment.constants.SmbConstants;
import onevz.soe.smbenrollment.utils.ValidationUtils;
import onevz.soe.util.CollectionUtilities;
import onevz.soe.util.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ValidateBusinessAccountHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateBusinessAccountHelper.class);
    
    // Inject dependencies
    private FiveGCoverageCheckHelper fiveGCoverageCheckHelper;
    private SpectrumAdapterService spectrumAdapterService;

    // Lines 205-248 - validateSplitAddress main method
    //Rahul
    public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(  // 1 usage  new *
            FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
        
        logger.info("=== Starting validateSplitAddress ===");
        logger.info("Request received with {} customer addresses", 
                fiveGCoverageCheckRequest.getCustomerAddress() != null ? 
                        fiveGCoverageCheckRequest.getCustomerAddress().size() : 0);
        
        // Fixed: Inverted the condition to process VALID addresses through split address service
        if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
            logger.info("Customer addresses are VALID. Proceeding with split address service calls.");
            List<Mono<Void>> addressProcessingTasks = new ArrayList<>();
            
            for (CustomerAddress customerAddress : fiveGCoverageCheckRequest.getCustomerAddress()) {
                logger.info("Processing address: {}, {}, {}", 
                        customerAddress.getAddressLine1(), 
                        customerAddress.getCity(), 
                        customerAddress.getZipcode());
                
                SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);
                logger.info("Built split address request for: {}", customerAddress.getAddressLine1());
                
                Mono<Void> task = spectrumAdapterService.splitAddress(splitAddressRequest)
                        .doOnSubscribe(subscription -> 
                                logger.info(">>> Calling split address service for: {}", customerAddress.getAddressLine1()))
                        .doOnNext( addressResponse -> {
                            logger.info("<<< Received response from split address service for: {}", customerAddress.getAddressLine1());
                            logger.debug("Split address response: {}", addressResponse);
                            
                            if (isValidSplitAddressResponse(addressResponse)) {
                                logger.info("Split address response is VALID");
                                onevz.soe.smbenrollment.responses.spectrumadapter.Address address = extractSplitAddress(addressResponse);
                                if (address != null) {
                                    logger.info("Extracted address: streetNum={}, streetName={}, city={}, state={}, zip={}", 
                                            address.getStreetNum(), address.getStreetName(), 
                                            address.getCity(), address.getState(), address.getZipcode());
                                    updateCustomerAddress(customerAddress, address);
                                    logger.info("Customer address updated successfully from split address service");
                                } else {
                                    logger.warn("Extracted address is NULL");
                                }
                            } else {
                                logger.warn("Split address response is INVALID or incomplete");
                            }
                        })
                        .doOnError( e -> logger.error("!!! Error occurred from cxp spectrum service for address: {}. Error: {}", 
                                customerAddress.getAddressLine1(), e.getMessage(), e))
                        .onErrorResume( error -> {
                            logger.error("Handling error gracefully for address: {}", customerAddress.getAddressLine1());
                            return Mono.empty();
                        })
                        .then();
                
                addressProcessingTasks.add(task);
            }
            
            logger.info("Total split address tasks created: {}", addressProcessingTasks.size());
            
            return Mono.when(addressProcessingTasks)
                    .doOnSuccess(v -> logger.info("All split address service calls completed successfully"))
                    .doOnError(e -> logger.error("Error in split address processing: {}", e.getMessage(), e))
                    .then(processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest))
                    .map(this::buildSuccessResponse)
                    .onErrorResume(this::buildErrorResponse);
        }
        
        logger.warn("Customer addresses are INVALID. Skipping split address service calls.");
        return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest)
                .map(this::buildSuccessResponse)
                .onErrorResume(this::buildErrorResponse);
    }
    
    private Mono<FiveGCoverageCheckResponse> processValidateCustomerAccountForFiveGCoverage(FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
        logger.info("=== processValidateCustomerAccountForFiveGCoverage called ===");
        logger.info("This is where nautilicious service call will be made");
        
        // Build response with the processed customer addresses
        FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
        
        // If CustomerAddress in request and response are DIFFERENT types from different packages,
        // we need to convert them. Check if compilation fails with type mismatch, then use conversion.
        // If they're the same type, direct assignment works fine.
        
        // Option 1: Direct assignment (if same type or compatible)
        // response.setCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress());
        
        // Option 2: Convert if types are different (uncomment if needed)
        // List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> responseAddresses = 
        //     convertToResponseCustomerAddresses(fiveGCoverageCheckRequest.getCustomerAddress());
        // response.setCustomerAddress(responseAddresses);
        
        // For now, attempting direct assignment - if this causes compilation error,
        // we'll need to implement the conversion method
        response.setCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress());
        
        logger.info("Returning response with {} customer addresses", 
                fiveGCoverageCheckRequest.getCustomerAddress().size());
        
        // TODO: Before returning, call nautilicious service here
        // Example:
        // return nautiliciousService.checkCoverage(fiveGCoverageCheckRequest)
        //     .map(coverageResponse -> {
        //         // Merge coverage response into FiveGCoverageCheckResponse
        //         response.setCustomerAddress(coverageResponse.getAddresses());
        //         return response;
        //     });
        
        return Mono.just(response);
    }
    
    // Helper method to convert request CustomerAddress to response CustomerAddress
    // Uncomment and use this if the CustomerAddress types are different
    /*
    private List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> convertToResponseCustomerAddresses(
            List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> requestAddresses) {
        
        if (CollectionUtilities.isEmptyOrNull(requestAddresses)) {
            return new ArrayList<>();
        }
        
        return requestAddresses.stream()
            .map(this::convertToResponseCustomerAddress)
            .collect(Collectors.toList());
    }
    
    private onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress convertToResponseCustomerAddress(
            onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress requestAddress) {
        
        onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress responseAddress = 
            new onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress();
        
        // Copy all fields from request to response
        responseAddress.setStreetNum(requestAddress.getStreetNum());
        responseAddress.setStreetName(requestAddress.getStreetName());
        responseAddress.setAptNumber(requestAddress.getAptNumber());
        responseAddress.setPoBoxNo(requestAddress.getPoBoxNo());
        responseAddress.setCountry(requestAddress.getCountry());
        responseAddress.setAddressLine1(requestAddress.getAddressLine1());
        responseAddress.setAddressLine2(requestAddress.getAddressLine2());
        responseAddress.setCity(requestAddress.getCity());
        responseAddress.setState(requestAddress.getState());
        responseAddress.setZipcode(requestAddress.getZipcode());
        responseAddress.setZipcodeplus4(requestAddress.getZipcodeplus4());
        responseAddress.setType(requestAddress.getType());
        responseAddress.setDir(requestAddress.getDir());
        responseAddress.setAddressType(requestAddress.getAddressType());
        responseAddress.setAddressDesc(requestAddress.getAddressDesc());
        responseAddress.setQualified(requestAddress.isQualified());
        responseAddress.setStatusMsg(requestAddress.getStatusMsg());
        responseAddress.setEventCorrelationId(requestAddress.getEventCorrelationId());
        
        return responseAddress;
    }
    */

    // Lines 250-270 - updateCustomerAddress method
    private void updateCustomerAddress(CustomerAddress customerAddress,  // 1 usage  new *
            onevz.soe.smbenrollment.responses.spectrumadapter.Address address) {
        
        customerAddress.setStreetNum(address.getStreetNum());
        customerAddress.setStreetName(address.getStreetName());
        customerAddress.setAptNumber(address.getAptNum());
        customerAddress.setPoBoxNo(address.getPobox());  // Fixed typo: was getPoboxO()
        
        String addressLine1 = (address.getStreetNum() != null ? address.getStreetNum() + " " : "") +
                (address.getStreetName() != null ? address.getStreetName() + " " : "") +
                (address.getType() != null ? address.getType() : "");
        customerAddress.setAddressLine1(addressLine1.trim().replaceAll( "\\s{2,}", " "));
        
        if (!StringUtilities.isEmptyOrNull(address.getAptNum())) {
            customerAddress.setAddressLine2(address.getAptNum());
        }
        
        customerAddress.setCity(address.getCity());
        customerAddress.setState(address.getState());
        customerAddress.setZipcode(address.getZipcode());
        customerAddress.setZipcodeplus4(address.getZipcode4());  // Fixed: was setZipCodePlus4
    }

    // Lines 272-280 - buildSuccessResponse method
    private SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> buildSuccessResponse(  // 2 usages  new *
            FiveGCoverageCheckResponse response) {
        
        SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> wrapper = new SoeDataWrapper<>();
        SmbResponseWrapper<FiveGCoverageCheckResponse> smbWrapper = new SmbResponseWrapper<>();
        smbWrapper.setResponse(response);
        smbWrapper.setStatusCode(SmbConstants.SUCCESS_STATUS);
        wrapper.setData(smbWrapper);
        return wrapper;
    }
    
    // Lines 282-289 - buildErrorResponse method
    private Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> buildErrorResponse(Throwable error) {
        logger.error("Failed to validate split address: {}", error.getMessage(), error);
        SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>> errorWrapper = new SoeDataWrapper<>();
        SmbResponseWrapper<FiveGCoverageCheckResponse> smbWrapper = new SmbResponseWrapper<>();
        smbWrapper.setStatusCode(SmbConstants.FAILURE_STATUS);
        smbWrapper.setErrors(ValidationUtils.formatErrorMessage(error.getMessage()));
        errorWrapper.setData(smbWrapper);
        return Mono.just(errorWrapper);
    }

    // Lines 326-331
    public boolean isValidSplitAddressResponse(SplitAddressResponse splitAddressResponse) {  // 6 usages
        return splitAddressResponse != null
                && splitAddressResponse.getData().getAddressSplit().getResponse().getAddress() != null;
    }
    
    // Lines 337-345
    //Rahul
    public boolean isInValidCustomerAddress(List<CustomerAddress> customerAddresses) {  // 1 usage  new *
        return CollectionUtilities.isEmptyOrNull(customerAddresses) ||
                customerAddresses.stream().anyMatch( address ->
                        address == null ||
                        StringUtilities.isEmptyOrNull(address.getAddressLine1()) ||
                        StringUtilities.isEmptyOrNull(address.getZipcode())
                );
    }


    public boolean isValidSplitAddressResponse(SplitAddressResponse splitAddressResponse) {  // 6 usages  ± mutta9
        return splitAddressResponse.getData() != null
                && splitAddressResponse.getData().getAddressSplit() != null
                && splitAddressResponse.getData().getAddressSplit().getResponse() != null
                && splitAddressResponse.getData().getAddressSplit().getResponse().getAddress() != null;
    }
    

}
