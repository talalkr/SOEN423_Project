package replica_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jeremy_replica.log.ILogger;

public class ReplicaManager implements Runnable {
	private final int BUFFER_SIZE = 5000;
	private final int THREAD_POOL_SIZE = Integer.MAX_VALUE;
	private final ExecutorService threadPool;
	private ILogger logger;
	private String replicaPath;
	private int port;
	private int replicaPort;
	private int replicaManagerSequencerPort;
	int[] otherReplicaManagers;
	private Process replica;
	private boolean isRebooting = false;
	private Thread shutdownHook;
	private final String tag;
	
	public ReplicaManager(int port, int replicaPort, int replicaManagerSequencerPort, int[] otherReplicaManagers, String replicaPath, ILogger logger) {
		super();
		this.tag = "REPLICA_MANAGER_" + port;
		this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		this.replicaPath = replicaPath;
		this.port = port;
		this.replicaManagerSequencerPort = replicaManagerSequencerPort;
		this.replicaPort = replicaPort;
		this.otherReplicaManagers = otherReplicaManagers;
		this.logger = logger;
		this.replica = null;
		this.isRebooting = false;
		this.shutdownHook = null;
	}
	
	public int[] getReplicaManagerPorts() {
		return otherReplicaManagers;
	}

	public void setOtherReplicaManagers(int[] otherReplicaManagers) {
		this.otherReplicaManagers = otherReplicaManagers;
	}

	public int getReplicaManagerSequencerPort() {
		return replicaManagerSequencerPort;
	}

	public void setReplicaManagerSequencerPort(int replicaManagerSequencerPort) {
		this.replicaManagerSequencerPort = replicaManagerSequencerPort;
	}

	public synchronized boolean isRebooting() {
		return isRebooting;
	}

	public synchronized void setRebooting(boolean isRebooting) {
		this.isRebooting = isRebooting;
	}

	public int getPort(){
		return this.port;
	}
	
	public int getReplicaPort(){
		return this.replicaPort;
	}

	@Override
	public void run() {
		initReplica();
		serveRequests();
	}
	
	public synchronized boolean killReplica(){
		try {
			if(replica == null){
				return true;
			}		
			if(replica.isAlive()){
				replica.destroy();
				replica.waitFor();
				resetShutdownHook();
				logger.log(tag, "REPLICA_KILL", "Replica: " + replicaPath + " was killed.");
			} else {
				logger.log(tag, "REPLICA_ALREADY_DEAD", "Replica: " + replicaPath + " was already dead.");
			}
			return true;
		} catch (InterruptedException e) {
			logger.log(tag, "REPLICA_KILL_ERROR", e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	public synchronized boolean rebootReplica(){
		try {
			Runtime runtime = Runtime.getRuntime();
			if(replica != null){
				replica.destroy();
				replica.waitFor();
			}
			replica = runtime.exec(replicaPath);
			logger.log(tag, "REPLICA_REBOOTED", "Replica: " + replicaPath + " was rebooted.");
			resetShutdownHook();
			return true;
		} catch (IOException e) {
			logger.log(tag, "REPLICA_REBOOT_ERROR", e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e1) {
			logger.log(tag, "REPLICA_REBOOT_ERROR", e1.getMessage());
			e1.printStackTrace();
		}
		return false;
	}
	
	private void initReplica(){		
		try {
			Runtime runtime = Runtime.getRuntime();
			replica = runtime.exec(replicaPath);
			logger.log(tag, "REPLICA_INITIALIZED", "Replica: " + replicaPath + " was initialized.");
			resetShutdownHook();
		} catch (IOException e) {
			logger.log(tag, "REPLICA_INIT_ERROR", e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void resetShutdownHook(){
		Runtime runtime = Runtime.getRuntime();
		if(shutdownHook != null){
			runtime.removeShutdownHook(shutdownHook);	
		}
		shutdownHook = new Thread(() -> {
			try {
				if(replica != null){
					replica.destroy();
					logger.log(tag, "REPLICA_DESTROYED", "Replica: " + replicaPath + " was destroyed by JVM shutdown hook.");	
				}
			} catch (Exception e) {
				logger.log(tag, "REPLICA_DESTROY_FAIL", e.getMessage());
				e.printStackTrace();
			} finally {
				replica = null;	
			}
		});
		runtime.addShutdownHook(shutdownHook);
	}

	private void serveRequests() {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
			while (true) {
				byte[] buffer = new byte[BUFFER_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				threadPool.execute(new ReplicaManagerPacketDispatcher(socket, packet, this));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null){
				socket.close();
			}
		}
	}
}