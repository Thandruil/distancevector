package protocol;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import client.*;

public class DummyRoutingProtocol implements IRoutingProtocol {
	private static final int CLIENTS = 6;
	private LinkLayer linkLayer;
	private ConcurrentHashMap<Integer, BasicRoute> forwardingTable = new ConcurrentHashMap<Integer, BasicRoute>();
	private int[] links;

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;
		this.links = new int[7];

		// First, send a broadcast packet (to address 0), with no data
		if (this.linkLayer.getOwnAddress() == 1) {
			Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, new DataTable(0));
			System.out.println(this.linkLayer.getOwnAddress());
			this.linkLayer.transmit(discoveryBroadcastPacket);
		}
	}

	@Override
	public void run() {
		for (int i = 1; i < CLIENTS + 1; i++) {
			int distance = this.linkLayer.getLinkCost(i);
			links[i] = distance;
			System.out.printf("%d distance %d\n", i, distance);
		}
		
		try {
			while (true) {
				// Try to receive a packet
				Packet packet = this.linkLayer.receive();
				if (packet != null) {

				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			// We were interrupted, stop execution of the protocol
		}
	}

	@Override
	public ConcurrentHashMap<Integer, BasicRoute> getForwardingTable() {
		return this.forwardingTable;
	}
}
