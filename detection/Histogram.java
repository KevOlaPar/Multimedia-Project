package detection;

public class Histogram {

    public static int[] calculateHist(double[] values, int[] range, int bins) {
        int[] hist = new int[bins];
        int start = range[0];
        int end = range[1];
        int totalLength = end - start + 1;
        int len = totalLength/bins;

        for (double value: values) {
            int val = (int)Math.round(value);
            if(val >= start && val <= end) {
                hist[(val-start)/len]++;
            }
        }
        return hist;
    }
}
