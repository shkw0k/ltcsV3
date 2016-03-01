package models;

import java.util.LinkedList;

public class ChangedEvents {

	private LinkedList<ChangedEvent> queue = new LinkedList<ChangedEvent>();

	synchronized public void add(ChangedEvent ev) {
		queue.clear();
		queue.add(ev);
		notifyAll();
	}

	/**
	 * Waits for an event.
	 * 
	 * Hardwired 10s waiting time.
	 * 
	 * @return null if no event
	 */
	synchronized public ChangedEvent pop() {
		if (queue.isEmpty()) {
			try {
				wait(10000);
			} catch (InterruptedException e) {
			}
		}
		if (queue.isEmpty())
			return null;
		return queue.pop();
	}
}
