// =====================================================================
// TEST CASES FOR 100% COVERAGE - ValidateBusinessAccountHelperTest
// Based on SonarQube Report - Partially Covered & Uncovered Lines
// =====================================================================

// =====================================================================
// SETUP: Add these mocks in setUp() if not already present
// =====================================================================
/*
// Mock for null handling in CollectionUtilities.isEmptyOrNull
mockedCollectionUtilities.when(() -> CollectionUtilities.isEmptyOrNull((List<?>) null))
        .thenReturn(true);

mockedCollectionUtilities.when(() -> CollectionUtilities.isEmptyOrNull(any(List.class)))
        .thenAnswer(invocation -> {
            List<?> list = invocation.getArgument(0);
            return list == null || list.isEmpty();
        });
*/

// =====================================================================
// LINE 224: if (CollectionUtilities.isEmptyOrNull(validatedAddresses)) {
// Need to test when validatedAddresses IS empty/null (true branch)
// =====================================================================

@Test
void testCheck5GCoverageForAddresses_ValidatedAddressesEmpty_ReturnsErrorResponse() {
    // Setup: Create request with addresses that will ALL fail split validation
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    request.setCustomerAddress(addresses);
    
    // Mock: splitAddress returns invalid response for all addresses
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(new SplitAddressResponse())); // No data = invalid
    
    // Mock: After collectList, validatedAddresses will be empty
    mockedCollectionUtilities.when(() -> CollectionUtilities.isEmptyOrNull(any(List.class)))
            .thenReturn(true);
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> 
                    response.getData().getStatusCode() == SmbConstants.FAILURE_STATUS)
            .verifyComplete();
}

// =====================================================================
// LINE 225: return buildErrorResponse(new IllegalStateException(
//           SmbConstants.ERROR_ALL_ADDRESSES_FAILED_SPLIT));
// Need: All addresses fail split validation
// =====================================================================

@Test
void testCheck5GCoverageForAddresses_AllAddressesFailSplit_ReturnsErrorResponse() {
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    addresses.add(createCustomerAddress("456 Oak Ave", "20002"));
    request.setCustomerAddress(addresses);
    
    // Mock: splitAddress throws error for all addresses
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Split failed")));
    
    // Mock fiveGCoverageCheckHelper
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                // All addresses should have failed status
                return response.getData() != null;
            })
            .verifyComplete();
}

// =====================================================================
// LINE 228-230: boolean allFailed = validatedAddresses.stream().allMatch(addr ->
//               addr.getStatusMsg() != null && addr.getStatusMsg().startsWith("Address validation failed"));
// Need: Test when allFailed is TRUE (all addresses have failure status)
// =====================================================================

@Test
void testCheck5GCoverageForAddresses_AllAddressesHaveFailureStatus_ReturnsEarlyWithFailedAddresses() {
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    request.setCustomerAddress(addresses);
    
    // Create a failed split address response (no valid address data)
    SplitAddressResponse invalidSplitResponse = new SplitAddressResponse();
    // No data set = isValidSplitAddressResponse returns false
    
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(invalidSplitResponse));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                List<CustomerAddress> resultAddresses = response.getData().getResponse().getCustomerAddress();
                // All addresses should have "Address validation failed" in statusMsg
                return resultAddresses != null && resultAddresses.stream()
                        .allMatch(addr -> addr.getStatusMsg() != null && 
                                addr.getStatusMsg().contains("failed"));
            })
            .verifyComplete();
}

// =====================================================================
// LINE 253: if (address != null) {
// Need: Test when extracted address IS null
// =====================================================================

