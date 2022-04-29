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
        }
    }

    public void close() {
        close = true;
    }

}
