package protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import client.*;

public class DummyRoutingProtocol implements IRoutingProtocol {
	private static final int CLIENTS = 6;
	private LinkLayer linkLayer;
	private ConcurrentHashMap<Integer, BasicRoute> forwardingTable = new ConcurrentHashMap<Integer, BasicRoute>();
	private DataTable dataTable;

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;

		System.out.println(linkLayer.getOwnAddress());

		this.dataTable = new DataTable(3);
		this.dataTable.addRow(new Integer[]{0, 0, 0});

		for (int i = 1; i < CLIENTS + 1; i++) {
			int distance = this.linkLayer.getLinkCost(i);
			this.dataTable.addRow(new Integer[]{i, distance, this.linkLayer.getOwnAddress()});
		}
		this.dataTable.set(this.linkLayer.getOwnAddress(), 1, 0);

		updateForwardingTable();
		broadcastTable();
	}

	@Override
	public void run() {
		try {
			while (true) {
				// Try to receive a packet
				Packet packet = this.linkLayer.receive();
				if (packet != null) {
					DataTable data = packet.getData();
					boolean isUpdated = false;
					for (int i = 1; i < CLIENTS + 1; i++) {
						Integer[] row = data.getRow(i);
						if (row[1] != -1 && (row[1] < dataTable.get(i, 1) || dataTable.get(i, 1) == -1 || dataTable.get(i, 2) == packet.getSourceAddress())) {
							int dst = row[1] + dataTable.get(packet.getSourceAddress(), 1);
							if (dataTable.get(i, 1) != dst || dataTable.get(i, 2) != packet.getSourceAddress()) {
								System.out.println("source:" + packet.getSourceAddress());
								System.out.println("remote " + i + ":" + Arrays.toString(row));
								System.out.println("local " + i + ":" + Arrays.toString(dataTable.getRow(i)));
								isUpdated = true;
								dataTable.set(i, 1, dst);
								dataTable.set(i, 2, packet.getSourceAddress());
							}
						}
					}
					if (isUpdated) {
						updateForwardingTable();
						broadcastTable();
					}
				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			// We were interrupted, stop execution of the protocol
		}
	}
	
	public void broadcastTable() {
		Packet packet = new Packet(this.linkLayer.getOwnAddress(), 0, this.dataTable);
		this.linkLayer.transmit(packet);
	}
	
	public void updateForwardingTable() {
		for (int i = 1; i < CLIENTS + 1; i++) {
			BasicRoute route = new BasicRoute();
			route.nextHop = dataTable.get(i, 2);
			this.forwardingTable.put(i, route);
		}
	}

	@Override
	public ConcurrentHashMap<Integer, BasicRoute> getForwardingTable() {
		return this.forwardingTable;
	}
}
