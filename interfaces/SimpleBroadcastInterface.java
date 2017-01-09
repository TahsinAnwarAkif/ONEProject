/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package interfaces;

import java.util.Collection;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import static core.DTNHost.MaliciousNodes;
import core.NetworkInterface;
import core.Settings;
import java.lang.reflect.Array;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SimpleBroadcastInterface extends NetworkInterface {
	/**
	 * Reads the interface settings from the Settings file
	 *  
	 */
	public SimpleBroadcastInterface(Settings s)	{
		super(s);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SimpleBroadcastInterface(SimpleBroadcastInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new SimpleBroadcastInterface(this);
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		
                System.out.println("CONNECT!");
                
                if (isScanning()  
				&& anotherInterface.getHost().isActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
                         
			// new contact within range
			// connection speed is the lower one of the two speeds 
			
                        //System.out.println(anotherInterface.getHost().getAddress());
                        int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
                        /*
                        int n;
                        n = Array.getLength(DTNHost.MaliciousNodes);
                        
                        for (int i = 0; i < n; i++)
                        {
                              if (DTNHost.MaliciousNodes[i] == anotherInterface.getHost().getAddress())return;
                        }
                        */
                        
                        
                        connect(con,anotherInterface);
		}
	}

	/**
	 * Updates the state of current connections (ie tears down connections
	 * that are out of range).
	 */
	public void update() {
		// First break the old ones
		System.out.println("UPDATE!");
                optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);
                
			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";
                        
                        int f = 0;
                        for(int j = 0; j < MaliciousNodes.length; j++)
                                {
                                    if (anotherInterface.getHost().getAddress() == this.host.MaliciousNodes[j])
                                        f = 1;
                        
                                }
			
                        if (!isWithinRange(anotherInterface) || f == 1  ) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces) {
			/*
                        if (i.getHost().getAddress() == 4)
                        {
                            connections.remove(i);
                            
                            return;
                        }
                        */
                        connect(i);
		}
	}

	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
                System.out.println("CREATE CONNECTION!");
		
                if (!isConnected(anotherInterface) && (this != anotherInterface)) {    			
			// connection speed is the lower one of the two speeds
                        //if (anotherInterface.getHost().getAddress() == 4)return;
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SimpleBroadcastInterface " + super.toString();
	}

}
