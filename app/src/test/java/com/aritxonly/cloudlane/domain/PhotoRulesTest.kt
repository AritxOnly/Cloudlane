package com.aritxonly.cloudlane.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
class PhotoRulesTest {
 private val today=LocalDate.of(2026,11,3)
 @Test fun protectsDocumentsBeforeCodes() { assertNull(PhotoRules.reason("Screenshot.png","",180,"发票 验证码 123",today)) }
 @Test fun oldCameraPhotoIsNotScreenshot() { assertNull(PhotoRules.reason("IMG.jpg","DCIM/Camera",500,"验证码",today)) }
 @Test fun doesNotInferExpiryFromArbitraryDate() { assertNull(PhotoRules.reason("Screenshot.png","",2,"订单日期 2020-01-01",today)) }
 @Test fun recognizesExplicitExpiredDate() { assertNotNull(PhotoRules.reason("Screenshot.png","",2,"有效期至：2026-01-01",today)) }
 @Test fun invalidDateCannotCrash() { assertNull(PhotoRules.reason("Screenshot.png","",2,"有效期至 2026-99-99",today)) }
 @Test fun nightBoundary() { assertTrue(PhotoRules.isNight(LocalTime.of(0,0)));assertTrue(PhotoRules.isNight(LocalTime.of(6,59)));assertFalse(PhotoRules.isNight(LocalTime.of(7,0))) }
 @Test fun leavesOneDuplicateAndIgnoresEmptyHash() { assertEquals(setOf("b"),PhotoRules.duplicateExtras(mapOf("b" to "same","a" to "same","c" to "","d" to ""))) }
 @Test fun cachedEvidenceExpiresWithoutRepeatingOcr() {
  val evidence = PhotoRules.evidence("Screenshot.png", "", "有效期至：2026-11-03")
  assertNull(PhotoRules.reason(evidence, 2, today))
  assertNotNull(PhotoRules.reason(evidence, 10, today.plusDays(8)))
 }
 @Test fun cachedEvidenceAgesIntoReview() {
  val evidence = PhotoRules.evidence("Screenshot.png", "", "普通截图")
  assertNull(PhotoRules.reason(evidence, 89, today))
  assertNotNull(PhotoRules.reason(evidence, 90, today.plusDays(1)))
 }
}
