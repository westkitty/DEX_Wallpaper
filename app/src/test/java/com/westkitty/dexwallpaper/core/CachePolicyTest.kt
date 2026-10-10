package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class CachePolicyTest {
    @Test fun oldestDisposableEvictedFirst() { assertEquals(listOf("a"),CachePolicy.evict(listOf(CacheEntry("b",10,2),CacheEntry("a",10,1)),10)) }
    @Test fun originalsNeverEvicted() { assertEquals(emptyList<String>(),CachePolicy.evict(listOf(CacheEntry("original",100,0,true)),0)) }
    @Test fun cacheWithinBudgetIsUntouched() { assertTrue(CachePolicy.evict(listOf(CacheEntry("a",10,1)),10).isEmpty()) }
}
