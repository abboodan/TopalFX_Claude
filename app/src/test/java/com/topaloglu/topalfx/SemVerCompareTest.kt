package com.topaloglu.topalfx

import com.topaloglu.topalfx.updater.SemVer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerCompareTest {

    @Test
    fun `patch bump with v prefix is newer`() {
        assertTrue(SemVer.isNewer("v1.0.1", "1.0.0"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(SemVer.isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun `major bump beats higher minor and patch`() {
        assertTrue(SemVer.isNewer("v2.0.0", "1.9.9"))
    }

    @Test
    fun `older remote is not newer`() {
        assertFalse(SemVer.isNewer("v1.0.0", "1.0.1"))
    }

    @Test
    fun `two-part versions are padded`() {
        assertTrue(SemVer.isNewer("v1.1", "1.0.9"))
    }

    @Test
    fun `malformed tags are never newer`() {
        assertFalse(SemVer.isNewer("beta", "1.0.0"))
        assertFalse(SemVer.isNewer("", "1.0.0"))
        assertFalse(SemVer.isNewer("v1.0.0", "garbage"))
    }
}
