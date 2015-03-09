package br.com.sankhya.dashviewer;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.util.Log;

public class XMLParser {
	public Document getDomElement(String xml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			return db.parse(is);
		} catch (Exception e) {
			Log.e("Error: ", e.getMessage());
		}

		return null;
	}

	public String getValue(Element item, String name) {
		NodeList n = item.getElementsByTagName(name);
		return getElementValue(n.item(0));
	}

	public final String getElementValue(Node elem) {
		Node child;

		if (elem != null) {
			if (elem.hasChildNodes()) {
				for (child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
					if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
						return child.getNodeValue();
					}
				}
			}
		}

		return null;
	}
}
