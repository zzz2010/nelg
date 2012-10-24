import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;

import org.jppf.server.protocol.JPPFTask;


public class TestJFFP extends JPPFTask {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		   // write your task code here.
		   System.out.println("*** zzz ***");
		 
		   // eventually set the execution results
		   setResult("the execution was performed successfully");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		   // create a JPPF job

		   JPPFJob job = new JPPFJob();
		   job.setBlocking(true);
		   // give this job a readable unique id that we can use to monitor and manage it.
		   job.setName("Template Job Id");
		 
		   // add 10 tasks to the job.
		   for (int i=0; i<10; i++)
			try {
				job.addTask(new TestJFFP());
			} catch (JPPFException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   
		   JPPFClient jppfCLient = new JPPFClient();
		   
		   try {
			List<JPPFTask> results = jppfCLient.submit(job);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
		   
	}

}