@Test
void testProcessSplitAddressForCustomer_ExtractedAddressIsNull_ReturnsFailedAddress() {
    CustomerAddress customerAddress = createCustomerAddress("123 Main St", "10001");
    
    // Create split response where address is null
    SplitAddressResponse splitResponse = new SplitAddressResponse();
    SplitAddressResponse.SplitAddressResponseData data = new SplitAddressResponse.SplitAddressResponseData();
    AddressSplit addressSplit = new AddressSplit();
    AddressSplit.AddressSplitServiceResponse serviceResponse = new AddressSplit.AddressSplitServiceResponse();
    serviceResponse.setAddress(null);  // address is null
    addressSplit.setResponse(serviceResponse);
    data.setAddressSplit(addressSplit);
    splitResponse.setData(data);
    
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(splitResponse));
    
    // Call the method through the main flow
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(Collections.singletonList(customerAddress));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                // Address should be marked as failed
                CustomerAddress result = response.getData().getResponse().getCustomerAddress().get(0);
                return !result.isQualified() && 
                       result.getStatusMsg() != null && 
                       result.getStatusMsg().contains("failed");
            })
            .verifyComplete();
}

// =====================================================================
// LINE 279: if (CollectionUtilities.isEmptyOrNull(customerAddresses)) {
// Need: Test processValidateCustomerAccountForFiveGCoverage with empty list
// =====================================================================

@Test
void testProcessValidateCustomerAccountForFiveGCoverage_EmptyAddresses_ReturnsEmptyResponse() {
    List<CustomerAddress> emptyAddresses = new ArrayList<>();
    
    Mono<FiveGCoverageCheckResponse> result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "processValidateCustomerAccountForFiveGCoverage",
            emptyAddresses
    );
    
    StepVerifier.create(result)
            .expectNextMatches(response -> 
                    response.getCustomerAddress() == null || 
                    response.getCustomerAddress().isEmpty())
            .verifyComplete();
}

// =====================================================================
// LINE 297-298: response != null && response.getCustomerAddress() != null ?
//               response.getCustomerAddress().size() : 0))
// Need: Test when response or response.getCustomerAddress() is null
// =====================================================================

@Test
void testProcessValidateCustomerAccountForFiveGCoverage_NautilusReturnsNullResponse() {
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    
    when(fiveGCoverageCheckHelper.buildNautilusQualificationRequest(any(List.class)))
            .thenReturn(new NautilusQualificationRequest());
    
    // Nautilus returns response with null customerAddress
    NautilusQualificationResponse nautilusResponse = new NautilusQualificationResponse();
    nautilusResponse.setData(null);
    
    when(spectrumAdapterService.checkAddressQualification(any(NautilusQualificationRequest.class)))
            .thenReturn(Mono.just(nautilusResponse));
    
    Mono<FiveGCoverageCheckResponse> result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "processValidateCustomerAccountForFiveGCoverage",
            addresses
    );
    
    StepVerifier.create(result)
            .expectNextMatches(response -> response != null)
            .verifyComplete();
}

// =====================================================================
// LINE 346: if (!wasProcessed) {
// Need: Test address that was NOT processed (skipped because missing required fields)
// =====================================================================

@Test
void testMapNautilusResponseToFiveGCoverageResponse_AddressNotProcessed_MarkedAsSkipped() {
    // Create original addresses - one valid, one invalid (will be skipped)
    List<CustomerAddress> originalAddresses = new ArrayList<>();
    
    CustomerAddress validAddress = createCustomerAddress("123 Main St", "10001");
    originalAddresses.add(validAddress);
    
    CustomerAddress invalidAddress = new CustomerAddress();
    invalidAddress.setAddressLine1(null);  // Missing required field
    invalidAddress.setZipCode(null);       // Missing required field
    originalAddresses.add(invalidAddress);
    
    // validAddressesForNautilus will only contain the valid address
    List<CustomerAddress> validAddressesForNautilus = new ArrayList<>();
    validAddressesForNautilus.add(validAddress);
    
    // Create Nautilus response for only the valid address
    NautilusQualificationResponse nautilusResponse = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    List<BulkAddressQualificationResponse> qualResponses = new ArrayList<>();
    
    BulkAddressQualificationResponse qualResponse = new BulkAddressQualificationResponse();
    qualResponse.setRecordIdentifier("1");
    qualResponse.setFiveGHomeQualified(true);
    qualResponse.setReturnMessage("Eligible");
    qualResponses.add(qualResponse);
    
    data.setBulkAddressQualificationResponse(qualResponses);
    nautilusResponse.setData(data);
    
    Mono<FiveGCoverageCheckResponse> result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapNautilusResponseToFiveGCoverageResponse",
            originalAddresses,
            validAddressesForNautilus,
            nautilusResponse
    );
    
    StepVerifier.create(result)
            .expectNextMatches(response -> {
                List<CustomerAddress> resultAddresses = response.getCustomerAddress();
                // Should have 2 addresses - one qualified, one skipped
                return resultAddresses.size() == 2 &&
                       resultAddresses.stream().anyMatch(a -> 
                               a.getStatusMsg() != null && 
                               a.getStatusMsg().contains("skipped"));
            })
            .verifyComplete();
}

