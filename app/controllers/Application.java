package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import models.Node;

import org.petalslink.dsb.cxf.CXFHelper;
import org.petalslink.dsb.servicepoller.api.ServicePollerException;
import org.petalslink.dsb.servicepoller.api.WSNPollerServiceInformation;
import org.petalslink.dsb.servicepoller.client.WSNPollerClient;
import org.petalslink.dsb.ws.api.ServiceEndpoint;
import org.w3c.dom.Document;

import play.libs.XML;
import play.mvc.Before;
import play.mvc.Controller;
import eu.playproject.servicebus.cronclient.WSNCronClient;
import eu.playproject.servicebus.cronclient.WSNCronClientImpl;

public class Application extends Controller {
	
	@Before
	/**
	 * Put some data on all requests...
	 */
	public static void init() {
		Node n = Node.getCurrentNode();
		if (n != null) {
			renderArgs.put("currentNode", n);
		}
	}
	
	public static void connect() {
		List<Node> nodes = Node.all().fetch();
		render(nodes);
	}
	
	public static void nodeConnect(Long id) {
		Node n = Node.findById(id);
		if (n != null) {
			session.put("node", n.id);
			flash.success("Connected to node %s", n.name);
		} else {
			flash.error("No such node");
		}
		connect();
	}
	
	public static void nodeDisconnect(Long id) {
		session.remove("node");
		flash.success("Disconnected...");
		connect();
	}

	public static void index() {
		// List<WSNPollerServiceInformation> informations = new
		// ArrayList<WSNPollerServiceInformation>();
		// WSNPollerServiceInformation info = new WSNPollerServiceInformation();
		// info.setCronExpression("lll");
		// info.setId("IDA");
		// info.setInputMessage("<foo></foo>");
		// informations.add(info);
		WSNPollerClient client = new WSNPollerClient(getURL());
		List<WSNPollerServiceInformation> informations = null;
		try {
			informations = client.getInformation();
		} catch (Exception e) {
			flash.error("Can not connect to service");
		}
		render(informations);
	}

	public static void cron() {
		render();
	}

	public static void postCreateCron(String wsdlURL, String message,
			String topicurl, String topicname, String topicprefix,
			String operation, String period) {

		// /TODO : Add validation
		Document document = XML.getDocument(message);
		if (document == null) {
			flash.error(
					"Can not create a XML document from the input message '%s'",
					message);
			index();
		}

		WSNCronClient client = new WSNCronClientImpl(getURL());

		String id = null;
		QName topic = new QName(topicurl, topicname, topicprefix);
		QName notify = new QName("http://docs.oasis-open.org/wsn/b-2", "Notify");
		ServiceEndpoint to = new ServiceEndpoint();
		to.setEndpoint("NotificationConsumerPort");
		to.setItf(new QName("http://docs.oasis-open.org/wsn/b-2",
				"NotificationConsumer"));
		to.setService(new QName("http://docs.oasis-open.org/wsn/b-2",
				"NotificationConsumerService"));

		String cronExpression = "0/" + period + " * * * * ?";

		try {
			id = client.pollService(wsdlURL, QName.valueOf(operation),
					document, cronExpression, to, notify, topic);

			flash.success("Cron job created (ID is %s)", id);

		} catch (ServicePollerException e) {
			flash.error("Error while invoking polling operation : %s",
					e.getMessage());
		} catch (Exception ee) {
			flash.error("Unexpected exception %s", ee.getMessage());
		}
		index();
	}

	public static void startCron(String id) {
		index();
	}

	public static void stopCron(String id) {
		WSNPollerClient client = CXFHelper.getClient(getURL(),
				WSNPollerClient.class);
		try {
			client.stop(id);
		} catch (Exception e) {
			flash.error("Error while stopping poll");
		}
		index();
	}

	public static void pauseCron(String id) {
		WSNPollerClient client = CXFHelper.getClient(getURL(),
				WSNPollerClient.class);
		try {
			client.pause(id);
		} catch (Exception e) {
			flash.error("Error while pausing poll");
		}
		index();
	}

	public static void resumeCron(String id) {
		WSNPollerClient client = CXFHelper.getClient(getURL(),
				WSNPollerClient.class);
		try {
			client.resume(id);
		} catch (Exception e) {
			flash.error("Error while pausing poll");
		}
		index();
	}

	public static void cronDetails(String id) {
		if (id == null) {
			flash.error("Bad request, id is null");
			index();
		}
		WSNPollerClient client = new WSNPollerClient(getURL());
		try {
			List<WSNPollerServiceInformation> informations = client
					.getInformation();
			boolean found = false;
			WSNPollerServiceInformation information = null;
			Iterator<WSNPollerServiceInformation> iter = informations
					.iterator();
			while (!found && iter.hasNext()) {
				information = iter.next();
				if (id.equals(information.getId())) {
					found = true;
				}
			}

			if (found) {
				render(information);
			}
		} catch (Exception e) {
			flash.error("Error while pausing poll");
		}
		flash.error("Bad request, id '%s' not found", id);
		index();
	}

	private static String getURL() {
		Node n = Node.getCurrentNode();
		if (n == null) {
			flash.success("Please connect");
			connect();
		}
		return n.baseURL;
	}

}