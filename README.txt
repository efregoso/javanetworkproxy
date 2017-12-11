README.txt

This is a simple web browser proxy for basic HTTP:// requests.

For this proxy, I am using port 5008.

To operate the proxy:
	a. Run the proxy from within the IDE or the command line.
	b. On receiving "Ready to receive browser request" message, type in URL to browser
		& click enter.
	
I used Firefox & Microsoft Edge to test this proxy.

I tested case.edu with this proxy.

Bugs: 	
	- Though the proxy displays the entire case.edu homepage in Firefox, it displays only text in Edge. 
	
	- The program never completes without either a Third or Fourth IOException being thrown, though it
	loads the majority of case.edu. It looks like it stems from an instance where a request is not sent
	or read into a buffer, but somehow still starts a thread:
	Ex.)	Thread 49: Connection accepted.
			Thread 49: The original textheader of the browser request: 
			Thread 49: Sample from ret: 0, 0, 0
	I have not figured out why this happens.
