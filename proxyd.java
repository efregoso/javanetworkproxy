import java.net.*; 
import java.io.*;

public class proxyd extends Thread {
  
  //byte buffer for holding input from streams
  private byte[] fromStream = new byte[1000];
  //Socket for listening for incoming messages
  
  //OutputStream for socketFromBrowser
  private OutputStream browserOutStream;
  //InputStream for socketFromBrowser
  private InputStream browserStream;
  //A string for holding the remote host name
  private String remoteHost;
  //A byte buffer for sending back to the browser
  private byte[] b = new byte[1000];
  //A socket connected to the remote server;
  private Socket socketToRemote;
  //Input & Output streams for socketToRemote
  private OutputStream remoteStream;
  private InputStream incomingRemote;
  //A socket from the browser
  private Socket socketFromBrowser;
  //A String to store a text representation of the new header sent to the remote host
  private String textHeaderEdit;
  //A String to store a text representation of the entire URL
  private String regex;
  
  //A constructor for creating a proxy item from a given socket
  proxyd(Socket socket){
    this.socketFromBrowser = socket;
  }
  
  //The main method for the proxy
  public static void main(String[] args) {
    //create a new ServerSocket & Socket pair to accept incoming messages from the browser
    try {
      ServerSocket fromBrowser = new ServerSocket(5008);
      System.out.println("Socket fromBrowser port: " + fromBrowser.getLocalPort());
      System.out.println("Ready to receive browser requests.");
      Socket s = fromBrowser.accept();
      while (s.isConnected() == true) {
        proxyd p = new proxyd(s);
        p.start();
        s = fromBrowser.accept();
      }
      //When there are no more requests from the browser, close the ServerSocket.
      fromBrowser.close();
      //Print status
      System.out.println("Socket fromBrowser has closed.");
    }   
    
    //Catch any IOExceptions & print a status message with an exit
    catch (IOException i) {
      System.out.println("First IO Exception encountered.");
      System.exit(1);
    }
  }
  
