package peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import sslengine.SSLUtils;
import handlers.ReadMessageCompletionHandler;

import java.util.concurrent.Executors;

public class Peer extends Node implements RMI {

    private static int POOL_SIZE = 20;
    private BigInteger id;
    private PeerSignature signature;
    private ConcurrentHashMap<Integer, PeerSignature> fingerTable;
    private PeerSignature predecessor;
    private ScheduledExecutorService threadPool;
    private PeerInformation peerInformation;
    private PeerFiles peerFiles;
    private AsynchronousChannelGroup group;
    private AsynchronousServerSocketChannel server;
    private String peer_root_folder;

    public Peer(InetAddress address, int port, BigInteger id, PeerSignature existingNode, String access_point)
            throws KeyManagementException, Exception {
        initPeer(address, port, id, access_point);
        joinChord(existingNode);
    }

    public Peer(InetAddress address, int port, BigInteger id, String access_point)
            throws KeyManagementException, Exception {
        initPeer(address, port, id, access_point);
        createChord();
    }

    public static void main(String[] args) {
        Peer peer = null;
        try {
            String accessPoint = args[0];
            int port = Integer.valueOf(args[1]);
            InetAddress ip = InetAddress.getByName(args[2]);
            BigInteger peerId = SHA256Hasher.hash(ip, port).mod(BigInteger.valueOf(2).pow(Constants.CHORD_MOD));
            MyLogger.print("Peer with ID " + peerId.toString() + " running! Address: " + ip.getHostAddress(),
                    Level.INFO);

            if (args.length < 5) {
                peer = new Peer(ip, port, peerId, accessPoint);
            } else {
                InetAddress successor_ip = InetAddress.getByName(args[3]);
                int successor_port = Integer.valueOf(args[4]);
                PeerSignature successorPeer = new PeerSignature(successor_ip, successor_port);
                peer = new Peer(ip, port, peerId, successorPeer, accessPoint);
            }

            // Create rmi stub
            RMI stub = (RMI) UnicastRemoteObject.exportObject(peer, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, stub);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initPeer(InetAddress address, int port, BigInteger id, String access_point)
            throws KeyManagementException, Exception {
        this.id = id;
        this.signature = new PeerSignature(address, port);
        MyLogger.bind(this.signature);
        this.fingerTable = new ConcurrentHashMap<>();
        this.threadPool = Executors.newScheduledThreadPool(POOL_SIZE);
        this.peerInformation = new PeerInformation();
        this.peerFiles = new PeerFiles();
        this.peer_root_folder = access_point;
        this.group = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(POOL_SIZE));
        this.server = SSLUtils.getServerSocketChannel(group);
        InetSocketAddress socket = new InetSocketAddress(address.getHostAddress(), port);
        this.server.bind(socket);

        this.listen();
    }

    public void createChord() {
        this.setPredecessor(null);
        this.setSuccessor(signature);
        this.stabilize();
    }

    public ScheduledExecutorService getThreadPool() {
        return this.threadPool;
    }

    public PeerSignature getSuccessor() {
        return this.fingerTable.get(1);
    }

    public ConcurrentHashMap<Integer, PeerSignature> getFingerTable() {
        return this.fingerTable;
    }

    public void listen() {
        this.server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(AsynchronousSocketChannel channel, Void attachment) {
                server.accept(null, this);
                threadPool.execute(() -> {
                    SSLUtils.readMessage(channel, new ReadMessageCompletionHandler(Peer.this, channel));
                });
            }

            public void failed(Throwable exc, Void attachment) {
                // throw new RuntimeException("unable to accept new connection", exc);
            }
        });
    }

    public void setPredecessor(PeerSignature predecessor) {
        if (predecessor != null)
            MyLogger.print("New Predecessor: " + predecessor.getId().toString(), Level.INFO);
        else
            MyLogger.print("Null Predecessor", Level.WARNING);

        this.predecessor = predecessor;
    }

    public void setSuccessor(PeerSignature successor) {

        PeerSignature peer;
        if (successor == null) {
            peer = this.signature;
        } else {
            peer = successor;
        }

        MyLogger.print("New Successor: " + peer.getId().toString(), Level.INFO);
        this.fingerTable.put(1, peer);
    }

    public BigInteger getId() {
        return this.id;
    }

    public PeerSignature getPredecessor() {
        return this.predecessor;
    }

    public int getPort() {
        return this.signature.getPort();
    }

    public InetAddress getAddress() {
        return this.signature.getAddress();
    }

    public PeerSignature getPeerSignature() {
        return this.signature;
    }

    public PeerInformation getPeerInformation() {
        return peerInformation;
    }

    public PeerFiles getPeerFiles() {
        return peerFiles;
    }

    public String getPeerRootFolder() {
        return peer_root_folder;
    }

    public AsynchronousChannelGroup getGroup() {
        return group;
    }

    /**
     * All the stabilization functiosn
     */
    public void stabilize() {

        CheckPredecessor c = new CheckPredecessor(this);
        Stabilize s = new Stabilize(this);
        FixFinger f = new FixFinger(this, Constants.CHORD_MOD);

        try {
            this.threadPool.scheduleAtFixedRate(c, 0, Constants.FIXED_RATE, TimeUnit.MILLISECONDS);
            this.threadPool.scheduleAtFixedRate(s, 0, Constants.FIXED_RATE, TimeUnit.MILLISECONDS);
            this.threadPool.scheduleAtFixedRate(f, 0, Constants.FIXED_RATE, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify successor that this node should be its predecessor
     */
    public void notify(PeerSignature node) {
        SendNotification notification = new SendNotification(node, this);
        this.threadPool.submit(notification);
    }

    @Override
    public void addFinger(PeerSignature signature, int index) {
        this.fingerTable.put(index, signature);
    }

    public PeerSignature getCloserPreviousNode(BigInteger id) {
        PeerSignature closestFinger = null;
        BigInteger maxId = BigInteger.valueOf(0);
        for (PeerSignature finger : this.fingerTable.values()) {
            if (between(maxId, finger.getId(), id)) {
                maxId = finger.getId();
                closestFinger = finger;
            }
        }

        return closestFinger;
    }

    public boolean joinChord(PeerSignature successor) {
        PeerSignature result = this.requestSuccessor(successor, this.signature.getId());

        if (successor == null) {
            MyLogger.print("Cannot find successor", Level.SEVERE);
            return false;
        }

        this.setSuccessor(result);

        // start running threads
        this.stabilize();

        return true;
    }

    public PeerSignature requestSuccessor(PeerSignature node, BigInteger key) {

        CountDownLatch latch = new CountDownLatch(1);
        SendSuccessorRequest request = new SendSuccessorRequest(node, this, key, latch);
        this.threadPool.submit(request);

        try {
            latch.await(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
            PeerSignature successor = request.getSuccessor();
            return successor;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public PeerSignature requestPredecessor(PeerSignature node) {
        // System.out.println(node.getAddress() + " " + node.getPort());
        CountDownLatch latch = new CountDownLatch(1);
        SendRequestPredecessor request = new SendRequestPredecessor(node, this, latch);
        this.threadPool.submit(request);
        try {
            latch.await(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS);
            PeerSignature predecessor = request.getPredecessor();
            return predecessor;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean between(BigInteger middle, BigInteger left, BigInteger right) {
        if (right.compareTo(left) > 0) {
            return middle.compareTo(left) > 0 && middle.compareTo(right) < 0;
        } else if (right.compareTo(left) < 0) {
            return middle.compareTo(left) > 0 || middle.compareTo(right) < 0;
        }
        return false;
    }

    public static String getFolder(String path) {
        String folder = path;
        File dir = new File(folder);
        if (!dir.exists())
            dir.mkdirs();
        return folder;
    }

    public PeerSignature findSuccessor(BigInteger id) {
        if (this.getSuccessor().getId().equals(this.signature.getId())
                || (this.predecessor == null && id.equals(this.signature.getId()))) {
            return this.signature;
        }

        if (this.predecessor != null && between(id, this.predecessor.getId(), this.id)) {
            return this.signature;
        }

        if (between(id, this.id, this.getSuccessor().getId())) {
            return this.getSuccessor();
        }

        if (id.equals(this.getSuccessor().getId())) {
            return this.getSuccessor();
        }

        PeerSignature closest = this.getCloserPreviousNode(id);

        return requestSuccessor(closest, id);
    }

    @Override
    public void backup(String file, int replicationDegree) {
        FileInfo file_info = new FileInfo(file, replicationDegree);
        Backup backup = new Backup(file_info, this);
        backup.backup();
    }

    @Override
    public void restore(String file) throws RemoteException {
        File f = new File(file);
        FileInfo fileinfo = this.peerFiles.getFile(f.getName());

        if (fileinfo == null) {
            MyLogger.print("File not backed up", Level.WARNING);
            return;
        }

        Restore restore = new Restore(this, fileinfo);
        restore.restore();
    }

    @Override
    public void delete(String file) throws RemoteException {
        File f = new File(file);
        FileInfo fileinfo = this.peerFiles.getFile(f.getName());

        if (fileinfo == null) {
            MyLogger.print("File not backed up", Level.WARNING);
            return;
        } else
            this.peerFiles.deleteBackupRequest(fileinfo.getFilename());

        DeleteProtocol deleteProtocol = new DeleteProtocol(fileinfo, this);
        deleteProtocol.delete();
    }

    @Override
    public void reclaim(int max_size_kbs) throws RemoteException {
        this.peerInformation.setMaxSpace(max_size_kbs);
        Reclaim reclaim = new Reclaim(this);
        reclaim.reclaim();
    }

    @Override
    public String state() throws RemoteException {
        String state = "ID: " + this.id;
        state += "\nAddress: " + this.getAddress().getHostAddress() + " | Port: " + this.getPort() + "\n";
        return state + this.peerInformation.toString();
    }
}