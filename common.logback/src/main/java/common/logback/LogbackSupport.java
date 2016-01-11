package common.logback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.impl.StaticLoggerBinder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * @author Lisong
 */
public class LogbackSupport
{
	public final String lineSep = "";

	private static final XPath path = XPathFactory.newInstance().newXPath();

	private String[] supportNode = new String[] { "appender", "logger" };

	private final String ROOT_NODE = "//root";

	private final String PROPER_NODE = "//property";

	private boolean printLogConfig = true;

	private boolean usingLocalConfig = false;

	private final String packageName = "/common/logback/config";

	private final String LOGBACK_COMMON = packageName + "/logback-common.xml";

	private String logbackFilePath/* = packageName + "/logback.xml"*/;

	private final String defaultLogbackFilePath = packageName + "/logback-test.xml";

	public String getLogbackFilePath()
	{
		return logbackFilePath;
	}

	public String[] getSupportNode()
	{
		return supportNode;
	}

	public void init()
	{
		if (!usingLocalConfig) {
			System.out.println("Local env. Using default logback config: " + defaultLogbackFilePath);
			try {
				InputStream iss = loadClasspathFileStream(defaultLogbackFilePath);
				LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(loggerContext);
				loggerContext.reset();
				configurator.doConfigure(iss);
				System.out.println("Logback configured!");
			}
			catch (FileNotFoundException e) {
				System.err.println("No '" + defaultLogbackFilePath + "' found.");
			}
			catch (JoranException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			return;
		}
		InputStream commonStream = null;
		InputStream localStream = null;
		try {
			commonStream = loadClasspathFileStream(LOGBACK_COMMON);
			Document commonDoc = parseDocument(commonStream);
			localStream = loadClasspathFileStream(logbackFilePath);
			Document localDoc = parseDocument(localStream);

			initLogback(commonDoc, localDoc);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		finally {
			try {
				if (commonStream != null) {
					commonStream.close();
				}
			}
			catch (Exception e) {
				//Ignore
			}
			finally {
				if (localStream != null) {
					try {
						localStream.close();
					}
					catch (IOException e) {
					}
				}
			}
		}
	}

	public boolean isPrintLogConfig()
	{
		return printLogConfig;
	}

	public boolean isUsingLocalConfig()
	{
		return usingLocalConfig;
	}

	public void setLogbackFilePath(String logbackFilePath)
	{
		this.logbackFilePath = logbackFilePath;
	}

	public void setPrintLogConfig(boolean printLogConfig)
	{
		this.printLogConfig = printLogConfig;
	}

	public void setSupportNode(String[] supportNode)
	{
		this.supportNode = supportNode;
	}

	public void setUsingLocalConfig(boolean usingLocalConfig)
	{
		this.usingLocalConfig = usingLocalConfig;
	}

	private NodeList getList(XPath path, Object node, String expression) throws XPathExpressionException
	{
		return (NodeList) path.evaluate(expression, node, XPathConstants.NODESET);
	}

	private Document parseDocument(InputStream is) throws SAXException, IOException, ParserConfigurationException
	{
		DocumentBuilder dbd = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = dbd.parse(is);
		return doc;
	}

	private void printNode(Node n, StringBuffer sb, String lineSeprator)
	{
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			printNodeHeader(n, sb, lineSeprator);
			NodeList chds = n.getChildNodes();
			int chdLen = chds == null ? 0 : chds.getLength();
			for (int cindex = 0; cindex < chdLen; cindex++) {
				printNode(chds.item(cindex), sb, lineSeprator);
			}
			printNodeEnd(n, sb, lineSeprator);
		}
		else if (n.getNodeType() == Node.TEXT_NODE) {
			String text = n.getTextContent();
			sb.append(text == null ? "" : text);
		}
	}

	private void printNodeEnd(Node n, StringBuffer sb, String lineSeprator)
	{
		sb.append("</").append(n.getNodeName()).append(">").append(lineSeprator);
	}

	private void printNodeHeader(Node n, StringBuffer sb, String lineSeprator)
	{
		sb.append("<").append(n.getNodeName()).append(" ");
		NamedNodeMap attrmap = n.getAttributes();
		int len = attrmap == null ? 0 : attrmap.getLength();
		for (int k = 0; k < len; k++) {
			String name = attrmap.item(k).getNodeName();
			String value = attrmap.item(k).getNodeValue();
			sb.append(name).append("=\"").append(value).append("\" ");
		}
		sb.append(">").append(lineSeprator);
	}

	private void printNodeList(NodeList list, StringBuffer sb, String lineSeprator)
	{
		int len = list == null ? 0 : list.getLength();
		for (int k = 0; k < len; k++) {
			printNode(list.item(k), sb, lineSeprator);
		}
	}

	protected void initLogback(Document commonDoc, Document localDoc) throws Exception
	{
		File temp = mergeLogConfig(commonDoc, localDoc);
		LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		loggerContext.reset();
		configurator.doConfigure(temp);
		System.out.println("Logback configured!");
	}

	protected InputStream loadClasspathFileStream(String path) throws FileNotFoundException
	{
		System.out.println("Loading '" + path + "' with Class");
		InputStream is = getClass().getResourceAsStream(path);
		if (is == null) {
			System.out.println("Loading '" + path + "' with ClassLoader: " + LogbackSupport.class.getClassLoader());
			is = LogbackSupport.class.getClassLoader().getResourceAsStream(path);
		}

		if (is == null) {
			System.out.println("Loading '" + path + "' with ClassLoader: " + Thread.currentThread().getContextClassLoader());
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		}

		if (is == null) {
			throw new FileNotFoundException(path + " cannot be opened because it does not exist");
		}
		return is;
	}

	protected File mergeLogConfig(Document commonDoc, Document localDoc) throws Exception, ParserConfigurationException,
			XPathExpressionException
	{
		System.out.println("Reading logback config files....");
		NodeList configuration = getList(path, localDoc, "//configuration");
		if (configuration == null || configuration.getLength() == 0) {
			throw new IllegalStateException("No Logback configuration found!");
		}
		StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printNodeHeader(configuration.item(0), sb, lineSep);

		printNodeList(getList(path, localDoc, PROPER_NODE), sb, lineSep);
		printNodeList(getList(path, commonDoc, PROPER_NODE), sb, lineSep);
		for (String nodeName : supportNode) {
			String searchNodeName = "//" + nodeName;
			printNodeList(getList(path, localDoc, searchNodeName), sb, lineSep);
			printNodeList(getList(path, commonDoc, searchNodeName), sb, lineSep);
		}
		NodeList rootNode = getList(path, localDoc, ROOT_NODE);
		rootNode = rootNode == null || rootNode.getLength() == 0 ? getList(path, commonDoc, ROOT_NODE) : rootNode;

		if (rootNode != null && rootNode.getLength() > 0) {
			printNode(rootNode.item(0), sb, lineSep);
		}

		printNodeEnd(configuration.item(0), sb, lineSep);

		BufferedWriter out = null;
		File temp = null;
		try {
			temp = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".xml");
			temp.deleteOnExit();
			out = new BufferedWriter(new FileWriter(temp));
			out.write(sb.toString());
		}
		finally {
			out.close();
		}
		if (printLogConfig) {
			System.out.println("===========Config Logback with the file =============");
			System.out.println(sb.toString());
			System.out.println("==================================================");
		}
		sb.setLength(0);
		return temp;
	}
}