// =====================================================================
// LINE 362: if (addr1 == null
// Need: Test isSameAddress when addr1 is null
// =====================================================================

@Test
void testIsSameAddress_Addr1IsNull_ReturnsFalse() {
    CustomerAddress addr2 = createCustomerAddress("123 Main St", "10001");
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isSameAddress",
            (CustomerAddress) null,
            addr2
    );
    
    assertFalse(result);
}

@Test
void testIsSameAddress_Addr2IsNull_ReturnsFalse() {
    CustomerAddress addr1 = createCustomerAddress("123 Main St", "10001");
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isSameAddress",
            addr1,
            (CustomerAddress) null
    );
    
    assertFalse(result);
}

@Test
void testIsSameAddress_BothNull_ReturnsFalse() {
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isSameAddress",
            (CustomerAddress) null,
            (CustomerAddress) null
    );
    
    assertFalse(result);
}

// =====================================================================
// LINE 467: if (qualificationResponses == null) {
// Need: Test buildQualificationMap with null input
// =====================================================================

@Test
void testBuildQualificationMap_NullInput_ReturnsEmptyMap() {
    Map<String, BulkAddressQualificationResponse> result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildQualificationMap",
            (List<BulkAddressQualificationResponse>) null
    );
    
    assertNotNull(result);
    assertTrue(result.isEmpty());
}

@Test
void testBuildQualificationMap_EmptyList_ReturnsEmptyMap() {
    List<BulkAddressQualificationResponse> emptyList = new ArrayList<>();
    
    Map<String, BulkAddressQualificationResponse> result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildQualificationMap",
            emptyList
    );
    
    assertNotNull(result);
    assertTrue(result.isEmpty());
}

// =====================================================================
// LINE 485: if (isServiceCallSuccessful && qualification.isFiveGHomeQualified()) {
// Need: Test buildStatusMessage when BOTH conditions are true AND when one is false
// =====================================================================

@Test
void testBuildStatusMessage_ServiceSuccessfulAndFiveGQualified_ReturnsQualifiedMsg() {
    BulkAddressQualificationResponse qualification = new BulkAddressQualificationResponse();
    qualification.setFiveGHomeQualified(true);
    qualification.setReturnMessage("Eligible");
    
    String result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildStatusMessage",
            qualification,
            true  // isServiceCallSuccessful
    );
    
    assertEquals(SmbConstants.QUALIFIED_STATUS_MSG, result);
}

@Test
void testBuildStatusMessage_ServiceSuccessfulButNotFiveGQualified_ReturnsReturnMessage() {
    BulkAddressQualificationResponse qualification = new BulkAddressQualificationResponse();
    qualification.setFiveGHomeQualified(false);
    qualification.setReturnMessage("Not Eligible - No Coverage");
    
    String result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildStatusMessage",
            qualification,
            true  // isServiceCallSuccessful
    );
    
    assertEquals("Not Eligible - No Coverage", result);
}

@Test
void testBuildStatusMessage_ServiceNotSuccessful_ReturnsNotQualifiedMsg() {
    BulkAddressQualificationResponse qualification = new BulkAddressQualificationResponse();
    qualification.setFiveGHomeQualified(true);  // Even if qualified
    qualification.setReturnMessage(null);
    
    String result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildStatusMessage",
            qualification,
            false  // isServiceCallSuccessful = false
    );
    
    assertEquals(SmbConstants.NOT_QUALIFIED_STATUS_MSG, result);
}

