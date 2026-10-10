package com.westkitty.dexwallpaper.core

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

data class FeedItem(val title: String,val link: String,val date: String)
data class DavItem(val href: String,val image: Boolean,val directory: Boolean)
object XmlFeeds {
    fun rss(xml: String): List<FeedItem> {
        val items=mutableListOf<FeedItem>(); var item=false; var title=""; var link=""; var date=""
        parse(xml,object: BoundedHandler() {
            override fun open(name: String,attributes: Attributes) { if(name=="item" || name=="entry") { item=true;title="";link="";date="" }; if(item && name=="link") link=attributes.getValue("href")?.take(2048) ?: link }
            override fun close(name: String,text: String) {
                if(item) when(name) { "title" -> title=text; "link" -> if(link.isEmpty()) link=text; "pubDate","published","updated" -> date=text }
                if(name=="item" || name=="entry") { require(items.size<500) { "Too many feed items" };items+=FeedItem(title.take(1000),link.take(2048),date.take(100));item=false }
            }
        });return items
    }
    fun webDav(xml: String): List<DavItem> {
        val items=mutableListOf<DavItem>();var response=false;var href="";var type="";var directory=false
        parse(xml,object: BoundedHandler() {
            override fun open(name: String,attributes: Attributes) { if(name=="response") { response=true;href="";type="";directory=false };if(response && name=="collection") directory=true }
            override fun close(name: String,text: String) {
                if(response) when(name) { "href" -> href=text; "getcontenttype" -> type=text }
                if(name=="response") { require(items.size<5000) { "Too many DAV items" }; if(href.isNotBlank()) items+=DavItem(href,type.startsWith("image/") || href.substringBefore('?').substringAfterLast('.').lowercase() in setOf("jpg","jpeg","png","webp","heic","heif","bmp"),directory);response=false }
            }
        });return items
    }
    private fun parse(xml: String,handler: BoundedHandler) {
        require(xml.toByteArray().size<=1024*1024 && !xml.contains("<!DOCTYPE",true) && !xml.contains("<!ENTITY",true)) { "Oversized XML or entity declaration forbidden" }
        val reader=SAXParserFactory.newInstance().apply { isNamespaceAware=true }.newSAXParser().xmlReader
        reader.contentHandler=handler;reader.entityResolver=handler
        reader.parse(InputSource(StringReader(xml)))
    }
    private abstract class BoundedHandler: DefaultHandler() {
        private var depth=0;private var nodes=0;private val text=StringBuilder()
        abstract fun open(name: String,attributes: Attributes)
        abstract fun close(name: String,text: String)
        override fun startElement(uri: String?,local: String?,qName: String?,attributes: Attributes) {
            require(++depth<=32 && ++nodes<=20000) { "XML structure budget exceeded" };text.setLength(0);open(local?.takeIf { it.isNotEmpty() } ?: qName.orEmpty().substringAfter(':'),attributes)
        }
        override fun characters(chars: CharArray,start: Int,length: Int) { require(text.length+length<=4096) { "XML field exceeds 4KB" };text.append(chars,start,length) }
        override fun endElement(uri: String?,local: String?,qName: String?) { close(local?.takeIf { it.isNotEmpty() } ?: qName.orEmpty().substringAfter(':'),text.toString().trim());depth--;text.setLength(0) }
        override fun resolveEntity(publicId: String?,systemId: String?): InputSource = throw SAXException("External entities are forbidden")
    }
}
