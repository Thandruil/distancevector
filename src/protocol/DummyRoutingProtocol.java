package protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import client.*;
import com.sun.org.apache.xpath.internal.SourceTree;

public class DummyRoutingProtocol implements IRoutingProtocol {
	private static final int CLIENTS = 6;
	private LinkLayer linkLayer;
	private ConcurrentHashMap<Integer, BasicRoute> forwardingTable = new ConcurrentHashMap<Integer, BasicRoute>();
	private DataTable dataTable;
    private DataTable[] neighboursTable;
	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;
        neighboursTable = new DataTable[CLIENTS+1];
		System.out.println(linkLayer.getOwnAddress());

		this.dataTable = new DataTable(3);
		this.dataTable.addRow(new Integer[]{0, 0, 0});

		for (int i = 1; i < CLIENTS + 1; i++) {
			int distance = this.linkLayer.getLinkCost(i);
			this.dataTable.addRow(new Integer[]{i, distance, i});
		}
		this.dataTable.set(this.linkLayer.getOwnAddress(), 1, 0);

		for (int i = 1; i < CLIENTS + 1; i++) {
			System.out.println(Arrays.toString(dataTable.getRow(i)));
		}

		updateForwardingTable();
		broadcastTable();
	}

	@Override
	public void run() {
		try {
			while (true) {
				// Try to receive a packet
				Packet packet = this.linkLayer.receive();
				boolean isUpdated = false;
				if (packet != null) {
					DataTable data = packet.getData();
                    neighboursTable[packet.getSourceAddress()] = data;
					for (int i = 1; i < CLIENTS + 1; i++) {
						Integer[] row = data.getRow(i);
						int dst = row[1] + dataTable.get(packet.getSourceAddress(), 1);
						if (i != this.linkLayer.getOwnAddress() && row[1] != -1 && (dst < dataTable.get(i, 1) || dataTable.get(i, 1) == -1 || dataTable.get(i, 2) == packet.getSourceAddress())) {
							if (dataTable.get(i, 1) != dst || dataTable.get(i, 2) != packet.getSourceAddress()) {
                                System.out.println("source:" + packet.getSourceAddress());
                                System.out.println("remote " + i + ":" + Arrays.toString(row));
								System.out.println("local " + i + ":" + Arrays.toString(dataTable.getRow(i)));
								isUpdated = true;
								dataTable.set(i, 1, dst);
								dataTable.set(i, 2, dataTable.get(packet.getSourceAddress(), 2));
							}
						}
					}

				}
				for (int i = 1; i < CLIENTS + 1; i++) {
                    if (i == linkLayer.getOwnAddress()) {
                        continue;
                    }
					int dst = this.linkLayer.getLinkCost(i);
                    int oldDst = dataTable.get(i, 2) == i ? dataTable.get(i, 1) : -1;
                    if (dst != oldDst) {
                        System.out.println("Link to " + i + " changed from " + oldDst + " to " + dst);
                    }
                    if (oldDst == -1) {
                        //isUpdated = true;
                    } else {
                        if (dst != oldDst) {
                            for (int j = 1; j < CLIENTS + 1; j++) {
                                if (dataTable.get(j, 2) == i) {
                                    if (dst == -1) {
                                        dataTable.set(j, 1, -1);
                                        isUpdated = true;
                                    } else {
                                        dataTable.set(j, 1, dataTable.get(j, 1) + dst - oldDst);
                                        isUpdated = true;
                                    }
                                    for (int k = 1; k < CLIENTS + 1; k++) {
                                        if (neighboursTable[k] != null &&
                                                neighboursTable[k].get(j, 1) + dataTable.get(dataTable.get(k, 2), 1) < dataTable.get(j, 1) &&
                                                neighboursTable[k].get(j, 1) != -1) {
                                            dataTable.set(j, 1, neighboursTable[k].get(j, 1) + dataTable.get(dataTable.get(k, 2), 1));
                                            dataTable.set(j, 2, k);
                                            isUpdated = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
				}
				if (isUpdated) {
					for (int i = 1; i < CLIENTS + 1; i++) {
						System.out.println(Arrays.toString(dataTable.getRow(i)));
					}
					updateForwardingTable();
					broadcastTable();
				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			// We were interrupted, stop execution of the protocol
		}
	}
	
	public void broadcastTable() {
		Packet packet = new Packet(this.linkLayer.getOwnAddress(), 0, this.dataTable);
        for (int i = 1; i < CLIENTS + 1; i++) {
            if (this.linkLayer.getLinkCost(i) != -1) {
                if (neighboursTable[i] == null) {
                    this.linkLayer.transmit(packet);
                    return;
                } else {
                    for (int j = 1; j < CLIENTS + 1; j++) {
                        int hisDst = neighboursTable[i].get(j, 1);
                        if (hisDst == -1 && dataTable.get(j, 1) != -1) {
                            this.linkLayer.transmit(packet);
                            return;
                        }
                        if (dataTable.get(j, 1) + this.linkLayer.getLinkCost(i) < hisDst) {
                            this.linkLayer.transmit(packet);
                            return;
                        }
                    }
                }
            }
        }
        System.out.println("SAVED A BROADCAST");

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
