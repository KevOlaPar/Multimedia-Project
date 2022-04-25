package media_player;

interface TrackerDelegate {
    void track();
}

public class Tracker extends Thread {

    TrackerDelegate delegate;
    boolean close = false;
    Tracker(TrackerDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        while(!close) {
            delegate.track();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        close = true;
    }

}
