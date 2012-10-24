import org.jppf.client.event.TaskResultEvent;
import org.jppf.client.event.TaskResultListener;
import org.jppf.server.protocol.JPPFTask;


public class ClassificationResultListener implements TaskResultListener {

	@Override
	public void resultsReceived(TaskResultEvent event) {
		// TODO Auto-generated method stub
		for (JPPFTask task: event.getTaskList())
		{
			ClassificationResult reslut=(ClassificationResult) task.getResult();
    	 	reslut.toFile();
		}
	}

}
