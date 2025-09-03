/**
 * 
 */
/**
 * 
 */
module NganHangDeThiTiengNhat {
	requires java.sql;
	requires mysql.connector.java;
	requires java.desktop;
	requires com.formdev.flatlaf;
	requires org.apache.poi.ooxml;
	requires org.apache.pdfbox;
	requires org.apache.commons.logging;
	requires org.apache.fontbox;
	uses javax.sound.sampled.spi.AudioFileReader;
	uses javax.sound.sampled.spi.FormatConversionProvider;
    requires org.apache.poi.ooxml.schemas; // POI
    requires org.apache.xmlbeans;          // POI
    requires org.apache.commons.compress;  // POI
    requires org.apache.commons.codec;     // POI
    requires org.apache.commons.collections4; // POI
    requires org.apache.logging.log4j;
    requires mp3spi;
    requires org.slf4j;
    requires tritonus.share;
    requires jlayer;

	requires tess4j;
    requires ch.qos.logback.classic; // Tên module cho logback-classic
    requires ch.qos.logback.core;    // Tên module cho logback-core
    requires jul.to.slf4j;    
}