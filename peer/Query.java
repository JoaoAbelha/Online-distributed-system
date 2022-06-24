package peer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Query {

    public static void main(final String[] args) {

        final int len = args.length;

        if (len < 2 || len > 4) {
            System.exit(1);
        }

        String peer = args[0];
        String protocol = args[1];
        String filename = null;
        if (len > 2)
            filename = args[2];

        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            RMI stub = (RMI) registry.lookup(peer);

            switch (protocol) {
                case "BACKUP":
                    if (len != 4) {
                        System.out.println("Usage: <Peer> BACKUP <file> <replication degree>");
                        System.exit(1);
                    }
                    final int degree = Integer.parseInt(args[3]);
                    System.out.println("Starting backup");
                    stub.backup(filename, degree);
                    break;
                case "RESTORE":
                    if (len != 3) {
                        System.out.println("Usage: <Peer> RESTORE <file>");
                        System.exit(1);
                    }
                    stub.restore(filename);
                    break;
                case "DELETE":
                    if (len != 3) {
                        System.out.println("Usage: <Peer> DELETE <file>");
                        System.exit(1);
                    }
                    stub.delete(filename);
                    break;
                case "RECLAIM":
                    if (len != 3) {
                        System.out.println("Usage: <Peer> RECLAIM <max_size_kbs>");
                        System.exit(1);
                    }
                    stub.reclaim(Integer.parseInt(args[2]));
                    break;
                case "STATE":
                    if (len != 2) {
                        System.out.println("Usage: <Peer> STATE");
                        System.exit(1);
                    }
                    System.out.println(stub.state());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}