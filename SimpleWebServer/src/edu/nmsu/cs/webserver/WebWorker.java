package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

 //IMPORTANT NOTE: All added code to SimpleWebServer was written by me, however I was aided by Dr. Bill Hamilton
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileReader;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class WebWorker implements Runnable
{
	private String path; 
	private Socket socket;
	private String line;
	private String resp = " ";

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		System.out.println(path); 

		try
		{
			InputStream is = socket.getInputStream(); //coming to me from client
			OutputStream os = socket.getOutputStream(); //coming from me 
			boolean fileFound = true; 
			int code;
			String message;
			readHTTPRequest(is); //browser makes request
			// ./rec/acc/test.html 
			path = "SimpleWebServer" + path; 
			System.out.println(path); // test path
			File myObj = new File(path);
			BufferedReader bufferedReader = null;
			try
			{
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(myObj)));
			}
			catch (FileNotFoundException x)
			{
				fileFound = false; 
			}
			
			if (fileFound)
			{
				code = 200;
				message = "O.K.";
				writeHTTPHeader(os, "text/html", code, message); //sends the response back to client
				writeContent(os, bufferedReader); 
				
			}
			else
			{
				code = 404; 
				message = "File Not Found";
				writeHTTPHeader(os, "text/html", code, message);
			}
			// if (path != False)
			//i read in the file, if found give it to writeHTTP and write content, 
			//if not provide a error html to say its not available and send writeHTTP that it can't be found to send a 404 error
			
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	
	
	/**
	 * Read the HTTP request header.
	 **/
	private void readHTTPRequest(InputStream is) //reads in input stream, try catch format, wait until (using r.ready) to read in input up to one carriage return.
												// print to console for debugging purposes. Here is where we serve the file back to the browswer. The browser asks for the 
												// file here.

												// return string with file name, remember to change return types and handle errors
												//main purpose of read is to get the file path out to me before the file can be given to the browser
	{
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		boolean firstLine = true;
		while (true) 
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				if (firstLine == true )
					{
						 path = line.split(" ")[1]; 
						 firstLine = false;

					}
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, int code, String message) throws Exception
	{   //these lines are what are changed when the file is requested and found successfully or not found. 
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		os.write(("HTTP/1.1 " + code + " " + message).getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes()); //influences content type that will be served
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, BufferedReader bufferedReader) throws Exception 
	{
		if (bufferedReader == null)
			{
				os.write("<html><head></head><body>\n".getBytes());
				os.write("<h3> ERROR 404: File Not Found</h3>\n".getBytes());
				os.write("</body></html>\n".getBytes()); 
			} 
		else
		{
			while ((line = bufferedReader.readLine()) != null)
			{
				System.out.println(line);
				resp += line;
			}
			bufferedReader.close();
			os.write(resp.getBytes());
		}
		//buffered reader to pull in java file and shunt contents out to the server
		//send file or text saying it cant be found 
	}

} // end class
