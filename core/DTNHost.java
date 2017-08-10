/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import static junit.runner.Version.id;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.sql.Types.NULL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.RoutingInfo;

/**
 * A DTN capable host.
 */

public class DTNHost implements Comparable<DTNHost> {
	private static int nextAddress = 0;
	private int address;

	private Coord location; 	// where is the host
	private Coord destination;	// where is it going

	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private List<NetworkInterface> net;
	private ModuleCommunicationBus comBus;
        
        public   ArrayList NodeInfo;
        public Map<DTNHost,Integer> MaliciousInfo;
        public Map<String,ArrayList> MessageInfo;
        public Map<DTNHost,ArrayList> NInfo;
        public   ArrayList MaliciousNodes;
        public   ArrayList MsgInfo;
        public static double RatioThreshold;
        public static double SumThreshold;
        public Date date = new Date();
        public static List<Double> t1,t2;
        public static ArrayList tfr1,tfr2;
        public  long counter;
        public int index;
        public static String fileName;
        public static FileOutputStream fs;
        public static BufferedWriter br;
        public static boolean[] flag;
        public ArrayList TAs  = new ArrayList() ;
      
	static {
                DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
                flag = new boolean[1000];
                fileName = "G:\\One Simulator\\out" + ".txt";
            try {
                fs = new FileOutputStream(fileName);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(DTNHost.class.getName()).log(Level.SEVERE, null, ex);
            }
                br = new BufferedWriter(new OutputStreamWriter(fs));
                
            try {
                br.write("START:-------");
                br.newLine();
            } catch (IOException ex) {
                Logger.getLogger(DTNHost.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
	/**
         * 
         * 
	 * Creates a new DTNHost.
	 * @param msgLs Message listeners
	 * @param movLs Movement listeners
	 * @param groupId GroupID of this host
	 * @param interf List of NetworkInterfaces for the class
	 * @param comBus Module communication bus object
	 * @param mmProto Prototype of the movement model of this host
	 * @param mRouterProto Prototype of the message router of this host
	 */
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus, 
			MovementModel mmProto, MessageRouter mRouterProto) throws FileNotFoundException, IOException {
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = getNextAddress();
		this.name = groupId+address;
		this.net = new ArrayList<NetworkInterface>();
		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
                this.NodeInfo       = new ArrayList();
                this.MaliciousInfo  = new HashMap<DTNHost,Integer>();
                this.MessageInfo    = new LinkedHashMap<String,ArrayList>();
                this.NInfo          = new HashMap<DTNHost,ArrayList>();
                //this.RatioThreshold = Double.parseDouble(this.br.readLine());
                //this.SumThreshold   = Double.parseDouble(this.br.readLine());

                this.RatioThreshold = 0.43;
                this.SumThreshold   = 100;
                this.counter        = (long) 20.0;
                this.t1 = new ArrayList();
                this.t2 = new ArrayList();
                this.tfr1 = new ArrayList();
                this.tfr2 = new ArrayList();
                this.TAs  = new ArrayList();
                
        }
	
	/**
	 * Returns a new network interface address and increments the address for
	 * subsequent calls.
	 * @return The next address.
	 */
	private static synchronized int getNextAddress() {
		return nextAddress++;	
	}

	/**
	 * Reset the host and its interfaces
	 */
	public static void reset() {
		nextAddress = 0;
	}

	/**
	 * Returns true if this node is active (false if not)
	 * @return true if this node is active (false if not)
	 */
	public boolean isActive() {
		return this.movement.isActive();
	}

	/**
	 * Set a router for this host
	 * @param router The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.address;
	}
	
	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}
	
    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
	}

	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a copy of the list of connections this host has with other hosts
	 * @return a copy of the list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> lc = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			lc.addAll(i.getConnections());
		}

		return lc;
	}

	/**
	 * Returns the current location of this host. 
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no
	 * path is in use at the moment.
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
	}


	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer 
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		double bSize = router.getBufferSize();
		double freeBuffer = router.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Returns the interface objects of the node
	 */
	public List<NetworkInterface> getInterfaces() {
		return net;
	}

	/**
	 * Find the network interface based on the index
	 */
	protected NetworkInterface getInterface(int interfaceNo) {
		NetworkInterface ni = null;
		try {
			ni = net.get(interfaceNo-1);
		} catch (IndexOutOfBoundsException ex) {
			System.out.println("No such interface: "+interfaceNo);
			System.exit(0);
		}
		return ni;
	}

	/**
	 * Find the network interface based on the interfacetype
	 */
	protected NetworkInterface getInterface(String interfacetype) {
		for (NetworkInterface ni : net) {
			if (ni.getInterfaceType().equals(interfacetype)) {
				return ni;
			}
		}
		return null;	
	}

	/**
	 * Force a connection event
	 */
	public void forceConnection(DTNHost anotherHost, String interfaceId, 
			boolean up) {
		NetworkInterface ni;
		NetworkInterface no;

		if (interfaceId != null) {
			ni = getInterface(interfaceId);
			no = anotherHost.getInterface(interfaceId);

			assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
			assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
		} else {
			ni = getInterface(1);
			no = anotherHost.getInterface(1);
			
			assert (ni.getInterfaceType().equals(no.getInterfaceType())) : 
				"Interface types do not match.  Please specify interface type explicitly";
		}
		
		if (up) {
			ni.createConnection(no);
		} else {
			ni.destroyConnection(no);
		}
	}

	/**
	 * for tests only --- do not use!!!
	 */
	public void connect(DTNHost h) {
		System.err.println(
				"WARNING: using deprecated DTNHost.connect(DTNHost)" +
		"\n Use DTNHost.forceConnection(DTNHost,null,true) instead");
		forceConnection(h,null,true);
	}

	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update(boolean simulateConnections) {
		
                DTNHost to = this.router.getHost();
                if (!isActive()) {
			return;
		}
		
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
                
                if( to.getAddress() == 1) forceToBeMalicious(to);
                if( to.getAddress() == 3) forceToBeMalicious(to);
                if( to.getAddress() == 5) forceToBeMalicious(to);
                if( to.getAddress() == 7) forceToBeMalicious(to);
                if( to.getAddress() == 9) forceToBeMalicious(to);
                if( to.getAddress() == 11) forceToBeMalicious(to);
                if( to.getAddress() == 13) forceToBeMalicious(to);
                if( to.getAddress() == 15) forceToBeMalicious(to);
                if( to.getAddress() == 17) forceToBeMalicious(to);
                if( to.getAddress() == 19) forceToBeMalicious(to);
                
                if( to.getAddress() == 21) forceToBeMalicious(to);    
                if( to.getAddress() == 23) forceToBeMalicious(to);
                if( to.getAddress() == 25) forceToBeMalicious(to);
                if( to.getAddress() == 27) forceToBeMalicious(to);
                if( to.getAddress() == 29) forceToBeMalicious(to);
                if( to.getAddress() == 31) forceToBeMalicious(to);
                /*
                if( to.getAddress() == 33) forceToBeMalicious(to);
                if( to.getAddress() == 35) forceToBeMalicious(to);
                if( to.getAddress() == 37) forceToBeMalicious(to);
                if( to.getAddress() == 39) forceToBeMalicious(to);
                /*
                if( to.getAddress() == 41) forceToBeMalicious(to);
                if( to.getAddress() == 43) forceToBeMalicious(to);
                if( to.getAddress() == 45) forceToBeMalicious(to);
                if( to.getAddress() == 47) forceToBeMalicious(to);
                if( to.getAddress() == 49) forceToBeMalicious(to);
                if( to.getAddress() == 51) forceToBeMalicious(to);
                if( to.getAddress() == 53) forceToBeMalicious(to);
                if( to.getAddress() == 55) forceToBeMalicious(to);
                if( to.getAddress() == 57) forceToBeMalicious(to);
                if( to.getAddress() == 59) forceToBeMalicious(to);         
                
                if( to.getAddress() == 63) forceToBeMalicious(to);
                if( to.getAddress() == 65) forceToBeMalicious(to);
                if( to.getAddress() == 67) forceToBeMalicious(to);
                if( to.getAddress() == 69) forceToBeMalicious(to);
                if( to.getAddress() == 70) forceToBeMalicious(to);
                if( to.getAddress() == 71) forceToBeMalicious(to);
                if( to.getAddress() == 72) forceToBeMalicious(to);
                if( to.getAddress() == 73) forceToBeMalicious(to);
                if( to.getAddress() == 74) forceToBeMalicious(to);
                if( to.getAddress() == 75) forceToBeMalicious(to);
                if( to.getAddress() == 76) forceToBeMalicious(to);
                if( to.getAddress() == 77) forceToBeMalicious(to);
                if( to.getAddress() == 78) forceToBeMalicious(to);
                if( to.getAddress() == 79) forceToBeMalicious(to);
                /*
                if( to.getAddress() == 61) forceToBeMalicious(to);
                if( to.getAddress() == 63) forceToBeMalicious(to);
                if( to.getAddress() == 65) forceToBeMalicious(to);
                if( to.getAddress() == 67) forceToBeMalicious(to);
                if( to.getAddress() == 69) forceToBeMalicious(to);
                
                if( to.getAddress() == 70) forceToBeMalicious(to);
                if( to.getAddress() == 71) forceToBeMalicious(to);
                if( to.getAddress() == 72) forceToBeMalicious(to);
                if( to.getAddress() == 73) forceToBeMalicious(to);
                if( to.getAddress() == 74) forceToBeMalicious(to);
                if( to.getAddress() == 75) forceToBeMalicious(to);
                if( to.getAddress() == 76) forceToBeMalicious(to);
                if( to.getAddress() == 77) forceToBeMalicious(to);
                if( to.getAddress() == 78) forceToBeMalicious(to);
                if( to.getAddress() == 79) forceToBeMalicious(to);
                */
                
                /*
                if( to.getAddress() == 31) forceToBeMalicious(to);
                if( to.getAddress() == 33) forceToBeMalicious(to);
                if( to.getAddress() == 35) forceToBeMalicious(to);
                if( to.getAddress() == 37) forceToBeMalicious(to);
                if( to.getAddress() == 39) forceToBeMalicious(to);
                if( to.getAddress() == 41) forceToBeMalicious(to);
                if( to.getAddress() == 43) forceToBeMalicious(to);
                if( to.getAddress() == 45) forceToBeMalicious(to);
                if( to.getAddress() == 47) forceToBeMalicious(to);
                if( to.getAddress() == 49) forceToBeMalicious(to);
                if( to.getAddress() == 51) forceToBeMalicious(to);
                if( to.getAddress() == 53) forceToBeMalicious(to);
                if( to.getAddress() == 55) forceToBeMalicious(to);
                if( to.getAddress() == 57) forceToBeMalicious(to);
                if( to.getAddress() == 59) forceToBeMalicious(to);
                */
                /*
                if( to.getAddress() == 93) forceToBeMalicious(to);
                if( to.getAddress() == 96) forceToBeMalicious(to);
                
                if( to.getAddress() == 99) forceToBeMalicious(to);
                if( to.getAddress() == 102) forceToBeMalicious(to);
                if( to.getAddress() == 105) forceToBeMalicious(to);
                */
               
                
                this.router.update();
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is
	 * not time to move yet
	 * @param timeIncrement How long time the node moves
	 */
	public void move(double timeIncrement) {		
		double possibleMovement;
		double distance;
		double dx, dy;

		if (!isActive() || SimClock.getTime() < this.nextTimeToMove) {
			return; 
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				return; // no more waypoints left
			}
			distance = this.location.distance(this.destination);
		}

		// move towards the point for possibleMovement amount
		dx = (possibleMovement/distance) * (this.destination.getX() -
				this.location.getX());
		dy = (possibleMovement/distance) * (this.destination.getY() -
				this.location.getY());
		this.location.translate(dx, dy);
	}	

	/**
	 * Sets the next destination and speed to correspond the next waypoint
	 * on the path.
	 * @return True if there was a next waypoint to set, false if node still
	 * should wait
	 */
	private boolean setNextWaypoint() {
		if (path == null) {
			path = movement.getPath();
		}

		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		
            this.router.sendMessage(id, to);
            //}
            }
        /**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
        
        
        //FINDING AN ENTRY OF N2 IN N1 TABLE
        public int NodePresentAlready(DTNHost n1,DTNHost n2)
        {
        DTNHost[] node = new DTNHost[1000];
        for(int i = 0; i <n1.NodeInfo.size(); i++)
            {
                ArrayList tmp =(ArrayList)n1.NodeInfo.get(i);
                node[i]       = (DTNHost)tmp.get(0);
                if(node[i] == n2)return i;
            }
        return -1;
        }
        
        public void NodeInfoUpdate(DTNHost n, String id)
        { 
         ArrayList tmpN = new ArrayList();
       //ArrayList tmpp = new ArrayList(n.MessageInfo.values());
         ArrayList tmp  = (ArrayList) n.MessageInfo.get(id);
         DTNHost    source = (DTNHost)tmp.get(1);
         DTNHost    dst    = (DTNHost)tmp.get(2);
         
         if(!n.NInfo.containsKey(source))
         {
                tmpN.add(1.0);
                tmpN.add(0.0);
                tmpN.add(POSITIVE_INFINITY);
                n.NInfo.put(source,tmpN);
         }
         else
         {
                double frtt = 0.0;
                ArrayList a;
                a =(ArrayList)n.NInfo.get(source);
                double ftt = (double)a.get(0) + 1.0;
                double rtt = (double)a.get(1);
                
                if( rtt == 0.0 ) frtt = POSITIVE_INFINITY;
                  
                else 
                                 frtt = (double)(ftt / rtt);
                 
                a.set(0,ftt);
                a.set(2, frtt );
                
                if (frtt <= RatioThreshold && ftt + rtt >= SumThreshold)
                    {
                    int value    = 1;
                    boolean flag = n.MaliciousInfo.containsKey(source);     
                    if(flag)value= (int) n.MaliciousInfo.get(source) + 1;
                    n.MaliciousInfo.put(source,value);
                    
                    //if(!flag)System.out.println("MALICIOUS NODE: "+source+" FIRST FOUND AT: "+SimClock.getTime()/1000.0);
                    } 
         }
         
         tmpN = new ArrayList();
         
         if(!n.NInfo.containsKey(dst))
         {
                tmpN.add(0.0);
                tmpN.add(1.0);
                tmpN.add(0.0);
                n.NInfo.put(dst,tmpN);
         }
         else
         {
                double frtt = 0.0;
                ArrayList a;
                a =(ArrayList)n.NInfo.get(dst);
                double ftt = (double) a.get(0);
                double rtt = (double) a.get(1) + 1.0;
                frtt = (double)(ftt / rtt);
                a.set(1,rtt);
                a.set(2, frtt );
   
                if (frtt <= RatioThreshold && ftt + rtt >= SumThreshold)
                    {
                    int value    = 1;
                    boolean flag = n.MaliciousInfo.containsKey(dst);     
                    if(flag) value = (int) n.MaliciousInfo.get(dst) + 1;    
                    n.MaliciousInfo.put(dst,value);
                    
                    //if(!flag)System.out.println("MALICIOUS NODE: "+dst+" FIRST FOUND AT: "+SimClock.getTime()/1000.0);
                    }
         }
        }   
        
        public boolean isFound(DTNHost n,ArrayList Entry)
        {
        ArrayList tmp = new ArrayList();
        for(int i = 0; i < n.MsgInfo.size();i++)
        {
            tmp = (ArrayList)n.MsgInfo.get(i);
            if(tmp == Entry) return true;
        }
        
        return false;
        }
        public void getAllMsgs(DTNHost from,DTNHost to)
        {
        
            
        //ADDING IN FROM TABLE    
        for(String key: to.MessageInfo.keySet())
        {
            if(!from.MessageInfo.containsKey(key))
            {
                ArrayList tmp;
                tmp = (ArrayList) to.MessageInfo.get(key);    
                from.MessageInfo.put(key,tmp);
                NodeInfoUpdate(from, key);
            }
        }
        
        //ADDING IN TO TABLE    
        for(String key: from.MessageInfo.keySet())
        {
            if(!to.MessageInfo.containsKey(key))
            {
                ArrayList tmp;
                tmp = (ArrayList) from.MessageInfo.get(key);    
                to.MessageInfo.put(key,tmp);
                NodeInfoUpdate(to,key);
            }
        }
        
        }
        
        public void ShareMaliciousTables(DTNHost from,DTNHost to)
        {

         //ADDING IN FROM TABLE
         for (DTNHost key : to.MaliciousInfo.keySet())
         {
            if(from.MaliciousInfo.containsKey(key))
                from.MaliciousInfo.put(key,(int)from.MaliciousInfo.get(key)+1);
            
            else 
                from.MaliciousInfo.put(key,1);
         }
         
         //ADDING IN TO TABLE
         for (DTNHost key : from.MaliciousInfo.keySet())
         {
            if(to.MaliciousInfo.containsKey(key))
                to.MaliciousInfo.put(key,(int)to.MaliciousInfo.get(key)+1);
            
            else 
                to.MaliciousInfo.put(key,1);
         }
        }
        
        
        
        
        
        public int receiveMessage(Message m, DTNHost from) {
		
                
                int retVal = this.router.receiveMessage(m, from); 
                // MsgInfo.put(m, );
                if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}

		return retVal;	
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
        public void forceToBeMalicious(DTNHost n)
        {
        	
            	ArrayList<Message> temp = 
				new ArrayList<Message>(n.getMessageCollection());
				for(int y=0;y<temp.size();y++)
				{
                                    Message m = temp.get(y);
                                    if(this.router.getMessage(m.id) == temp.get(y) && temp.size() > 4) 
                                     {
                                        if(y ==0 || y == 1) router.deleteMessage(m.id,true);
                                        System.out.println("---------------------------------------------NODE "+n.name+"  DROPPED MESSAGE "+m.id+"------------------------------------------");
                                     }
                                 }
        }
        
        public void messageTransferred(String id, DTNHost from) throws IOException 
        {
   
            DTNHost to = this.router.getHost();
            ArrayList tmp = new ArrayList();
            ArrayList tmp2 = new ArrayList();
            //String timeStamp = Long.toString(date.getTime());
            String timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());

            tmp.add(timeStamp);
            tmp.add(from);
            tmp.add(to);
            
            from.MessageInfo.put(id,tmp);
            NodeInfoUpdate(from,id);
            
            to.MessageInfo.put(id,tmp);
            NodeInfoUpdate(to,id);
            
            getAllMsgs(from,to);
 
            //MSG INFO
            /*
            System.out.println();
            System.out.println("MSG INFO TABLE OF NODE: "+from);
            System.out.println(from.MessageInfo);
            
            System.out.println();
            System.out.println("MSG INFO TABLE OF NODE: "+to);
            System.out.println(to.MessageInfo);
            */
            
            //NODE INFO
            /*
            System.out.println();
            System.out.println("NODE INFO TABLE OF NODE: "+from);
            System.out.println(from.NInfo);
            
            System.out.println("NODE INFO TABLE OF NODE: "+to);
            System.out.println(to.NInfo);
            
            //MALICIOUS TABLE
            /*
            System.out.println("BEFORE SHARING: MALICIOUS INFO TABLE OF NODE: "+from);
            System.out.println(from.MaliciousInfo);
            
            System.out.println("BEFORE SHARING: MALICIOUS INFO TABLE OF NODE: "+to);
            System.out.println(to.MaliciousInfo);
            */
            ShareMaliciousTables(from,to); 
            br.flush();
            
            
            for(DTNHost key: from.MaliciousInfo.keySet())
            { 
                if( from.MaliciousInfo.get(key) == 20 && flag[from.getAddress()] == false ) 
                {
                    flag[from.getAddress()] = true;
                    br.write("MALICIOUS NODE: "+key+" FOUND AT: "+SimClock.getTime()/1000.0);
                    br.newLine();               
                    br.flush();
                }
            }
            
            for(DTNHost key: to.MaliciousInfo.keySet())
            { 
                if( to.MaliciousInfo.get(key) == 20 && flag[to.getAddress()] == false )
                {
                    flag[to.getAddress()] = true;
                    br.write("MALICIOUS NODE: "+key+" FOUND AT: "+SimClock.getTime()/1000.0);
                    br.newLine();
                    br.flush();
                }
            }
            
            
            this.router.messageTransferred(id, from);
        }

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * @param m The message to create
	 */
	public void createNewMessage(Message m) {
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * @param otherHost The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}

}
