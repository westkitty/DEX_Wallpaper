package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class XmlFeedsTest {
    @Test fun rssAndAtomParseActualFields() { assertEquals("Hello & world",XmlFeeds.rss("<rss><channel><item><title>Hello &amp; world</title><link>https://example.com/item</link><pubDate>Today</pubDate></item></channel></rss>").single().title);assertEquals("https://example.com/entry",XmlFeeds.rss("<feed xmlns='http://www.w3.org/2005/Atom'><entry><title>News</title><link href='https://example.com/entry'/></entry></feed>").single().link) }
    @Test fun webDavResolvesTypesAndFolders() { val rows=XmlFeeds.webDav("<d:multistatus xmlns:d='DAV:'><d:response><d:href>/art/a.jpg</d:href><d:propstat><d:prop><d:getcontenttype>image/jpeg</d:getcontenttype></d:prop></d:propstat></d:response><d:response><d:href>/art/</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop></d:propstat></d:response></d:multistatus>");assertTrue(rows.first().image);assertTrue(rows.last().directory) }
    @Test fun externalEntitiesAndDeepDocumentsRejected() { assertTrue(runCatching { XmlFeeds.rss("<!DOCTYPE rss [<!ENTITY xxe SYSTEM 'file:///private'>]><rss>&xxe;</rss>") }.isFailure);assertTrue(runCatching { XmlFeeds.rss("<a>".repeat(40)+"x"+"</a>".repeat(40)) }.isFailure) }
    @Test fun largeFieldsAndOversizedFeedsRejected() { assertTrue(runCatching { XmlFeeds.rss("<rss><title>"+"x".repeat(5000)+"</title></rss>") }.isFailure);assertTrue(runCatching { XmlFeeds.rss(" ".repeat(1024*1024+1)) }.isFailure) }
}
