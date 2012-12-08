/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
