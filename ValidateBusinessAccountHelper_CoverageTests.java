// =====================================================================
// ADD THESE TEST METHODS TO YOUR EXISTING ValidateBusinessAccountHelperTest CLASS
// These cover the uncovered lines shown in Image 2:
//   - Line 16: return defaultValue (in parse methods)
//   - Lines 18-20: if (original == null) return new CustomerAddress()
//   - Lines 3,5,6,8: filter branches in mapEligibilities
//   - Line 12: hasInvalidCustomerAddress branches
// =====================================================================

// ==================== TESTS FOR: return defaultValue (parseIntegerSafely/parseDoubleSafely/parseDoubleToIntSafely) ====================

@Test
void testParseIntegerSafely_NullValue_ReturnsDefaultValue() {
    Integer result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseIntegerSafely",
            null,           // value is null
            "testField",    // fieldName
            42              // defaultValue
    );
    assertEquals(42, result);
}

@Test
void testParseIntegerSafely_EmptyValue_ReturnsDefaultValue() {
    Integer result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseIntegerSafely",
            "",             // value is empty
            "testField",
            99
    );
    assertEquals(99, result);
}

@Test
void testParseDoubleSafely_NullValue_ReturnsDefaultValue() {
    Double result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseDoubleSafely",
            null,           // value is null
            "testField",
            3.14
    );
    assertEquals(3.14, result, 0.001);
}

@Test
void testParseDoubleSafely_EmptyValue_ReturnsDefaultValue() {
    Double result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseDoubleSafely",
            "",             // value is empty
            "testField",
            2.71
    );
    assertEquals(2.71, result, 0.001);
}

@Test
void testParseDoubleToIntSafely_NullValue_ReturnsDefaultValue() {
    Integer result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseDoubleToIntSafely",
            null,           // value is null
            "testField",
            50
    );
    assertEquals(50, result);
}

@Test
void testParseDoubleToIntSafely_EmptyValue_ReturnsDefaultValue() {
    Integer result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "parseDoubleToIntSafely",
            "",             // value is empty
            "testField",
            75
    );
    assertEquals(75, result);
}

// ==================== TESTS FOR: createCustomerAddressCopy when original is null ====================

@Test
void testCreateCustomerAddressCopy_NullOriginal_ReturnsNewCustomerAddress() {
    CustomerAddress result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "createCustomerAddressCopy",
            (CustomerAddress) null
    );
    
    assertNotNull(result);
    assertNull(result.getAddressLine1());
    assertNull(result.getZipCode());
    assertFalse(result.isQualified());
}

// ==================== TESTS FOR: mapEligibilities filter branches ====================

@Test
void testMapEligibilities_FiveGHomeBundleWithNullBundle_FilteredOut() {
    Eligibilities eligibilities = new Eligibilities();
    List<BundleInfo> bundles = new ArrayList<>();
    
    BundleInfo validBundle = new BundleInfo();
    validBundle.setBundleName("ValidBundle");
    bundles.add(validBundle);
    bundles.add(null);  // null bundle - should be filtered
    
    eligibilities.setFiveGHomeBundle(bundles);
    
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
    
    // fiveGHomeBundle
    List<BundleInfo> fiveGBundles = new ArrayList<>();
    BundleInfo fiveGValid = new BundleInfo();
    fiveGValid.setBundleName("FiveGBundle");
    fiveGBundles.add(fiveGValid);
    fiveGBundles.add(null);  // filtered out
    eligibilities.setFiveGHomeBundle(fiveGBundles);
    
    // cbandBundle
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
    
    List<BundleInfo> bundles = new ArrayList<>();
    BundleInfo bundleNullName = new BundleInfo();
    bundleNullName.setBundleName(null);
    bundles.add(bundleNullName);
    
    BundleInfo bundleEmptyName = new BundleInfo();
    bundleEmptyName.setBundleName("");
    bundles.add(bundleEmptyName);
    
    bundles.add(null);
    
    eligibilities.setFiveGHomeBundle(bundles);
    
    CustomerAddress enrichedAddress = new CustomerAddress();
    
    ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "mapEligibilities",
            enrichedAddress,
            eligibilities
    );
    
    assertTrue(enrichedAddress.getBundleList() == null || enrichedAddress.getBundleList().isEmpty());
}

// ==================== TESTS FOR: hasInvalidCustomerAddress branches ====================

@Test
void testHasInvalidCustomerAddress_NullList_ReturnsTrue() {
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            (List<CustomerAddress>) null
    );
    assertTrue(result);
}

@Test
void testHasInvalidCustomerAddress_EmptyList_ReturnsTrue() {
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "hasInvalidCustomerAddress",
            new ArrayList<CustomerAddress>()
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
// IMPORT REQUIRED (add to your imports if not already present):
// import org.springframework.test.util.ReflectionTestUtils;
// =====================================================================
