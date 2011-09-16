package controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.petalslink.dsb.cxf.CXFHelper;
import org.petalslink.dsb.servicepoller.api.ServicePollerException;
import org.petalslink.dsb.servicepoller.api.WSNPollerService;
import org.petalslink.dsb.servicepoller.api.WSNPollerServiceInformation;
import org.petalslink.dsb.ws.api.ServiceEndpoint;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import play.libs.XML;
import play.mvc.Controller;
import eu.playproject.servicebus.cronclient.WSNCronClient;
import eu.playproject.servicebus.cronclient.WSNCronClientImpl;

public class Application extends Controller {

	public static void index() {
		cron();
	}
	
	public static void cron() {
		//WSNPollerService service = CXFHelper.getClient(getURL(), WSNPollerService.class);
		//List<WSNPollerServiceInformation> informations = service.getInformation();
		List<WSNPollerServiceInformation> informations = new ArrayList<WSNPollerServiceInformation>();
		WSNPollerServiceInformation info = new WSNPollerServiceInformation();
		info.setCronExpression("lll");
		info.setId("IDA");
		info.setInputMessage("<foo></foo>");
		informations.add(info);
		render(informations);
	}

	public static void postCreateCron(String inputMessage, String topicURI,
			String topicName, String topicPrefix, String operationToPoll,
			String delay) {
		Document document = XML.getDocument(inputMessage);
		if (document == null) {
			flash.error(
					"Can not create a XML document from the input message '%s'",
					inputMessage);
		}

		WSNCronClient client = new WSNCronClientImpl(
				"http://localhost:7600/petals/ws/");

		// poll every second and send response to the notification engine...
		String id = null;
		QName topic = new QName(topicURI, topicName, topicPrefix);
		QName notify = new QName("http://docs.oasis-open.org/wsn/b-2", "Notify");
		ServiceEndpoint to = new ServiceEndpoint();
		to.setEndpoint("NotificationConsumerPort");
		to.setItf(new QName("http://docs.oasis-open.org/wsn/b-2",
				"NotificationConsumer"));
		to.setService(new QName("http://docs.oasis-open.org/wsn/b-2",
				"NotificationConsumerService"));

		String cronExpression = "* * * * * ?";

		try {
			id = client.pollService(
					"http://localhost:7600/petals/ws/HelloService?wsdl",
					QName.valueOf(operationToPoll), document, cronExpression,
					to, notify, topic);

			System.out.println("Get an ID " + id);
		} catch (ServicePollerException e) {
			flash.error("Error while invoking polling operation : %s",
					e.getMessage());
		}
		render();
	}
	
	private static String getURL() {
		return "http://localhost:7600/petals/ws";
	}

}