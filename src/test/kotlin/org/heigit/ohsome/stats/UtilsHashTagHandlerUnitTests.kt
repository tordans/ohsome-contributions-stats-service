package org.heigit.ohsome.stats

import org.junit.jupiter.api.Assertions.*
import org.heigit.ohsome.stats.utils.HashtagHandler
import org.heigit.ohsome.stats.utils.getGroupbyInterval
import org.junit.jupiter.api.Test


class UtilsHashTagHandlerUnitTests {

    @Test
    fun `check if normal hashtag is wildcard`() {
        val hashtagHandler = HashtagHandler("normalHashtag")
        assertEquals("normalHashtag", hashtagHandler.hashtag)
        assertFalse(hashtagHandler.isWildCard)
    }

    @Test
    fun `check if wildcard hashtag is wildcard`() {
        val hashtagHandler = HashtagHandler("wildcardHashtag*")
        assertEquals("wildcardHashtag", hashtagHandler.hashtag)
        assertTrue(hashtagHandler.isWildCard)
    }
}
