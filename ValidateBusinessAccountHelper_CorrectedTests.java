// =====================================================================
// CORRECTED TEST METHODS - ADD TO ValidateBusinessAccountHelperTest CLASS
// =====================================================================

// =====================================================================
// STEP 1: ADD THIS MOCK IN YOUR setUp() METHOD (after existing mocks)
// =====================================================================

/*
Add this mock for CollectionUtilities.isEmptyOrNull in setUp() method:

mockedCollectionUtilities.when(() -> CollectionUtilities.isEmptyOrNull(any(List.class)))
    .thenAnswer(invocation -> {
        List<?> list = invocation.getArgument(0);
        return list == null || list.isEmpty();
    });
*/

// =====================================================================
// STEP 2: ADD THESE CORRECTED TEST METHODS
// =====================================================================

// ==================== CORRECTED mapEligibilities TESTS ====================
// Key fix: Initialize BOTH fiveGHomeBundle AND cbandBundle (one with test data, other as empty list)

@Test
void testMapEligibilities_FiveGHomeBundleWithNullBundle_FilteredOut() {
    Eligibilities eligibilities = new Eligibilities();
    
    // Set fiveGHomeBundle with test data
    List<BundleInfo> fiveGBundles = new ArrayList<>();
    BundleInfo validBundle = new BundleInfo();
    validBundle.setBundleName("ValidBundle");
    fiveGBundles.add(validBundle);
    fiveGBundles.add(null);  // null bundle - should be filtered
    eligibilities.setFiveGHomeBundle(fiveGBundles);
    
    // IMPORTANT: Set cbandBundle to empty list to avoid NPE
    eligibilities.setCbandBundle(new ArrayList<>());
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertNotNull(enrichedAddress.getBundleList());
    assertEquals(1, enrichedAddress.getBundleList().size());
    assertEquals("ValidBundle", enrichedAddress.getBundleList().get(0));
}

@Test
void testMapEligibilities_FiveGHomeBundleWithEmptyBundleName_FilteredOut() {
    Eligibilities eligibilities = new Eligibilities();
    
    List<BundleInfo> bundles = new ArrayList<>();
    
    BundleInfo bundleWithNullName = new BundleInfo();
    bundleWithNullName.setBundleName(null);
    bundles.add(bundleWithNullName);
    
    BundleInfo bundleWithEmptyName = new BundleInfo();
    bundleWithEmptyName.setBundleName("");
    bundles.add(bundleWithEmptyName);
    
    BundleInfo validBundle = new BundleInfo();
    validBundle.setBundleName("ValidBundleName");
    bundles.add(validBundle);
    
    eligibilities.setFiveGHomeBundle(bundles);
    
    // IMPORTANT: Set cbandBundle to empty list to avoid NPE
    eligibilities.setCbandBundle(new ArrayList<>());
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertNotNull(enrichedAddress.getBundleList());
    assertEquals(1, enrichedAddress.getBundleList().size());
    assertEquals("ValidBundleName", enrichedAddress.getBundleList().get(0));
}

@Test
void testMapEligibilities_CbandBundleWithNullAndEmptyBundles_FilteredOut() {
    Eligibilities eligibilities = new Eligibilities();
    
    // IMPORTANT: Set fiveGHomeBundle to empty list to avoid NPE
    eligibilities.setFiveGHomeBundle(new ArrayList<>());
    
    // Set cbandBundle with test data
    List<BundleInfo> cbandBundles = new ArrayList<>();
    cbandBundles.add(null);  // null bundle
    
    BundleInfo bundleWithNullName = new BundleInfo();
    bundleWithNullName.setBundleName(null);
    cbandBundles.add(bundleWithNullName);
    
    BundleInfo bundleWithEmptyName = new BundleInfo();
    bundleWithEmptyName.setBundleName("");
    cbandBundles.add(bundleWithEmptyName);
    
    BundleInfo validCbandBundle = new BundleInfo();
    validCbandBundle.setBundleName("ValidCbandBundle");
    cbandBundles.add(validCbandBundle);
    
    eligibilities.setCbandBundle(cbandBundles);
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertNotNull(enrichedAddress.getBundleList());
    assertEquals(1, enrichedAddress.getBundleList().size());
    assertEquals("ValidCbandBundle", enrichedAddress.getBundleList().get(0));
}