@Test
void testBuildStatusMessage_ReturnMessageIsNull_ReturnsNotQualifiedMsg() {
    BulkAddressQualificationResponse qualification = new BulkAddressQualificationResponse();
    qualification.setFiveGHomeQualified(false);
    qualification.setReturnMessage(null);
    
    String result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildStatusMessage",
            qualification,
            true
    );
    
    assertEquals(SmbConstants.NOT_QUALIFIED_STATUS_MSG, result);
}

@Test
void testBuildStatusMessage_ReturnMessageIsEmpty_ReturnsNotQualifiedMsg() {
    BulkAddressQualificationResponse qualification = new BulkAddressQualificationResponse();
    qualification.setFiveGHomeQualified(false);
    qualification.setReturnMessage("");
    
    String result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "buildStatusMessage",
            qualification,
            true
    );
    
    assertEquals(SmbConstants.NOT_QUALIFIED_STATUS_MSG, result);
}

// =====================================================================
// INTEGRATION TEST: Full flow with various scenarios
// =====================================================================

@Test
void testCheck5GCoverageForAddresses_MixedResults_SomeValidSomeInvalid() {
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    List<CustomerAddress> addresses = new ArrayList<>();
    
    // Valid address
    CustomerAddress validAddr = createCustomerAddress("123 Main St", "10001");
    addresses.add(validAddr);
    
    // Address with missing fields (will be skipped for Nautilus)
    CustomerAddress invalidAddr = new CustomerAddress();
    invalidAddr.setAddressLine1("");
    invalidAddr.setZipCode("");
    addresses.add(invalidAddr);
    
    request.setCustomerAddress(addresses);
    
    // Mock split address - only valid address gets processed
    SplitAddressResponse validSplitResponse = createSplitAddressResponse();
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(validSplitResponse));
    
    // Mock Nautilus
    NautilusQualificationResponse nautilusResponse = createNautilusQualificationResponse();
    when(fiveGCoverageCheckHelper.buildNautilusQualificationRequest(any(List.class)))
            .thenReturn(new NautilusQualificationRequest());
    when(spectrumAdapterService.checkAddressQualification(any(NautilusQualificationRequest.class)))
            .thenReturn(Mono.just(nautilusResponse));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                return response.getData() != null &&
                       response.getData().getStatusCode() == SmbConstants.SUCCESS_STATUS;
            })
            .verifyComplete();
}

// =====================================================================
// HELPER METHODS (add if not already present)
// =====================================================================

private NautilusQualificationResponse createNautilusQualificationResponse() {
    NautilusQualificationResponse response = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    
    List<BulkAddressQualificationResponse> qualResponses = new ArrayList<>();
    BulkAddressQualificationResponse qualResponse = new BulkAddressQualificationResponse();
    qualResponse.setRecordIdentifier("1");
    qualResponse.setFiveGHomeQualified(true);
    qualResponse.setReturnCode("0");
    qualResponse.setReturnMessage("Eligible");
    qualResponse.setCBandQualified(true);
    qualResponse.setLTEQualified(true);
    
    AddressInfo addressInfo = new AddressInfo();
    addressInfo.setAddressId("300512786823");
    addressInfo.setLocationId("300512786823");
    qualResponse.setAddressInfo(addressInfo);
    
    AvailableCapacityInfo capacityInfo = new AvailableCapacityInfo();
    capacityInfo.setCbandCapacity("15.0");
    capacityInfo.setLteCapacity("0.0");
    qualResponse.setAvailableCapacityInfo(capacityInfo);
    
    Eligibilities eligibilities = new Eligibilities();
    List<BundleInfo> bundles = new ArrayList<>();
    BundleInfo bundle = new BundleInfo();
    bundle.setBundleName("Gen4-5G-Home-Internet");
    bundles.add(bundle);
    eligibilities.setFiveGHomeBundle(bundles);
    qualResponse.setEligibilities(eligibilities);
    
    qualResponses.add(qualResponse);
    data.setBulkAddressQualificationResponse(qualResponses);
    response.setData(data);
    
    return response;
}

// =====================================================================
// IMPORTS NEEDED:
// =====================================================================
/*
import java.util.Collections;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
*/