  //A method for running a thread
  public void run() {
    
    //Print status
    System.out.println(this.getId() + ": Connection accepted.");
    
    //Get input & output streams for the socket connected to the browser
    try {
      this.browserOutStream = this.socketFromBrowser.getOutputStream();
      this.browserStream = this.socketFromBrowser.getInputStream();
      //If there is something to be read from the stream, read it into byte buffer fromStream
      if (this.browserStream.available() > 0) {
        this.browserStream.read(this.fromStream);
      }
    }
    
    //Catch any IOExceptions & print status with an exit
    catch (IOException i) {
      System.out.println(this.getId() + ": " + "Second IOException encountered.");
      System.exit(1);
    }
    
    //Byte array for storing header
    byte[] header = new byte[1000];
    
    //Preserve pointer for where the first instance of the carriage return sequence is found
    int endPointer = 0;
    
    //Find the header in this request
    for (int j = 0; this.fromStream[j] != 0; j++) {
      //if the bytes found are a double carriage return
      //to signify the end of the header
      if (this.fromStream[j] == 13 && this.fromStream[j + 1] == 10 && this.fromStream[j + 2] == 13 && this.fromStream[j + 3] == 10) {
        //copy everything from 0 to j-1 into the header byte array 
        for (int k = 0; k < j; k++) {
          header[k] = this.fromStream[k];
        }
        endPointer = j - 1;
      }
    }
    
    //Convert each byte in the header to a character
    StringBuilder asciiHeader = new StringBuilder();
    
    //Append each character to asciiHeader StringBuilder
    for (int l = 0; l < header.length; l++) {
      asciiHeader.append((char)header[l]);
    }
    
    //Convert asciiHeader StringBuilder to a string
    String textHeader = asciiHeader.toString();
    
    //DEBUGGING
    System.out.println(this.getId() + ": " + "The original textheader of the browser request: " + textHeader);
    //END DEBUGGING */
    
    //Find the URL in requests containing one
    if (textHeader.indexOf("http://") != -1) {
      int httpBegin = textHeader.indexOf("http://");
      
      //Find the double backslashes
      int doubleBackslashes = textHeader.indexOf("//");
      
      //DEBUGGING
      System.out.println(this.getId() + ": " + "http:// starts here: " + httpBegin);
      System.out.println(this.getId() + ": " + "Double backslashes are here: " + doubleBackslashes);
      //END DEBUGGING
      
      //Get the remote host domain
      this.remoteHost = textHeader.substring(doubleBackslashes + 2, textHeader.indexOf('/', doubleBackslashes + 2));
      
      //Find where the URL ends
      int firstSpace = textHeader.indexOf(" ", httpBegin);
      int httpEnd = firstSpace - 1;
      
      //A string for holding the entire http:// expression
      this.regex = textHeader.substring(httpBegin, httpEnd + 1);
      
      //A string for containing the relative URL
      String relativeURL;
      
      //Find the relative URL from the first backslash after the double backslash, on
      int firstSingleBackslash = this.regex.indexOf('/', doubleBackslashes + 2);
      relativeURL = this.regex.substring(firstSingleBackslash, this.regex.length());
      
      //Edit header so that Connections:keep-alive is changed to Connection:close
      //Also, replace regex with the relativeURL
      this.textHeaderEdit = textHeader.replaceAll("Connection: keep-alive", "Connection: close");
      if (relativeURL.length() > 1) {
        this.textHeaderEdit = textHeaderEdit.replaceAll(this.regex, relativeURL);
      }
      
      //This might be where the problem is, depending on if the problematic thread returns a b with no characters as the textHeader
      else {
        this.textHeaderEdit = this.textHeaderEdit.replaceAll(this.regex, "/index.html");
      }
      
      //Convert textHeaderEdit back into bytes
      try {
        this.b = this.textHeaderEdit.getBytes("ASCII");
        //DEBUGGING
        System.out.println("Samples from b: " + b[0] + ", " + b[1] + ", " + b[12]);
        //END DEBUGGING
      }
      
      catch (UnsupportedEncodingException p) {
        System.out.println(this.getId() + ": " + "UnsupportedEncodingException: textHeaderEdit");
        System.exit(1);
      }
    }  
    
    else {
      //Convert textHeaderEdit back into bytes to be sent as is
      try {
        this.textHeaderEdit = textHeader;
        this.b = this.textHeaderEdit.getBytes("ASCII");
      }
      catch (UnsupportedEncodingException p) {
        System.out.println(this.getId() + ": " + "UnsupportedEncodingException: textHeaderEdit");
        System.exit(1);
      }
    }
    
    //Counter for actual length of the message
    int l;
    
    //A byte buffer for synthesizing the header & the data
    byte[] b4 = new byte[10000];
    
    //Write the entirety of non-empty bytes to the new byte stream
    for (l = 0; l < this.b.length; l++) {
      if ( this.b[l] != 0 ) {
        b4[l] = this.b[l];
      }
    }
    //Then, move into fromStream, starting from endPointer + 1
    for (int m = endPointer + 1; m < this.fromStream.length; m++) {
      if ( this.fromStream[m] != 0) {
        b4[l] = this.fromStream[m];
        l++;
      }
    }
    
    //Another new, accurately-sized byte stream to send to the remote host
    byte[] ret = new byte[l];
    
    //Get rid of all the empty space after the message to reduce the bytes
    for (int p = 0; p < l; p++) {
      ret[p] = b4[p];
    }
    
    //DEBUGGING
    System.out.println(this.getId() + ": " + "Sample from ret: " + ret[0] + ", " + ret[1] + ", " + ret[12]);
    //END DEBUGGING
    
    //Send off b to the remote host in the output stream
    //Receive bytes from the remote host, & send them directly to the browser
    try {
      //turn the remote host domain into an InetAddress
      InetAddress remoteHostName = InetAddress.getByName(this.remoteHost);
      
      //create an output Socket, socketToRemote, connected to the remote server
      this.socketToRemote = new Socket(remoteHostName, 80);
      
      //Get output & input streams for this socket
      this.remoteStream = socketToRemote.getOutputStream();
      this.incomingRemote = socketToRemote.getInputStream();
      
      //Write to the OutputStream to send the request to the remote host
      this.remoteStream.write(ret);
      this.remoteStream.flush();
      
    }
    
    //Catch any IOExceptions & print a detailed status message with an exit
    catch (IOException i) {
      System.out.println(this.getId() + ": " + "Third IO Exception encountered. Remotehost: " + this.remoteHost + "; Full URL: " + this.regex + "; TextHeader sent to host: " + this.textHeaderEdit + ".");
      System.exit(1);
    }
    
    try {
      //Read the reply from the remoteHost into b1 while there is still something to read
      byte[] b1 = new byte[100000];
      
      //The number of bytes read from this read
      int bytesRead;
      
      while ((bytesRead = this.incomingRemote.read(b1)) != -1) {    
        //DEBUGGING
        System.out.println(this.getId() + ": " + "Samples from b1: " + b1[0] + ", " + b1[1] + ", " + b1[12] + ", " + b1[100]);
        //END DEBUGGING
        
        //Send out b1 to the browser
        this.browserOutStream.write(b1, 0, bytesRead);
        this.browserOutStream.flush();
        
        //Refresh b1
        b1 = new byte[100000];
      }
      
    }
    
    //Catch any IOExceptions & print a detailed status message with an exit
    catch (IOException x) {
      System.out.println(this.getId() + ": " + "Fourth IOException encountered; Remotehost: " + this.remoteHost + "; TextHeader sent to host: " + this.textHeaderEdit + ".");
      System.exit(1);
    }
    
    //Print finish message
    System.out.println(this.getId() + ": " + "Proxy connection finished.");
  }
}