@Test
void testMapEligibilities_BothBundleTypesWithFiltering() {
    Eligibilities eligibilities = new Eligibilities();
    
    // fiveGHomeBundle with valid + invalid
    List<BundleInfo> fiveGBundles = new ArrayList<>();
    BundleInfo fiveGValid = new BundleInfo();
    fiveGValid.setBundleName("FiveGBundle");
    fiveGBundles.add(fiveGValid);
    fiveGBundles.add(null);  // filtered out
    eligibilities.setFiveGHomeBundle(fiveGBundles);
    
    // cbandBundle with valid + invalid
    List<BundleInfo> cbandBundles = new ArrayList<>();
    BundleInfo cbandValid = new BundleInfo();
    cbandValid.setBundleName("CbandBundle");
    cbandBundles.add(cbandValid);
    BundleInfo cbandEmpty = new BundleInfo();
    cbandEmpty.setBundleName("");  // filtered out
    cbandBundles.add(cbandEmpty);
    eligibilities.setCbandBundle(cbandBundles);
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertNotNull(enrichedAddress.getBundleList());
    assertEquals(2, enrichedAddress.getBundleList().size());
    assertTrue(enrichedAddress.getBundleList().contains("FiveGBundle"));
    assertTrue(enrichedAddress.getBundleList().contains("CbandBundle"));
}

@Test
void testMapEligibilities_AllBundlesFilteredOut_BundleListNotSet() {
    Eligibilities eligibilities = new Eligibilities();
    
    // fiveGHomeBundle with all invalid bundles
    List<BundleInfo> bundles = new ArrayList<>();
    BundleInfo bundleNullName = new BundleInfo();
    bundleNullName.setBundleName(null);
    bundles.add(bundleNullName);
    
    BundleInfo bundleEmptyName = new BundleInfo();
    bundleEmptyName.setBundleName("");
    bundles.add(bundleEmptyName);
    
    bundles.add(null);
    
    eligibilities.setFiveGHomeBundle(bundles);
    
    // IMPORTANT: Set cbandBundle to empty list
    eligibilities.setCbandBundle(new ArrayList<>());
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertTrue(enrichedAddress.getBundleList() == null || enrichedAddress.getBundleList().isEmpty());
}

// ==================== CORRECTED hasInvalidCustomerAddress TESTS ====================
// These tests need the CollectionUtilities.isEmptyOrNull mock added in setUp()

@Test
void testHasInvalidCustomerAddress_NullList_ReturnsTrue() {
    // This test requires the mock for CollectionUtilities.isEmptyOrNull to be set up
    // If the mock returns true for null input, this test will pass
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            (List<CustomerAddress>) null
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_EmptyList_ReturnsTrue() {
    // This test requires the mock for CollectionUtilities.isEmptyOrNull to return true for empty list
    List<CustomerAddress> emptyList = new ArrayList<>();
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            emptyList
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_ContainsNullAddress_ReturnsTrue() {
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    addresses.add(null);  // null address
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            addresses
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_NullAddressLine1_ReturnsTrue() {
    List<CustomerAddress> addresses = new ArrayList<>();
    CustomerAddress addr = new CustomerAddress();
    addr.setAddressLine1(null);
    addr.setZipCode("10001");
    addresses.add(addr);
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            addresses
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_NullZipCode_ReturnsTrue() {
    List<CustomerAddress> addresses = new ArrayList<>();
    CustomerAddress addr = new CustomerAddress();
    addr.setAddressLine1("123 Main St");
    addr.setZipCode(null);
    addresses.add(addr);
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            addresses
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_AllValid_ReturnsFalse() {
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    addresses.add(createCustomerAddress("456 Oak Ave", "20002"));
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            addresses
    );
    assertFalse(result);
}

// =====================================================================
// SUMMARY OF CHANGES NEEDED:
// =====================================================================
/*
1. ADD this mock in setUp() method:

   mockedCollectionUtilities.when(() -> CollectionUtilities.isEmptyOrNull(any(List.class)))
       .thenAnswer(invocation -> {
           List<?> list = invocation.getArgument(0);
           return list == null || list.isEmpty();
       });

2. For mapEligibilities tests: 
   - Always set BOTH fiveGHomeBundle AND cbandBundle
   - The one you're NOT testing should be set to new ArrayList<>() (empty list)

3. For hasInvalidCustomerAddress tests:
   - Ensure the isEmptyOrNull mock handles null properly
*/
