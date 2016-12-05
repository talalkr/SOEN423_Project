package replica_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import packet.BookFlightOperation;
import packet.ExecuteOperationLogOperation;
import packet.Operation;
import packet.OperationLogOperation;
import packet.OperationLogReply;
import packet.OperationParameters;
import packet.OperationParametersHandler;
import packet.Packet;
import packet.ReplicaRebootReply;
import udp.UdpHelper;

public class ReplicaRebootHandler extends OperationParametersHandler {
	private ReplicaManager replicaManager;

	public ReplicaRebootHandler(InetAddress address, int port, OperationParameters operationParameters, ReplicaManager replicaManager) {
		super(address, port, operationParameters);
		this.replicaManager = replicaManager;
	}

	@Override
	public void execute() {
		DatagramSocket newSocket = null;
		try {
			newSocket = new DatagramSocket();
			
			// Reboot replica
			replicaManager.setRebooting(true);
			boolean result = replicaManager.rebootReplica();
			
			// TODO : Get sequencerPort from configuration
			int sequencerPort = 10000;
			OperationLogOperation operationLogOperation = new OperationLogOperation(replicaManager.getPort());
			Packet operationLogPacket = new Packet(Operation.OPERATION_LOG, operationLogOperation);
			byte[] operationLogMessage = UdpHelper.getByteArray(operationLogPacket);
			DatagramPacket requestPacket = new DatagramPacket(operationLogMessage, operationLogMessage.length, address, sequencerPort);
			newSocket.send(requestPacket);
			
			// Receive reply from Sequencer
			byte[] buffer = new byte[BUFFER_SIZE];
			DatagramPacket sequencerReply = new DatagramPacket(buffer, buffer.length);
			newSocket.receive(sequencerReply);
			Packet sequencerReplyPacket = (Packet) UdpHelper.getObjectFromByteArray(sequencerReply.getData());
			OperationLogReply operationLogReply = (OperationLogReply) sequencerReplyPacket.getOperationParameters();
			ArrayList<Packet> operationLog = operationLogReply.getOperationLog();
			
			// TESTING
			// Build the action for the packet
			BookFlightOperation bookFlightOperation = new BookFlightOperation.BuilderImpl("John").lastName("Doe")
					.address("Address").phoneNumber("PhoneNumber").destination("MTL|NDL").date("06/05/2016")
					.flightClass("FIRST").build();
			
			// Create a packet with the operation
			Packet testPacket = new Packet(Operation.BOOK_FLIGHT, bookFlightOperation);
			operationLog.add(testPacket);
			
			// Replica re-performs all operations in the log
			ExecuteOperationLogOperation executeOperationLogOperation = new ExecuteOperationLogOperation(operationLog);
			Packet executeOperationLogOperationPacket = new Packet(Operation.EXECUTE_OPERATION_LOG, executeOperationLogOperation);
			byte[] operationLogPayload = UdpHelper.getByteArray(executeOperationLogOperationPacket);
			DatagramPacket operationPacket = new DatagramPacket(operationLogPayload, operationLogPayload.length, address, replicaManager.getReplicaPort());
			try {
				// Would prefer an alternative, but it won't work without a delay
				// No access to error logs, and packet doesn't get sent otherwise
				Thread.sleep(400);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			newSocket.send(operationPacket);
			
			byte[] operationBuffer = new byte[BUFFER_SIZE];
			DatagramPacket operationPacketReply = new DatagramPacket(operationBuffer, operationBuffer.length);
			newSocket.receive(operationPacketReply);
			
			replicaManager.setRebooting(false);
			ReplicaRebootReply replicaRebootReply = new ReplicaRebootReply(result);
			Packet replyPacket = new Packet(Operation.REPLICA_REBOOT, replicaRebootReply);
			
			byte[] message = UdpHelper.getByteArray(replyPacket);
			DatagramPacket reply = new DatagramPacket(message, message.length, address, port);
			newSocket.send(reply);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (newSocket != null) {
				newSocket.close();
			}
		}
	}
}