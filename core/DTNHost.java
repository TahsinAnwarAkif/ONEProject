/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;


import static java.awt.PageAttributes.MediaType.A;
import static java.awt.PageAttributes.MediaType.B;
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
        public   int[] MaliciousNodes;
        public   ArrayList MsgInfo;
        public double Threshold;
  
	static {
		DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
	}
	/**
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
			MovementModel mmProto, MessageRouter mRouterProto) {
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
                this.MaliciousNodes = new int[1000];
                this.MsgInfo        = new ArrayList();
                this.Threshold      = 1.0;
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
		if (!isActive()) {
			return;
		}
		
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
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
		
            System.out.println("SEND!");
            /*
            int n;
            n = Array.getLength(MaliciousNodes);
            for (int i = 0; i < n; i++)
            {
                if (MaliciousNodes[i] == to)return;
            }
            */
            this.router.sendMessage(id, to);
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
        
        public void sortMsgInfo(DTNHost host)
        {
         ArrayList arr = new ArrayList();
         String[] str = new String[10000];
         DTNHost[] src  = new DTNHost[10000];
         DTNHost[] dest = new DTNHost[10000];
         int n = host.MsgInfo.size();
         
         for(int i = 0; i < n; i++)
         {
            arr = (ArrayList) host.MsgInfo.get(i);
            
            str[i] = (String) arr.get(0);
            src[i] = (DTNHost)arr.get(1);
            dest[i]= (DTNHost)arr.get(2);
         }
         int j;
         boolean flag = true;   
         String temp;
         DTNHost temp2;
         DTNHost temp3;
         while ( flag )
     {
            flag= false;    
            
            for( j=0;  j < n - 1;  j++ )
            {
                   if ( str[ j ].compareTo(str[j+1]) < 0 )  
                   {
                           temp = str[ j ];
                           temp2 = src[j];
                           temp3 = dest[j];
                           
                           str[ j ] = str  [ j+1 ];
                           src[j]   = src  [ j+1 ];
                           dest[j]  = dest [ j+1 ];
                           
                           str[ j+1 ] = temp;
                           src[ j+1 ] = temp2;
                           dest[ j+1 ] = temp3;
                           flag = true;         
                  } 
            } 
      }
                            host.MsgInfo = new ArrayList();
                            for(int i = 0; i < n; i++)
                            {
                                 ArrayList tmp = new ArrayList();
                                 
                                 tmp.add(str[i]);
                                 tmp.add(src[i]);
                                 tmp.add(dest[i]);
                                 host.MsgInfo.add(tmp);
        
                            }
        
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
        
        public void NodeInfoUpdate(DTNHost n)
        {
         //n.MaliciousNodes[0] = 1;
         DTNHost[] src = new DTNHost[1000]; 
         DTNHost[] dest= new DTNHost[1000];
         int flag1 = 0,flag2 = 0;
         
         DTNHost[] node = new DTNHost[1000]; 
         double[]     FTT  = new double[1000];
         double[]     RTT  = new double[1000];
         double[]  Ratio= new double[1000];
         
         
         ArrayList tmpN = new ArrayList();
         //GETTING NODE INFO ENTRIES
         
         for(int i = 0; i <n.NodeInfo.size(); i++)
            {
                tmpN =(ArrayList)n.NodeInfo.get(i);
                node[i] =(DTNHost)tmpN.get(0); 
                FTT[i]  =(double) tmpN.get(1);
                RTT[i]  =(double) tmpN.get(2); 
                Ratio[i]=(double) tmpN.get(3);
            }
         
         for(int i = 0; i <n.MsgInfo.size(); i++)
            {
                
                ArrayList  tmp     =(ArrayList)n.MsgInfo.get(i);
                           src[i]  = (DTNHost)tmp.get(1);
                          dest[i]  = (DTNHost)tmp.get(2);
                
                
                if(NodePresentAlready(n,src[i]) != -1)
                    {
                        int tt = NodePresentAlready(n,src[i]);
                        
                        tmpN =(ArrayList)n.NodeInfo.get(tt);
                            
                        FTT[tt] = FTT[tt] + 1.0;
                        if( RTT[tt] == 0.0 ) Ratio[tt] = POSITIVE_INFINITY;//OK
                        else Ratio[tt] = (double)(FTT[tt]/RTT[tt]);
                        
                        //tmpN =(ArrayList)n.NodeInfo.get(tt);
                        tmpN.set(1,FTT[tt]);
                        tmpN.set(3,Ratio[tt]);    
                    }
                else 
                    {
                        ArrayList tmp2 = new ArrayList();
                        
                        //FTT[n.NodeInfo.size()] = 1;
                        tmp2.add(src[i]);
                        tmp2.add(1.0);
                        tmp2.add(0.0);
                        tmp2.add(POSITIVE_INFINITY);
                        
                        n.NodeInfo.add(tmp2);
                
                    }
                //if (Ratio [i] != Threshold) n.MaliciousNodes[n.MaliciousNodes.length+1] = node[i].getAddress();
                
                
                
                if(NodePresentAlready(n,dest[i]) != -1)
                    {
                        int tt = NodePresentAlready(n,dest[i]);
                       
                        tmpN =(ArrayList)n.NodeInfo.get(tt);
                       
                        RTT[tt] = RTT[tt] + 1.0;    
                        Ratio[tt] = (double)(FTT[tt]/RTT[tt]);
                        tmpN.set(2,RTT[tt]);
                        tmpN.set(3,Ratio[tt]);    
                    }
                else 
                    {
                        ArrayList tmp2 = new ArrayList();
                        
                        tmp2.add(dest[i]);
                        tmp2.add(0.0);
                        tmp2.add(1.0);
                        tmp2.add(0.0);
                        n.NodeInfo.add(tmp2);
                    }
                        
                
        }
         System.out.println("NODE INFO TABLE: "+n);
         for(int j = 0; j < n.NodeInfo.size(); j++)
                {
                double[]        R = new double[1000];
                DTNHost[]   node1 = new DTNHost[1000];
                ArrayList       t = (ArrayList)n.NodeInfo.get(j);
                              R[j]=(double)t.get(3);
                          node1[j]=(DTNHost)t.get(0);
                //if (R[j] != Threshold) n.MaliciousNodes[n.MaliciousNodes.length+1] = node1[j].getAddress();
                System.out.println(t);          
                }
          
        }
        public  void getAllMsgs(DTNHost from,DTNHost to)
        {
         ArrayList fromMsg = from.MsgInfo;
         ArrayList toMsg   = to.MsgInfo;
         
         
         
         if(fromMsg.size() == 0 && toMsg.size() == 0) return;
         
         
         else
             {
               ArrayList Ti1 = (ArrayList) fromMsg.get(0);
               ArrayList Ti2 = (ArrayList) toMsg.get(0);
         
               String T1 = (String) Ti1.get(0);
               String T2 = (String) Ti2.get(0);  
               
               //update n1 node
               if (T2.compareTo(T1) > 0)
               {
                String[] Time  = new String[1000];   
                DTNHost[] src  = new DTNHost[1000];
                DTNHost[] dest = new DTNHost[1000];
                
                for(int i = 0; i < toMsg.size() ; i++)
                    {     
                         ArrayList tmp = (ArrayList)toMsg.get(i);
                         Time[i] = (String)tmp.get(0);
                         if(Time[i].compareTo(T1) > 0) fromMsg.add(tmp);
                         else break;
                    }
                
               from.MsgInfo = fromMsg;
               sortMsgInfo(from); 
               
               }
               
               //update n2 node
               else if (T2.compareTo(T1) < 0)
               {
                String[] Time  = new String[1000];   
                DTNHost[] src  = new DTNHost[1000];
                DTNHost[] dest = new DTNHost[1000];
                
                for(int i = 0; i < fromMsg.size() ; i++)
                    {     
                         ArrayList tmp = (ArrayList)fromMsg.get(i);
                         Time[i] = (String)tmp.get(0);
                         if(Time[i].compareTo(T2) > 0) toMsg.add(tmp);
                         else break;
                    }
               to.MsgInfo = toMsg; 
               sortMsgInfo(to); 
               
               }
             }
            }
        public boolean MaliciousAlready(DTNHost n,int Entry)
        {
        int[] tmp = new int[1000];
        for(int i = 0; i < n.MaliciousNodes.length;i++)
        {
            tmp[i] = n.MaliciousNodes[i];
            if(tmp[i] == Entry) return true;
        }
        
        return false;
        }
        public void ShareMaliciousTables(DTNHost from,DTNHost to)
        {
        //from.MaliciousNodes[0] = 1;
        //to.MaliciousNodes[0] = 1;
        
        int[] tmp = new int[1000];
            
        //ADDING IN FROM TABLE    
        for (int i = 0; i < to.MaliciousNodes.length; i++)
            {
                tmp[i] = to.MaliciousNodes[i];
                if(!MaliciousAlready(from,tmp[i]))from.MaliciousNodes[from.MaliciousNodes.length] = tmp[i];
        
            }
        //ADDING IN TO TABLE    
        for (int i = 0; i < from.MaliciousNodes.length; i++)
            {
                tmp[i] = from.MaliciousNodes[i];
                if(!MaliciousAlready(to,tmp[i]))to.MaliciousNodes[to.MaliciousNodes.length] = tmp[i];
        
            }
        }
        
        
        
        
        public int receiveMessage(Message m, DTNHost from) {
		System.out.println("RECEIVE!");
                 DTNHost to = this.router.getHost();
                ArrayList tmp = new ArrayList();
                String timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
                tmp.add(timeStamp);
                tmp.add(from);
                tmp.add(to);
                if(!isFound(from,tmp))from.MsgInfo.add(tmp);
                if(!isFound(to,tmp))  to.MsgInfo.add(tmp);
                
                sortMsgInfo(from);
                sortMsgInfo(to);
                
                getAllMsgs(from,to);
                
                NodeInfoUpdate(from);
                NodeInfoUpdate(to);
                
                ShareMaliciousTables(from,to);
                
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
	public void messageTransferred(String id, DTNHost from) {
		
               
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
