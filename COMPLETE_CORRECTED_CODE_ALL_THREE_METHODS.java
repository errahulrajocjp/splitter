// ============================================================================
// COMPLETE CORRECTED CODE - Copy ALL of these methods to your class
// ============================================================================

// Method 1: processValidateCustomerAccountForFiveGCoverage
private Mono<FiveGCoverageCheckResponse> processValidateCustomerAccountForFiveGCoverage(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("=== processValidateCustomerAccountForFiveGCoverage called ===");
    logger.info("This is where nautilicious service call will be made");
    
    // LOG THE REQUEST ADDRESSES **BEFORE** CONVERSION
    logger.info("=== BEFORE CONVERSION - Logging Request Addresses ===");
    List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> reqAddresses = 
        fiveGCoverageCheckRequest.getCustomerAddress();
    
    if (reqAddresses != null) {
        for (int i = 0; i < reqAddresses.size(); i++) {
            onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = reqAddresses.get(i);
            logger.info("REQUEST Address[" + i + "]: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], aptNumber=[" + addr.getAptNumber() + "], addressLine1=[" + addr.getAddressLine1() + "], city=[" + addr.getCity() + "], state=[" + addr.getState() + "], zipcode=[" + addr.getZipcode() + "]");
        }
    }
    
    logger.info("Returning response with " + (reqAddresses != null ? reqAddresses.size() : 0) + " customer addresses");
    
    // For now, return a valid response with the updated customer addresses
    FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
    
    // Convert from request to response CustomerAddress
    List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> responseAddresses = 
            convertToResponseCustomerAddresses(fiveGCoverageCheckRequest.getCustomerAddress());
    
    // LOG THE RESPONSE ADDRESSES **AFTER** CONVERSION
    logger.info("=== AFTER CONVERSION - Logging Response Addresses ===");
    if (responseAddresses != null) {
        for (int i = 0; i < responseAddresses.size(); i++) {
            onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress addr = responseAddresses.get(i);
            logger.info("RESPONSE Address[" + i + "]: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], aptNumber=[" + addr.getAptNumber() + "], addressLine1=[" + addr.getAddressLine1() + "], city=[" + addr.getCity() + "], state=[" + addr.getState() + "], zipcode=[" + addr.getZipcode() + "]");
        }
    }
    
    response.setCustomerAddress(responseAddresses);
    
    return Mono.just(response);
}


// Method 2: convertToResponseCustomerAddresses (PLURAL - converts List)
private List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> convertToResponseCustomerAddresses(
        List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> requestAddresses) {
    
    if (CollectionUtilities.isEmptyOrNull(requestAddresses)) {
        logger.info("Request addresses list is empty or null, returning empty list");
        return new ArrayList<>();
    }
    
    logger.info("Converting " + requestAddresses.size() + " request addresses to response addresses");
    
    List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> responseAddresses = new ArrayList<>();
    
    for (onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress requestAddr : requestAddresses) {
        onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress responseAddr = 
            convertToResponseCustomerAddress(requestAddr);
        responseAddresses.add(responseAddr);
    }
    
    logger.info("Converted " + responseAddresses.size() + " addresses successfully");
    
    return responseAddresses;
}


// Method 3: convertToResponseCustomerAddress (SINGULAR - converts single object)
private onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress convertToResponseCustomerAddress(
        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress requestAddress) {
    
    if (requestAddress == null) {
        logger.warn("Request address is null, returning null");
        return null;
    }
    
    logger.info(">>> CONVERTING SINGLE ADDRESS - INPUT REQUEST: streetNum=[" + requestAddress.getStreetNum() + "], streetName=[" + requestAddress.getStreetName() + "], addressLine1=[" + requestAddress.getAddressLine1() + "]");
    
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
    
    logger.info("<<< CONVERTED SINGLE ADDRESS - OUTPUT RESPONSE: streetNum=[" + responseAddress.getStreetNum() + "], streetName=[" + responseAddress.getStreetName() + "], addressLine1=[" + responseAddress.getAddressLine1() + "]");
    
    return responseAddress;
}


// ============================================================================
// IMPORTANT NOTES:
// ============================================================================
// 1. You need ALL THREE methods above
// 2. Method 1 calls Method 2 (plural)
// 3. Method 2 calls Method 3 (singular) in a loop
// 4. This avoids using Java Streams which might cause issues
// 5. All compilation errors should be resolved
// ============================================================================
