/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.SQLXML;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.sqsh.Formatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is used to format XML content contained in database columns.
 * Note that right now it requires reading in of the entire XML document
 * into memory as the current jsqsh architecture doesn't provide for the
 * streaming of objects as they get displayed.
 */
public class XMLFormatter
    implements Formatter {
    
   public String format (Object value) {
        
        SQLXML xml = (SQLXML) value;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            
            Reader in = xml.getCharacterStream();
            prettyPrint(createDOM(in), out);
            
            in.close();
            out.flush();
            
            return out.toString();
        }
        catch (Exception e) {
            
            return "XML parse error (" + e.getMessage() + ")";
        }
        
    }

    public int getMaxWidth () {

        return Integer.MAX_VALUE;
    }
    
    private Element createDOM(Reader in) 
        throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	dbf.setValidating(false);
    	DocumentBuilder db = dbf.newDocumentBuilder();
    	
    	InputSource sourceXML = new InputSource(in);
    	Document xmlDoc = db.parse(sourceXML);
    	Element e = xmlDoc.getDocumentElement();
    	e.normalize();
    	
    	return e;
    }
    
    private final void prettyPrint(Node xml, OutputStream out)
        throws TransformerConfigurationException,
               TransformerFactoryConfigurationError,
               TransformerException {
        
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    	tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    	tf.setOutputProperty(OutputKeys.INDENT, "yes");
    	tf.transform(new DOMSource(xml), new StreamResult(out));
    }
}
