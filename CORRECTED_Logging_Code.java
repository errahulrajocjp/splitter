// CORRECTED VERSION - Replace your processValidateCustomerAccountForFiveGCoverage method with this:

private Mono<FiveGCoverageCheckResponse> processValidateCustomerAccountForFiveGCoverage(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("=== processValidateCustomerAccountForFiveGCoverage called ===");
    logger.info("This is where nautilicious service call will be made");
    
    // LOG THE REQUEST ADDRESSES **BEFORE** CONVERSION
    logger.info("=== BEFORE CONVERSION - Logging Request Addresses ===");
    List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> reqAddresses = 
        fiveGCoverageCheckRequest.getCustomerAddress();
    
    for (int i = 0; i < reqAddresses.size(); i++) {
        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = reqAddresses.get(i);
        logger.info("REQUEST Address[" + i + "]: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], aptNumber=[" + addr.getAptNumber() + "], addressLine1=[" + addr.getAddressLine1() + "], city=[" + addr.getCity() + "], state=[" + addr.getState() + "], zipcode=[" + addr.getZipcode() + "]");
    }
    
    logger.info("Returning response with " + fiveGCoverageCheckRequest.getCustomerAddress().size() + " customer addresses");
    
    // For now, return a valid response with the updated customer addresses
    FiveGCoverageCheckResponse response = new FiveGCoverageCheckResponse();
    
    List<onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress> responseAddresses = 
            convertToResponseCustomerAddresses(fiveGCoverageCheckRequest.getCustomerAddress());
    
    // LOG THE RESPONSE ADDRESSES **AFTER** CONVERSION
    logger.info("=== AFTER CONVERSION - Logging Response Addresses ===");
    for (int i = 0; i < responseAddresses.size(); i++) {
        onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress addr = responseAddresses.get(i);
        logger.info("RESPONSE Address[" + i + "]: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], aptNumber=[" + addr.getAptNumber() + "], addressLine1=[" + addr.getAddressLine1() + "], city=[" + addr.getCity() + "], state=[" + addr.getState() + "], zipcode=[" + addr.getZipcode() + "]");
    }
    
    response.setCustomerAddress(responseAddresses);
    
    return Mono.just(response);
}


// CORRECTED VERSION - Replace your convertToResponseCustomerAddress method with this:

private onevz.soe.smbenrollment.responses.spectrumadapter.CustomerAddress convertToResponseCustomerAddress(
        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress requestAddress) {
    
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
