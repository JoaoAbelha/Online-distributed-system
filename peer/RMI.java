package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {

    public void backup(String file, int replicationDegree) throws RemoteException;

    public void restore(String file) throws RemoteException;

    public void delete(String file) throws RemoteException;

    public void reclaim(int max_size_kbs) throws RemoteException;

    public String state() throws RemoteException;
}